/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.playback
import com.xyonmusic.music.models.toMediaMetadata
import timber.log.Timber
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest

import android.content.Context
import android.net.ConnectivityManager
import android.os.SystemClock
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import com.xyonmusic.music.utils.PlaybackLogLevel
import com.xyonmusic.music.utils.PlaybackLogManager
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.DefaultDownloaderFactory
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.Downloader
import androidx.media3.exoplayer.offline.DownloaderFactory
import androidx.media3.exoplayer.offline.DownloadService
import com.xyonmusic.music.constants.AudioQuality
import com.xyonmusic.music.constants.AudioQualityKey
import com.xyonmusic.music.db.MusicDatabase
import com.xyonmusic.music.db.entities.FormatEntity
import com.xyonmusic.music.db.entities.SongEntity
import com.xyonmusic.music.di.DownloadCache
import com.xyonmusic.music.di.PlayerCache
import com.xyonmusic.music.ui.utils.resize
import com.xyonmusic.music.constants.AutoDownloadOnLikeKey
import com.xyonmusic.music.constants.SaveDownloadsToMusicFolderKey
import com.xyonmusic.music.utils.YTPlayerUtils
import com.xyonmusic.music.utils.enumPreference
import com.xyonmusic.music.utils.get
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import com.xyonmusic.music.applecanvas.AppleMusicCanvasProvider
import com.xyonmusic.music.utils.SpotifyCanvasLookup
import com.xyonmusic.music.canvas.AppleMusicArtistBackgroundProvider
import com.xyonmusic.music.constants.CanvasSource
import com.xyonmusic.music.constants.CanvasSourceKey
import com.xyonmusic.music.ui.player.normalizeCanvasArtistName
import com.xyonmusic.music.ui.player.normalizeCanvasSongTitle
import com.xyonmusic.music.utils.dataStore
import com.xyonmusic.music.canvas.TidalCanvasProvider
import java.util.Locale
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
    private val lyricsHelper: com.xyonmusic.music.lyrics.LyricsHelper,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val autoDownloadRequests = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val forbiddenRequests = java.util.concurrent.ConcurrentHashMap<String, DownloadRequest>()
    private var rateLimited = false
    private val appContext: Context = context
    private val pendingUpgrades = java.util.concurrent.ConcurrentHashMap<String, FormatEntity>()

    /**
     * Songs that entered the Songs list because their download was queued (they were not in the
     * library before). A cancelled download takes them back out; a finished one keeps them.
     */
    private val joinedByDownload = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads: kotlinx.coroutines.flow.StateFlow<Map<String, Download>> = _downloads
    private val downloadStateLock = Any()
    // Merge callbacks that arrive while the initial index is loading.
    private var initialIndexChanges: MutableMap<String, Download?>? = mutableMapOf()

    init {
        scope.launch {
            appContext.dataStore.data
                .map { it[AutoDownloadOnLikeKey] ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    if (!enabled) return@collectLatest

                    var known: Set<String>? = null
                    database.likedSongIds().collect { ids ->
                        val current = ids.toSet()
                        val previous = known
                        known = current
                        if (previous == null) return@collect

                        for (songId in current - previous) {
                            queueAutoDownload(songId)
                        }
                    }
                }
        }
    }

    fun onSongLiked(song: SongEntity) {
        if (song.liked) queueAutoDownload(song.id)
    }

    private fun queueAutoDownload(songId: String) {
        scope.launch {
            if (!appContext.dataStore.get(AutoDownloadOnLikeKey, false)) return@launch
            if (!autoDownloadRequests.add(songId)) return@launch

            val existing = runCatching { downloadManager.downloadIndex.getDownload(songId) }.getOrNull()
            when (existing?.state) {
                Download.STATE_QUEUED,
                Download.STATE_DOWNLOADING,
                Download.STATE_COMPLETED,
                Download.STATE_RESTARTING -> {
                    autoDownloadRequests.remove(songId)
                    return@launch
                }
            }

            runCatching {
                val title = database.songTitle(songId).orEmpty()
                DownloadService.sendAddDownload(
                    appContext,
                    ExoDownloadService::class.java,
                    DownloadRequest.Builder(songId, songId.toUri())
                        .setCustomCacheKey(songId)
                        .setData(title.toByteArray())
                        .build(),
                    false,
                )
            }.onFailure {
                autoDownloadRequests.remove(songId)
                Timber.e(it, "Auto-download on like failed for $songId")
            }
        }
    }

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    DataSource.Factory {
                        LocalOrRemoteDataSource(DefaultDataSource.Factory(appContext).createDataSource()) { ExpiredUrlGuard(
                            HlsJoinDataSource(
                                YTPlayerUtils.httpClient,
                                RangedUpstreamDataSource(OkHttpDataSource.Factory(YTPlayerUtils.httpClient)),
                            ),
                        ) { key ->
                            songUrlCache.remove(key.substringBefore('#')); songUrlCache.remove(key)
                            // The retry must also move off the client whose URL was rejected.
                            YTPlayerUtils.forceRefreshForVideo(key)
                        } }
                    },
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            // A quality upgrade is stored under "<id>#hq" and resolves the same video at the best quality.
            val videoId = mediaId.removeSuffix(UPGRADE_SUFFIX)
            val upgrade = videoId != mediaId

            // Open-ended downloads must resolve beyond a cached header.
            if (dataSpec.length >= 0 && playerCache.isCached(mediaId, dataSpec.position, dataSpec.length)) {
                return@Factory dataSpec
            }

            // Already exported (this install or an earlier one): copy that file instead of
            // fetching the song again, so it costs no data and leaves no "(1)" copy behind.
            if (!upgrade) exportedFileFor(videoId)?.let { return@Factory dataSpec.withUri(it) }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withStream(it.first)
            }

            val preferAac = appContext.dataStore.get(SaveDownloadsToMusicFolderKey, true)
            val playbackData = resolvePlaybackData(videoId, preferAac, if (upgrade) AudioQuality.HIGH else audioQuality)
            // The stored format and song row keep describing the current copy until the upgrade completes.
            if (upgrade) pendingUpgrades[videoId] = formatEntity(videoId, playbackData)
            else saveResolvedMetadata(mediaId, playbackData)
            val streamUrl = downloadStreamUrl(playbackData)

            // Drop the URL a while before Google does: a long download started near expiry
            // would otherwise get a 403 halfway through.
            val lifetimeMs = (playbackData.streamExpiresInSeconds * 1000L - URL_EXPIRY_MARGIN_MS).coerceAtLeast(0L)
            songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + lifetimeMs
            dataSpec.withStream(streamUrl)
        }

    private fun resolvePlaybackData(
        mediaId: String,
        preferAac: Boolean,
        quality: AudioQuality = audioQuality,
    ): YTPlayerUtils.PlaybackData =
        runBlocking(Dispatchers.IO) {
            YTPlayerUtils.playerResponseForPlayback(
                mediaId,
                audioQuality = quality,
                connectivityManager = connectivityManager,
                context = appContext,
                allowLossless = true,
                preferAac = preferAac,
            )
        }.getOrThrow()

    private fun formatEntity(mediaId: String, playbackData: YTPlayerUtils.PlaybackData): FormatEntity {
        val format = playbackData.format
        return FormatEntity(
            id = mediaId,
            itag = format.itag,
            mimeType = format.mimeType.substringBefore(';'),
            codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"") ?: "mp4a.40.2",
            bitrate = format.bitrate,
            sampleRate = format.audioSampleRate,
            contentLength = format.contentLength ?: 0L,
            loudnessDb = playbackData.audioConfig?.loudnessDb,
            perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
            playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl,
        )
    }

    private fun saveResolvedMetadata(mediaId: String, playbackData: YTPlayerUtils.PlaybackData) {
        database.query {
            upsert(formatEntity(mediaId, playbackData))

            val existing = getSongByIdBlocking(mediaId)?.song
            val thumbnailUrl = existing?.thumbnailUrl ?: playbackData.videoDetails?.thumbnail?.thumbnails
                ?.lastOrNull()?.url?.resize(1200, 1200)
            val updatedSong = if (existing != null) {
                existing.copy(thumbnailUrl = thumbnailUrl)
            } else {
                SongEntity(
                    id = mediaId,
                    title = playbackData.videoDetails?.title ?: "Unknown",
                    duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                    thumbnailUrl = thumbnailUrl,
                    isDownloaded = false,
                )
            }

            upsert(updatedSong)
            prefetchThumbnail(updatedSong.thumbnailUrl)
            cacheCanvas(mediaId, playbackData)
        }
    }

    private fun prefetchThumbnail(url: String?) {
        if (url == null) return

        // Cache the original bytes on disk without retaining a full-size decoded bitmap.
        val request = ImageRequest.Builder(appContext)
            .data(url)
            .diskCacheKey(url)
            .size(256)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        SingletonImageLoader.get(appContext).enqueue(request)
    }

    private fun cacheCanvas(mediaId: String, playbackData: YTPlayerUtils.PlaybackData) {
        scope.launch {
            val sourceName = appContext.dataStore.data
                .map { it[CanvasSourceKey] ?: CanvasSource.AUTO.name }
                .first()
            val canvasSource = CanvasSource.entries.firstOrNull { it.name == sourceName } ?: CanvasSource.AUTO

            val storefront = Locale.getDefault().country.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: "us"
            val requestedTitle = playbackData.videoDetails?.title.orEmpty()
            val requestedArtist = playbackData.videoDetails?.author.orEmpty()
            val title = normalizeCanvasSongTitle(requestedTitle)
            val artist = normalizeCanvasArtistName(requestedArtist)

            val canvasUrl = when (canvasSource) {
                CanvasSource.AUTO ->
                    SpotifyCanvasLookup.getBySongArtist(appContext, requestedTitle, requestedArtist)?.preferredAnimationUrl
                        ?: AppleMusicCanvasProvider.getBySongArtist(title, artist, "", storefront)?.preferredAnimationUrl
                        ?: TidalCanvasProvider.getBySongArtist(title, artist, "")?.preferredAnimationUrl
                CanvasSource.APPLE_MUSIC ->
                    AppleMusicCanvasProvider.getBySongArtist(title, artist, "", storefront)?.preferredAnimationUrl
                CanvasSource.TIDAL -> TidalCanvasProvider.getBySongArtist(title, artist, "")?.preferredAnimationUrl
                CanvasSource.SPOTIFY ->
                    SpotifyCanvasLookup.getBySongArtist(appContext, requestedTitle, requestedArtist)?.preferredAnimationUrl
            } ?: return@launch

            val dataSpec = DataSpec.Builder()
                .setUri(canvasUrl.toUri())
                .setKey("$mediaId#canvas")
                .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
                .build()
            val dataSource = CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(DefaultDataSource.Factory(appContext))
                .createDataSource()

            currentCoroutineContext().ensureActive()
            runCatching { CacheWriter(dataSource, dataSpec, null, null).cache() }
                .onSuccess { Timber.tag("CanvasDownload").d("Successfully cached canvas for $mediaId") }
                .onFailure { Timber.tag("CanvasDownload").e(it, "Failed to cache canvas for $mediaId") }
        }
    }

    private fun downloadStreamUrl(playbackData: YTPlayerUtils.PlaybackData): String = when {
        playbackData.isHlsStream || playbackData.isDashStream -> HlsJoinDataSource.uriFor(playbackData.streamUrl).toString()
        playbackData.isTidalStream -> playbackData.streamUrl
        else -> {
            // The byte range must end inside the file: googlevideo answers 416 to a range past its
            // end. Fallback clients (VISIONOS after a bot check on ANDROID_VR) often leave
            // contentLength out, and the old 10 MB guess failed every song shorter than that, so
            // read the size from the URL's own clen= and, without one, ask for no range at all:
            // RangedUpstreamDataSource still fetches it in chunks and stops at the real end.
            val length = playbackData.format.contentLength?.takeIf { it > 0 }
                ?: playbackData.streamUrl.toUri().getQueryParameter("clen")?.toLongOrNull()?.takeIf { it > 0 }
            if (length != null) "${playbackData.streamUrl}&range=0-${length - 1}" else playbackData.streamUrl
        }
    }

    // Separate joined HLS bytes from any cached YouTube rendition.
    private fun DataSpec.withStream(url: String): DataSpec =
        if (url.startsWith(HlsJoinDataSource.SCHEME + ":")) {
            buildUpon().setUri(url.toUri()).setKey("$key#hlsjoin")
                .setFlags(flags or DataSpec.FLAG_DONT_CACHE_IF_LENGTH_UNKNOWN).build()
        } else withUri(url.toUri())

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    private val downloadIndex = DefaultDownloadIndex(databaseProvider)

    @OptIn(DelicateCoroutinesApi::class)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            downloadIndex,
            SharedKeyDownloaderFactory(
                ThrottledDownloaderFactory(
                    DefaultDownloaderFactory(
                        CacheDataSource.Factory().setCache(downloadCache).setUpstreamDataSourceFactory(dataSourceFactory),
                        Executor(Runnable::run),
                    ),
                    minIntervalMs = MIN_DOWNLOAD_INTERVAL_MS,
                ),
                downloadIndex,
            ),
        ).apply {
            maxParallelDownloads = 1
            // Each retry re-resolves the stream once ExpiredUrlGuard drops a rejected URL, and
            // resumes from the bytes already cached.
            minRetryCount = 6
            addListener(
                ExoDownloadService.TerminalStateNotificationHelper(
                    context,
                    downloadNotificationHelper,
                    ExoDownloadService.NOTIFICATION_ID + 1,
                )
            )
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        autoDownloadRequests.remove(download.request.id)
                        publishDownload(download.request.id, download)

                        if (download.state == Download.STATE_FAILED) {
                            // A retry must resolve a fresh URL after a CDN error or expired signature.
                            songUrlCache.remove(download.request.id)
                            Timber.e(
                                finalException,
                                "Download failed: id=%s title=%s reason=%d",
                                download.request.id,
                                Util.fromUtf8Bytes(download.request.data),
                                download.failureReason,
                            )
                            if (isForbidden(finalException)) handleForbiddenDownload(download)
                            // Say why, so a failure is never silent (and so it can be reported from the phone).
                            if (!download.request.id.endsWith(UPGRADE_SUFFIX)) {
                                reportDownloadFailure(Util.fromUtf8Bytes(download.request.data), finalException)
                            }
                        }

                        // An upgrade copy never touches the song's own downloaded flag.
                        if (download.request.id.endsWith(UPGRADE_SUFFIX)) {
                            val base = download.request.id.removeSuffix(UPGRADE_SUFFIX)
                            when (download.state) {
                                Download.STATE_COMPLETED -> scope.launch { finishUpgrade(base) }
                                Download.STATE_FAILED -> pendingUpgrades.remove(base)
                            }
                            return
                        }

                        val shared = download.request.storageKey() != download.request.id
                        when (download.state) {
                            // Tapping Download puts the song in Songs right away (with its progress
                            // ring), instead of only once the whole queue reaches it.
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> scope.launch {
                                joinLibraryOnQueue(download)
                            }
                            Download.STATE_COMPLETED -> scope.launch {
                                joinedByDownload.remove(download.request.id)
                                ensureSongRow(download)
                                database.updateDownloadedInfo(download.request.id, true, LocalDateTime.now())
                                // A downloaded song also joins the Songs list (newest first).
                                database.addToLibraryIfMissing(download.request.id, System.currentTimeMillis())
                                if (shared) {
                                    shareFormat(download.request.id, download.request.storageKey())
                                    return@launch
                                }
                                registerDuplicate(download.request.id)
                                if (appContext.dataStore.get(SaveDownloadsToMusicFolderKey, true)) {
                                    exportToMusicFolder(download.request.id)
                                }
                            }
                            Download.STATE_FAILED,
                            Download.STATE_STOPPED,
                            Download.STATE_REMOVING -> scope.launch {
                                database.updateDownloadedInfo(download.request.id, false, null)
                            }
                        }
                    }

                    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                        // Cancelled before it finished: a song that only joined Songs for this
                        // download leaves again; one that was already in the library stays.
                        if (joinedByDownload.remove(download.request.id)) {
                            scope.launch { database.removeFromLibrary(download.request.id) }
                        }
                        songUrlCache.remove(download.request.id)
                        DownloadDuplicates.unregister(download.request.id)
                        autoDownloadRequests.remove(download.request.id)
                        publishDownload(download.request.id, null)
                    }
                }
            )
        }

    /**
     * A 403 means YouTube is rate-limiting this IP. Stop the whole queue, wait, then requeue
     * the failed songs with fresh URLs. Hammering the CDN with retries only extends the block.
     */
    private fun handleForbiddenDownload(download: Download) {
        forbiddenRequests[download.request.id] = download.request
        synchronized(this) {
            if (rateLimited) return
            rateLimited = true
        }
        downloadManager.pauseDownloads()
        scope.launch {
            delay(RATE_LIMIT_COOLDOWN_MS)
            val requests = forbiddenRequests.values.toList()
            forbiddenRequests.clear()
            requests.forEach { request ->
                runCatching { downloadManager.removeDownload(request.id) }
            }
            requests.forEach { request ->
                DownloadService.sendAddDownload(appContext, ExoDownloadService::class.java, request, false)
            }
            downloadManager.resumeDownloads()
            synchronized(this@DownloadUtil) { rateLimited = false }
        }
    }

    private var lastFailureToastAt = 0L

    /** A short, human reason for a failed download. */
    private fun describeFailure(error: Throwable?): String {
        var cause = error
        var depth = 0
        while (cause != null && depth < 8) {
            val message = cause.message.orEmpty()
            when {
                message.contains("bot", ignoreCase = true) || message.contains("Inicia sesión", ignoreCase = true) ||
                    message.contains("Sign in", ignoreCase = true) -> return "YouTube pide verificar que no eres un bot"
                cause is HttpDataSource.InvalidResponseCodeException ->
                    return "YouTube rechazó la descarga (HTTP ${cause.responseCode})"
                cause is java.net.UnknownHostException || cause is java.net.SocketTimeoutException ||
                    cause is java.net.ConnectException -> return "sin conexión con YouTube"
            }
            cause = cause.cause
            depth++
        }
        return (error?.message ?: error?.javaClass?.simpleName ?: "error desconocido").take(90)
    }

    private fun reportDownloadFailure(title: String, error: Throwable?) {
        val reason = describeFailure(error)
        PlaybackLogManager.log(PlaybackLogLevel.ERROR, "Descarga fallida: $title", reason)
        val now = System.currentTimeMillis()
        if (now - lastFailureToastAt < 20_000L) return
        lastFailureToastAt = now
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(appContext, "No se pudo descargar «$title»: $reason", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun isForbidden(error: Throwable?): Boolean {
        var cause = error
        var depth = 0
        while (cause != null && depth < 8) {
            if (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403) return true
            cause = cause.cause
            depth++
        }
        return false
    }

    init {
        scope.launch(Dispatchers.IO) {
            val result = mutableMapOf<String, Download>()
            val cursor = downloadManager.downloadIndex.getDownloads()
            cursor.use { c ->
                while (c.moveToNext()) {
                    result[c.download.request.id] = c.download
                }
            }
            synchronized(downloadStateLock) {
                initialIndexChanges?.forEach { (id, download) ->
                    if (download == null) result.remove(id) else result[id] = download
                }
                initialIndexChanges = null
                _downloads.value = result
            }
            result.values
                .filter { it.state == Download.STATE_COMPLETED && it.request.storageKey() == it.request.id }
                .forEach { registerDuplicate(it.request.id) }
            runCatching { adoptExportedFiles() }.onFailure { Timber.e(it, "Adopting exported files failed") }
        }
    }

    /** The storage key a song plays from: its own id unless its download shares another song's file. */
    fun cacheKeyFor(songId: String): String = _downloads.value[songId]?.request?.storageKey() ?: songId

    /**
     * A song served from cache never runs the resolver that writes its row, so without this the
     * download would finish with nothing to show in the Songs list.
     */
    private fun ensureSongRow(download: Download) {
        val id = download.request.id
        if (database.getSongByIdBlocking(id) != null) return
        database.insert(SongEntity(id = id, title = Util.fromUtf8Bytes(download.request.data).ifBlank { "Unknown" }))
    }

    private fun joinLibraryOnQueue(download: Download) {
        val id = download.request.id
        if (id.endsWith(UPGRADE_SUFFIX) || id in joinedByDownload) return
        ensureSongRow(download)
        val inLibrary = database.getSongByIdBlocking(id)?.song?.inLibrary != null
        if (inLibrary) return
        joinedByDownload += id
        database.addToLibraryIfMissing(id, System.currentTimeMillis())
    }

    /** The audio file already exported for this song, if the song is known and the file exists. */
    private fun exportedFileFor(songId: String): android.net.Uri? {
        val song = database.getSongByIdBlocking(songId) ?: return null
        if (song.song.isLocal) return null
        return DownloadExporter.findExported(appContext, song.artists.joinToString { it.name }, song.song.title)
    }

    /**
     * Songs in the library whose file is already in Download/XyonMusic (from before a reinstall,
     * for instance) but which this install does not know as downloaded: queue them, and the
     * resolver above fills each from its file, so they show as downloaded without re-downloading.
     */
    private suspend fun adoptExportedFiles() {
        val names = DownloadExporter.exportedBaseNames(appContext)
        if (names.isEmpty()) return
        val known = _downloads.value
        val songs = database.songs(com.xyonmusic.music.constants.SongSortType.CREATE_DATE, false).first()
        val toAdopt = songs.filter { s ->
            !s.song.isLocal && known[s.id]?.state != Download.STATE_COMPLETED &&
                exportBaseName(s.artists.joinToString { it.name }, s.song.title) in names
        }
        toAdopt.forEach { s ->
            DownloadService.sendAddDownload(
                appContext,
                ExoDownloadService::class.java,
                DownloadRequest.Builder(s.id, s.id.toUri()).setCustomCacheKey(s.id).setData(s.song.title.toByteArray()).build(),
                false,
            )
        }
        if (toAdopt.isNotEmpty()) Timber.d("Adopting %d already exported songs", toAdopt.size)
    }

    private suspend fun registerDuplicate(songId: String) {
        val song = database.song(songId).first() ?: return
        DownloadDuplicates.register(songId, song.song.title, song.artists.firstOrNull()?.name, song.song.duration)
    }

    private suspend fun shareFormat(songId: String, ownerId: String) {
        database.format(ownerId).first()?.let { owner -> database.query { upsert(owner.copy(id = songId)) } }
    }

    private fun publishDownload(id: String, download: Download?) = synchronized(downloadStateLock) {
        initialIndexChanges?.set(id, download)
        _downloads.update { current ->
            if (download == null) current - id else current + (id to download)
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }.distinctUntilChanged()

    /** Queues a best-quality copy of a downloaded song; the current copy stays until it is complete. */
    fun upgradeQuality(songId: String) {
        val title = runBlocking(Dispatchers.IO) { database.songTitle(songId).orEmpty() }
        val key = songId + UPGRADE_SUFFIX
        DownloadService.sendAddDownload(
            appContext,
            ExoDownloadService::class.java,
            DownloadRequest.Builder(key, key.toUri()).setCustomCacheKey(key).setData(title.toByteArray()).build(),
            false,
        )
    }

    /** Swaps the finished upgrade in only when it is better than what is stored; otherwise drops it. */
    private suspend fun finishUpgrade(songId: String) {
        val key = songId + UPGRADE_SUFFIX
        val better = pendingUpgrades.remove(songId)
        val current = database.format(songId).first()
        if (better == null || (current != null && better.bitrate <= current.bitrate)) {
            DownloadService.sendRemoveDownload(appContext, ExoDownloadService::class.java, key, false)
            return
        }
        database.query { upsert(better) }
        downloadCache.removeResource(songId)
        if (appContext.dataStore.get(SaveDownloadsToMusicFolderKey, true)) exportToMusicFolder(songId, key)
    }

    private suspend fun exportToMusicFolder(songId: String, cacheKey: String = songId) {
        val song = database.song(songId).first() ?: return
        val format = database.format(songId).first()
        val lyrics = database.lyrics(songId).first()?.lyrics
            ?: optionalSidecar { lyricsHelper.getLyrics(song.toMediaMetadata()).lyrics }
        val video = if (song.song.isLocal) null else optionalSidecar { YTPlayerUtils.videoStream(songId) }
        currentCoroutineContext().ensureActive()
        DownloadExporter.export(
            context = appContext,
            cache = downloadCache,
            pkg = DownloadExporter.Package(
                songId = cacheKey,
                title = song.song.title,
                artist = song.artists.joinToString { it.name },
                album = song.album?.title,
                audioMimeType = format?.mimeType,
                lyrics = lyrics?.takeUnless { it == com.xyonmusic.music.db.entities.LyricsEntity.LYRICS_NOT_FOUND },
                coverUrl = song.song.thumbnailUrl,
                video = video,
            ),
        )
    }

    private suspend fun <T> optionalSidecar(fetch: suspend () -> T): T? = try {
        fetch()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        null
    }

    fun release() {
        scope.cancel()
    }
}

/** Reads content:// and file:// from [local] and everything else from the network source. */
@UnstableApi
internal class LocalOrRemoteDataSource(
    private val local: DataSource,
    private val remoteFactory: () -> DataSource,
) : DataSource {
    private var remote: DataSource? = null
    private var current: DataSource? = null
    private val listeners = mutableListOf<androidx.media3.datasource.TransferListener>()

    override fun addTransferListener(transferListener: androidx.media3.datasource.TransferListener) {
        listeners += transferListener
        local.addTransferListener(transferListener)
        remote?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val scheme = dataSpec.uri.scheme
        val source = if (scheme == "content" || scheme == "file") local
            else remote ?: remoteFactory().also { r -> listeners.forEach(r::addTransferListener); remote = r }
        current = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = requireNotNull(current).read(buffer, offset, length)

    override fun getUri(): android.net.Uri? = current?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            current?.close()
        } finally {
            current = null
        }
    }
}

/** How long before Google's stated expiry a resolved stream URL stops being reused. */
private const val URL_EXPIRY_MARGIN_MS = 10 * 60 * 1000L

/**
 * googlevideo answers 403 (or 410) when a signed URL expired, was bound to another client or
 * only covered part of the file, and 416 when its byte range runs past the end of the file. The URL is then useless: [onRejected] drops it so the
 * DownloadManager's next retry resolves a fresh one instead of failing on the same URL.
 */
@UnstableApi
internal class ExpiredUrlGuard(
    private val upstream: DataSource,
    private val onRejected: (String) -> Unit,
) : DataSource by upstream {
    private var key: String? = null

    override fun open(dataSpec: DataSpec): Long {
        key = dataSpec.key
        return guard { upstream.open(dataSpec) }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = guard { upstream.read(buffer, offset, length) }

    private inline fun <T> guard(block: () -> T): T = try {
        block()
    } catch (error: java.io.IOException) {
        val code = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()?.responseCode
        // 416: the URL's range does not fit the file it points at, so it can never succeed either.
        if (code == 403 || code == 410 || code == 416) key?.let(onRejected)
        throw error
    }
}

/** Cache key suffix for a quality-upgrade copy of a download. */
const val UPGRADE_SUFFIX = "#hq"

/** Spaces download starts out so YouTube's guest rate limit (~300 songs/hour) is respected. */
private const val MIN_DOWNLOAD_INTERVAL_MS = 12_000L
private const val RATE_LIMIT_COOLDOWN_MS = 30L * 60L * 1000L

private class ThrottledDownloaderFactory(
    private val delegate: DownloaderFactory,
    private val minIntervalMs: Long,
) : DownloaderFactory {
    private val lastStartMs = AtomicLong(0L)

    override fun createDownloader(request: DownloadRequest): Downloader {
        val downloader = delegate.createDownloader(request)
        return object : Downloader {
            override fun download(progressListener: Downloader.ProgressListener?) {
                val last = lastStartMs.get()
                if (last != 0L) {
                    val wait = minIntervalMs - (SystemClock.elapsedRealtime() - last)
                    if (wait > 0L) {
                        try {
                            Thread.sleep(wait)
                        } catch (error: InterruptedException) {
                            Thread.currentThread().interrupt()
                            throw error
                        }
                    }
                }
                lastStartMs.set(SystemClock.elapsedRealtime())
                downloader.download(progressListener)
            }

            override fun cancel() = downloader.cancel()
            override fun remove() = downloader.remove()
        }
    }
}
