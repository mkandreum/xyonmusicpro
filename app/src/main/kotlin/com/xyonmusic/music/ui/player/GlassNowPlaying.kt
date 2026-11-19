/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Full-screen player, all glass (LiquidUI):
 *  - large artwork on top, title and artist centred under it;
 *  - at the bottom, ( ▶ ): a curved glass bar for the song position on the left (total time on top, current
 *    time at the bottom), the glass play/pause in the middle and a curved glass bar for the volume on the right
 *    (0 at the bottom, 10 on top). Holding a bar vibrates; dragging it vibrates on every step.
 *  - lyrics, queue and the song menu are small glass buttons on top; swipe the artwork to change song.
 */

package com.xyonmusic.music.ui.player

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.allowHardware
import coil3.toBitmap
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.extensions.togglePlayPause
import com.xyonmusic.music.ui.component.BottomSheetState
import com.xyonmusic.music.ui.component.LocalBottomSheetPageState
import com.xyonmusic.music.ui.component.LocalMenuState
import com.xyonmusic.music.ui.component.Lyrics
import com.xyonmusic.music.ui.component.expandedAnchor
import com.xyonmusic.music.ui.component.rememberBottomSheetState
import com.xyonmusic.music.ui.component.rememberNoArtworkPainter
import com.xyonmusic.music.ui.menu.PlayerMenu
import com.xyonmusic.music.ui.utils.ShowMediaInfo
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.liquidGlass
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.ui.LiquidArcSlider
import io.github.mkandreum.liquidbar.ui.LiquidArcSliderDefaults
import io.github.mkandreum.liquidbar.ui.LiquidGlassButton
import io.github.mkandreum.liquidbar.ui.LiquidPlayButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class NowPlayingPane { Cover, Lyrics, Queue }

@Composable
fun GlassNowPlaying(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val menuState = LocalMenuState.current
    val pageState = LocalBottomSheetPageState.current
    val context = LocalContext.current
    val song = metadata

    // Position: polled only while the sheet is open, so it costs nothing in the background.
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbing by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(playerConnection, state.isExpanded, isPlaying, song?.id) {
        do {
            position = playerConnection.player.currentPosition.coerceAtLeast(0L)
            duration = playerConnection.player.duration.takeIf { it > 0 } ?: 0L
            delay(if (isPlaying) 250L else 1000L)
        } while (isActive && state.isExpanded)
    }

    // Volume: the music stream on a 0-10 scale, kept in sync with the hardware keys.
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    LaunchedEffect(state.isExpanded) {
        while (isActive && state.isExpanded) {
            val v = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (v != volume) volume = v
            delay(400L)
        }
    }

    var pane by remember { mutableStateOf(NowPlayingPane.Cover) }
    // Registered only while lyrics/queue are open, so it sits above the sheet's own back handler.
    if (pane != NowPlayingPane.Cover) BackHandler { pane = NowPlayingPane.Cover }

    val backdrop = rememberGlassBackdrop()
    val placeholder = rememberNoArtworkPainter()
    val accent by rememberArtworkAccent(song?.thumbnailUrl)
    Box(modifier.fillMaxSize()) {
        // What the glass refracts: the artwork blown up and blurred, darker towards the controls.
        Box(Modifier.fillMaxSize().glassSource(backdrop)) {
            AsyncImage(
                model = song?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.4f; scaleY = 1.4f }
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(70.dp) else Modifier),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.30f),
                        0.5f to Color.Black.copy(alpha = 0.45f),
                        1f to Color.Black.copy(alpha = 0.80f),
                    )
                )
            )
        }

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopRow(pane, backdrop, onPane = { pane = if (pane == it) NowPlayingPane.Cover else it }) {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = song,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = { song?.id?.let { id -> pageState.showFloating { ShowMediaInfo(id) } } },
                        onDismiss = menuState::dismiss,
                    )
                }
            }

            val skip: (Boolean) -> Unit = { next ->
                if (next) {
                    if (playerConnection.player.nextMediaItemIndex != -1) playerConnection.seekToNext()
                } else if (playerConnection.player.previousMediaItemIndex != -1) playerConnection.seekToPrevious()
            }
            val controls: @Composable (Modifier) -> Unit = { m ->
                Controls(
                    modifier = m,
                    position = position, duration = duration, scrubbing = scrubbing,
                    onScrub = { scrubbing = it },
                    onScrubEnd = {
                        scrubbing?.let { f -> val ms = (f * duration).toLong(); playerConnection.player.seekTo(ms); position = ms }
                        scrubbing = null
                    },
                    isPlaying = isPlaying,
                    onPlayPause = { playerConnection.player.togglePlayPause() },
                    volume = volume, maxVolume = maxVolume,
                    onVolume = { target -> if (target != volume) { volume = target; audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) } },
                    backdrop = backdrop,
                    accent = accent,
                    onPrevious = { skip(false) },
                    onNext = { skip(true) },
                )
            }

            // Cover <-> lyrics/queue: fade with a slight zoom instead of a hard swap.
            AnimatedContent(
                targetState = pane == NowPlayingPane.Cover,
                transitionSpec = {
                    (fadeIn(tween(260, delayMillis = 60)) + androidx.compose.animation.scaleIn(tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialScale = 0.96f)) togetherWith
                        fadeOut(tween(140))
                },
                label = "nowPlayingCover",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { cover ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (cover) {
                // Disc, title and controls in one column: no dead space between them.
                val openArtist: (() -> Unit)? = song?.artists?.firstOrNull { it.id != null }?.id?.let { id ->
                    {
                        state.collapseSoft()
                        navController.navigate("artist/$id") { launchSingleTop = true }
                    }
                }
                CoverBlock(song?.thumbnailUrl, song?.title.orEmpty(), song?.artists?.joinToString { it.name }.orEmpty(), isPlaying, placeholder, skip, openArtist, backdrop) {
                    pane = NowPlayingPane.Lyrics
                }
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    controls(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 260.dp).fillMaxSize())
                    // Queue: a small glass symbol under the play button.
                    GlassSymbolButton("list.bullet", "Cola", backdrop, size = 44.dp, modifier = Modifier.offset(y = (-14).dp)) { pane = NowPlayingPane.Queue }
                    Spacer(Modifier.height(4.dp))
                }
            } else {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = pane,
                        transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                        label = "nowPlayingPane",
                        modifier = Modifier.fillMaxSize(),
                    ) { p ->
                        if (p == NowPlayingPane.Lyrics) {
                            Lyrics(
                                sliderPositionProvider = { scrubbing?.let { (it * duration).toLong() } ?: position },
                                showLyrics = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            // The queue sits on a glass panel over the blurred artwork.
                            BoxWithConstraints(
                                Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .glassPanel(backdrop),
                            ) {
                                val queueState = rememberBottomSheetState(
                                    dismissedBound = 0.dp,
                                    expandedBound = maxHeight,
                                    initialAnchor = expandedAnchor,
                                    animateOnMount = false,
                                )
                                Queue(
                                    state = queueState,
                                    playerBottomSheetState = state,
                                    navController = navController,
                                    background = Color.Transparent,
                                    onBackgroundColor = Color.White,
                                    TextBackgroundColor = Color.White,
                                    textButtonColor = Color.White,
                                    iconButtonColor = Color.White,
                                    pureBlack = false,
                                    showInlineLyrics = false,
                                    showHeaderRow = false,
                                    sheetDraggable = false,
                                    handleBack = false,
                                    applyTopSystemInset = false,
                                )
                            }
                        }
                    }
                }
                controls(Modifier.fillMaxWidth().height(200.dp))
            }
            }
            }
        }
    }
}

@Composable
private fun TopRow(pane: NowPlayingPane, backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop, onPane: (NowPlayingPane) -> Unit, onMenu: () -> Unit) {
    // Grabber on its own line, buttons under it: they never touch.
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.padding(top = 6.dp, bottom = 8.dp).size(40.dp, 5.dp)
                .clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.45f))
        )
        Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            if (pane != NowPlayingPane.Cover) TopGlassButton("Cerrar", true, backdrop) { onPane(pane) }
            TopGlassButton("···", false, backdrop, onMenu)
        }
    }
}

/** The disc: a centred square cover that melts into the screen, grows while playing and shrinks on pause. */
@Composable
private fun ColumnScope.CoverBlock(art: String?, title: String, artist: String, isPlaying: Boolean, placeholder: androidx.compose.ui.graphics.painter.Painter, onSkip: (Boolean) -> Unit, onArtist: (() -> Unit)?, backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop, onLyrics: () -> Unit) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val offset = remember { Animatable(0f) }
    val artScale by animateFloatAsState(if (isPlaying) 1f else 0.84f, spring(dampingRatio = 0.72f, stiffness = 220f), label = "artScale")
    Spacer(Modifier.height(4.dp))
    AsyncImage(
        model = art,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        modifier = Modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = artScale; scaleY = artScale
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                // Melt into the screen: soft edges all round, a longer fade at the bottom.
                drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.06f to Color.Black, 0.8f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.07f to Color.Black, 0.93f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val o = offset.value
                        if (abs(o) > size.width * 0.25f) { view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK); onSkip(o < 0) }
                        scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)) }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { ch, d -> ch.consume(); scope.launch { offset.snapTo(offset.value + d) } }
            },
    )
    Spacer(Modifier.height(10.dp))
    Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, fontFamily = com.xyonmusic.music.ui.theme.DisplayFontFamily, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(4.dp))
    // Tapping the artist opens their page: top songs, albums and the rest.
    Text(
        artist, color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = if (onArtist != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onArtist).padding(horizontal = 8.dp, vertical = 2.dp) else Modifier,
    )
    // Lyrics: a small glass symbol under the artists.
    Spacer(Modifier.height(16.dp))
    // Lyrics and download side by side: bare symbols, no circles.
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
        GlassSymbolButton("quote.bubble", "Letra", backdrop, size = 40.dp, onClick = onLyrics)
        DownloadSymbolButton(backdrop)
    }
    AudioQualityLine()
}

/** A bare SF symbol (no circle) with a glassy look: translucent white, soft shadow, bounce on press. */
@Composable
private fun GlassSymbolButton(symbol: String, label: String, backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.82f else 1f, spring(dampingRatio = 0.45f, stiffness = 600f), label = "symbolPress")
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale; shadowElevation = 0f }
            .clickable(interactionSource = src, indication = null, onClickLabel = label) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        com.xyonmusic.music.ui.component.SymbolIcon(
            symbol = com.xyonmusic.music.ui.component.sf(symbol),
            contentDescription = label,
            size = size * 0.62f,
            tint = Color.White.copy(alpha = 0.82f),
        )
    }
}

/**
 * Small, subtle line with the chosen quality and the stream's codec and bitrate, under the lyrics and download
 * symbols. Tapping it opens a tiny menu to pick the quality (Auto / Alta / Baja); it applies from the next song.
 */
@Composable
private fun AudioQualityLine() {
    val playerConnection = LocalPlayerConnection.current ?: return
    val format by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)
    var quality by com.xyonmusic.music.utils.rememberEnumPreference(
        com.xyonmusic.music.constants.AudioQualityKey, com.xyonmusic.music.constants.AudioQuality.AUTO,
    )
    var open by remember { mutableStateOf(false) }
    fun label(q: com.xyonmusic.music.constants.AudioQuality) = when (q) {
        com.xyonmusic.music.constants.AudioQuality.AUTO -> "Auto"
        com.xyonmusic.music.constants.AudioQuality.HIGH -> "Alta"
        com.xyonmusic.music.constants.AudioQuality.LOW -> "Baja"
    }
    val f = format
    val parts = buildList {
        add(label(quality))
        if (f != null) {
            add((f.codecs.takeIf { it.isNotBlank() } ?: f.mimeType.substringAfter('/')).substringBefore('.').uppercase())
            f.bitrate.takeIf { it > 0 }?.let { add("${it / 1000} kbps") }
            f.sampleRate?.takeIf { it > 0 }?.let { add("%.1f kHz".format(it / 1000f)) }
        }
    }
    Box {
        Text(
            parts.joinToString(" · ") + "  ▾",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            modifier = Modifier
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { open = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            com.xyonmusic.music.constants.AudioQuality.entries.forEach { q ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text((if (q == quality) "✓  " else "     ") + label(q)) },
                    onClick = { quality = q; open = false },
                )
            }
        }
    }
}

/** Download the current song; shows a check once it is downloaded and a spinner-free arrow while it downloads. */
@Composable
private fun DownloadSymbolButton(backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val song = metadata ?: return
    if (song.isLocalFile) return
    val context = LocalContext.current
    val downloadUtil = com.xyonmusic.music.LocalDownloadUtil.current.get()
    val database = com.xyonmusic.music.LocalDatabase.current.get()
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    val state = downloads[song.id]?.state
    val done = state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
    val busy = state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING || state == androidx.media3.exoplayer.offline.Download.STATE_QUEUED
    GlassSymbolButton(
        symbol = when { done -> "checkmark.circle"; busy -> "arrow.down.circle.dotted"; else -> "arrow.down.to.line" },
        label = when { done -> "Descargada"; busy -> "Cancelar descarga"; else -> "Descargar" },
        backdrop = backdrop,
        size = 40.dp,
    ) {
        if (done) {
            // Already downloaded: tapping again removes the download and takes the song out of Songs.
            com.xyonmusic.music.playback.removeDownloads(context, listOf(song.id))
            database.query { removeFromLibrary(song.id) }
        } else if (busy) {
            // Cancels only this song; the rest of the queue keeps downloading.
            com.xyonmusic.music.playback.cancelDownload(context, song.id, downloadUtil.downloads.value)
        } else {
            com.xyonmusic.music.playback.downloadSongs(
                context,
                listOf(com.xyonmusic.music.playback.DownloadTarget(song.id, song.title, song.artists.firstOrNull()?.name)),
                downloadUtil.downloads.value,
            )
        }
    }
}

/** Glass panel (LiquidUI) for the queue: blurred, refracted edge, dark tint. */
private fun Modifier.glassPanel(backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop): Modifier =
    liquidGlass(
        backdrop,
        RoundedCornerShape(28.dp),
        LiquidGlassStyle.Dark.copy(blur = 14.dp, refractionHeight = 28.dp, refractionAmount = 28.dp, surface = Color.Black.copy(alpha = 0.32f)),
    )

/** «╮ ▶ ╭»: position bar, glass play/pause and volume bar, as large as the space allows. */
@Composable
private fun Controls(
    modifier: Modifier,
    position: Long, duration: Long, scrubbing: Float?,
    onScrub: (Float) -> Unit, onScrubEnd: () -> Unit,
    isPlaying: Boolean, onPlayPause: () -> Unit,
    volume: Int, maxVolume: Int, onVolume: (Int) -> Unit,
    backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop,
    accent: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    // The bars take their colour from the artwork.
    val arcColors = LiquidArcSliderDefaults.colors(fill = accent.copy(alpha = 0.9f), bubble = accent, onBubble = if (accent.luminance() > 0.5f) Color.Black else Color.White)
    val shown = scrubbing ?: if (duration > 0) position.toFloat() / duration else 0f
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LiquidArcSlider(
            value = shown,
            onValueChange = onScrub,
            onValueChangeFinished = onScrubEnd,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            mirrored = false,
            // A step every 10 s, between 4 and 40 steps.
            segments = (duration / 10_000L).toInt().coerceIn(4, 40),
            topLabel = formatMs(duration),
            bottomLabel = formatMs((shown * duration).toLong()),
            format = { formatMs((it * duration).toLong()) },
            colors = arcColors,
            thickness = 22.dp,
            label = "Posición",
            inner = { io.github.mkandreum.liquidbar.ui.LiquidSkipButton(next = false, onClick = onPrevious, backdrop = backdrop, size = 70.dp) },
        )
        LiquidPlayButton(isPlaying = isPlaying, onClick = onPlayPause, backdrop = backdrop, size = 96.dp)
        LiquidArcSlider(
            value = volume * 10f / maxVolume,
            onValueChange = { v -> onVolume((v / 10f * maxVolume).roundToInt().coerceIn(0, maxVolume)) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
            mirrored = true,
            valueRange = 0f..10f,
            segments = 10,
            topLabel = "10",
            bottomLabel = "0",
            format = { "${it.roundToInt()}" },
            colors = arcColors,
            thickness = 22.dp,
            label = "Volumen",
            inner = { io.github.mkandreum.liquidbar.ui.LiquidSkipButton(next = true, onClick = onNext, backdrop = backdrop, size = 70.dp) },
        )
    }
}

@Composable
private fun TopGlassButton(text: String, active: Boolean, backdrop: io.github.mkandreum.liquidbar.glass.GlassBackdrop, onClick: () -> Unit) {
    LiquidGlassButton(
        onClick = onClick,
        backdrop = backdrop,
        modifier = Modifier.height(38.dp).widthIn(min = 46.dp),
        shape = RoundedCornerShape(19.dp),
        tint = if (active) Color.White else Color.Unspecified,
        onClickLabel = text,
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 14.dp))
    }
}

/** A light, saturated colour from the artwork for the bars (vibrant swatch first, kept readable on dark). */
@Composable
private fun rememberArtworkAccent(url: String?): androidx.compose.runtime.State<Color> {
    val context = LocalContext.current
    return androidx.compose.runtime.produceState(Color.White, url) {
        if (url == null) return@produceState
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val request = coil3.request.ImageRequest.Builder(context).data(url).size(160, 160).allowHardware(false).build()
            val bitmap = runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap()
                ?: return@withContext Color.White
            val palette = androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(12).generate()
            val argb = palette.lightVibrantSwatch?.rgb ?: palette.vibrantSwatch?.rgb ?: palette.lightMutedSwatch?.rgb ?: palette.dominantSwatch?.rgb
                ?: return@withContext Color.White
            val c = Color(argb)
            // Lift dark colours so the bar stays visible over the dark player.
            if (c.luminance() < 0.35f) androidx.compose.ui.graphics.lerp(c, Color.White, 0.45f) else c
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(s / 60, s % 60)
}
