/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.player

import com.xyonmusic.music.ui.component.volumeBars
import com.xyonmusic.music.ui.component.sf
import com.xyonmusic.music.ui.component.SymbolIcon
import androidx.compose.ui.graphics.luminance
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.content.Intent
import android.view.WindowManager
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.xyonmusic.music.ui.ios.compat.AlertDialog
import com.xyonmusic.music.ui.ios.compat.CircularWavyProgressIndicator
import com.xyonmusic.music.ui.ios.compat.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import com.xyonmusic.music.ui.ios.compat.Icon
import androidx.compose.material3.IconButtonDefaults
import com.xyonmusic.music.ui.ios.compat.LocalContentColor
import com.xyonmusic.music.ui.ios.compat.AppTheme
import androidx.compose.material3.OutlinedIconButton
import com.xyonmusic.music.ui.ios.compat.ProvideTextStyle
import androidx.compose.runtime.produceState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.ui.ios.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.xyonmusic.music.ui.theme.ForcedDarkScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import coil3.size.Size as CoilSize
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.xyonmusic.music.LocalDatabase
import com.xyonmusic.music.LocalDownloadUtil
import com.xyonmusic.music.LocalListenTogetherManager
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.LocalTabView
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.AudioQuality
import com.xyonmusic.music.constants.AudioQualityKey
import com.xyonmusic.music.constants.AppTextColorKey
import com.xyonmusic.music.constants.CropAlbumArtKey
import com.xyonmusic.music.constants.DarkModeKey
import com.xyonmusic.music.constants.CompactPlayerInTabViewKey
import com.xyonmusic.music.constants.CompactPlayerMaxWidth
import com.xyonmusic.music.constants.HidePlayerThumbnailKey
import com.xyonmusic.music.constants.HideVolumeBarKey
import com.xyonmusic.music.constants.EnableGoogleCastKey
import com.xyonmusic.music.constants.EnableLyricsThumbnailPlayPauseKey
import com.xyonmusic.music.constants.KeepScreenOn
import com.xyonmusic.music.constants.PlayerBackgroundStyle
import com.xyonmusic.music.constants.PlayerBackgroundStyleKey
import com.xyonmusic.music.constants.PlayerGradientAngleKey
import com.xyonmusic.music.constants.PlayerGradientStopsKey
import com.xyonmusic.music.ui.theme.decodeGradientStops
import com.xyonmusic.music.ui.theme.tiltedGradient
import com.xyonmusic.music.constants.PlayerStaticColorKey
import com.xyonmusic.music.constants.PlayerButtonsStyle
import com.xyonmusic.music.constants.OneTapFullscreenLyricsKey
import com.xyonmusic.music.constants.FullscreenLyricsCollapseTopKey
import com.xyonmusic.music.constants.PlayerButtonsStyleKey
import com.xyonmusic.music.constants.PlayerHorizontalPadding
import com.xyonmusic.music.constants.QueuePeekHeight
import com.xyonmusic.music.constants.ShowUpNextKey
import com.xyonmusic.music.constants.SliderStyle
import com.xyonmusic.music.constants.SliderStyleKey
import com.xyonmusic.music.constants.SwipeLyricsKey
import com.xyonmusic.music.constants.ThumbnailCornerRadius
import com.xyonmusic.music.constants.ThumbnailRoundedShape
import com.xyonmusic.music.constants.UseAppleMusicPlayerKey
import com.xyonmusic.music.constants.UseNewPlayerDesignKey
import com.xyonmusic.music.constants.ShowAudioQualityBadgeKey
import com.xyonmusic.music.db.entities.LyricsEntity
import com.xyonmusic.music.extensions.SwipeGesture
import com.xyonmusic.music.extensions.togglePlayPause
import com.xyonmusic.music.extensions.toggleRepeatMode
import com.xyonmusic.music.listentogether.RoomRole
import com.xyonmusic.music.models.MediaMetadata
import com.xyonmusic.music.playback.ExoDownloadService
import com.xyonmusic.music.core.AudioDeviceMenu
import com.xyonmusic.music.core.AudioDeviceType
import com.xyonmusic.music.core.audioDeviceSymbol
import com.xyonmusic.music.ui.component.CastButton
import com.xyonmusic.music.BuildConfig
import com.xyonmusic.music.ui.component.DjReadout
import com.xyonmusic.music.ui.component.BlurredBackdropImage
import com.xyonmusic.music.ui.component.BottomSheet
import com.xyonmusic.music.ui.component.BottomSheetState
import com.xyonmusic.music.ui.component.LocalBottomSheetPageState
import com.xyonmusic.music.ui.component.LocalMenuState
import com.xyonmusic.music.ui.component.Lyrics
import com.xyonmusic.music.ui.component.PlayerSliderTrack
import com.xyonmusic.music.ui.component.ResizableIconButton
import com.xyonmusic.music.ui.utils.resize
import com.xyonmusic.music.ui.utils.Motion
import com.xyonmusic.music.ui.utils.flamingoChrome
import com.xyonmusic.music.ui.component.heroFootLiveBlur
import com.xyonmusic.music.ui.player.customize.AnimatedPlayerGlyph
import com.xyonmusic.music.ui.player.customize.PlayerGlyph
import com.xyonmusic.music.ui.player.customize.PlayerIconSlot
import com.xyonmusic.music.ui.player.customize.rememberPlayerIcon
import com.xyonmusic.music.ui.component.GlassComponent
import com.xyonmusic.music.ui.component.LocalGlassEffectConfig
import com.xyonmusic.music.ui.component.PLAYER_BLUR_MULTIPLIER
import com.xyonmusic.music.ui.component.isGlassAllowed
import com.xyonmusic.music.ui.component.liquidGlass
import com.xyonmusic.music.ui.component.ScrollingWaveformSeekBar
import com.xyonmusic.music.ui.component.rememberPlaybackFraction
import com.xyonmusic.music.ui.component.WavySlider
import com.xyonmusic.music.ui.component.GlassCircleButton
import com.xyonmusic.music.ui.component.LocalAppBackdrop
import com.xyonmusic.music.ui.component.LocalBackdropLoopBucket
import com.xyonmusic.music.ui.component.backdrop.backdrops.backdropStaticRegion
import com.xyonmusic.music.ui.component.backdrop.backdrops.layerBackdrop
import com.xyonmusic.music.ui.component.backdrop.backdrops.rememberLayerBackdrop
import com.xyonmusic.music.ui.component.rememberBottomSheetState
import com.xyonmusic.music.ui.component.rememberHeroTint
import com.xyonmusic.music.ui.menu.OldPlayerMenu
import com.xyonmusic.music.ui.menu.PlayerMenu
import com.xyonmusic.music.ui.component.VolumeSlider
import com.xyonmusic.music.ui.screens.settings.DarkMode
import com.xyonmusic.music.ui.theme.AppleTokens
import com.xyonmusic.music.ui.theme.PlayerColorExtractor
import com.xyonmusic.music.ui.utils.ShowMediaInfo
import com.xyonmusic.music.ui.utils.ShowOffsetDialog
import com.xyonmusic.music.utils.makeTimeString
import com.xyonmusic.music.utils.rememberEnumPreference
import com.xyonmusic.music.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.max
import kotlin.math.roundToInt
import com.xyonmusic.music.ui.component.Icon as MIcon
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultDataSource
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.xyonmusic.music.applecanvas.AppleMusicCanvasProvider
import com.xyonmusic.music.canvas.CanvasArtwork
import com.xyonmusic.music.canvas.TidalCanvasProvider
import com.xyonmusic.music.constants.CanvasSource
import com.xyonmusic.music.constants.CanvasSourceKey
import com.xyonmusic.music.constants.CanvasThumbnailAnimationKey
import com.xyonmusic.music.constants.DataSaverEnabledKey
import com.xyonmusic.music.extensions.metadata
import com.xyonmusic.music.ui.player.CanvasArtworkPlaybackCache
import com.xyonmusic.music.ui.player.normalizeCanvasArtistName
import com.xyonmusic.music.ui.player.normalizeCanvasSongTitle
import com.xyonmusic.music.ui.theme.PlayerSliderColors
import com.xyonmusic.music.utils.SpotifyCanvasLookup
import java.util.Locale

/** Poll interval for VideoLoopClock and the bucket width glass surfaces cache
 *  a looping canvas video's blur/lens output at (see LocalBackdropLoopBucket). */
private const val VideoLoopBucketMs = 100L

/** Upper bound on waiting for the player sheet before opening the queue inside it. */
private const val PARENT_SHEET_SETTLE_TIMEOUT_MS = 1_000L

/** Strength of white content added onto the background under `additiveChrome`. Below full so
 *  the blend doesn't clip to plain white and wash out the background's colour. */
private const val AdditiveChromeContentAlpha = 1f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    /** Editor-only: mounted there a second time alongside the real instance, so window-level
     *  effects (status bar icon color, keep-screen-on) are suppressed to keep the two copies
     *  from fighting over that global state. */
    isPreviewOnly: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current.get()
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // Both player styles use the same sheet and drag gestures.
    val (useAppleMusicPlayer) = rememberPreference(UseAppleMusicPlayerKey, defaultValue = true)
    val v2Look by rememberEnumPreference(com.xyonmusic.music.constants.PlayerLookKey, com.xyonmusic.music.constants.PlayerLook.SQUARED)

    // Shared between the background slot (BackgroundVideoView polls into it) and the
    // control pills below — see VideoLoopClock's doc for why this is a plain clock.
    val videoLoopClock = rememberVideoLoopClock()

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) = rememberPreference(
        UseNewPlayerDesignKey,
        defaultValue = false
    )
    val (showAudioQualityBadge) = rememberPreference(
        ShowAudioQualityBadgeKey,
        defaultValue = true
    )
    val (hidePlayerThumbnail, onHidePlayerThumbnailChange) = rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    val (compactPlayerInTabView) = rememberPreference(CompactPlayerInTabViewKey, false)
    // Mini player is unaffected — only the expanded content gets width-capped, and only
    // when the side rail is actually showing (tab view / wide screen).
    val playerContentMaxWidth = if (LocalTabView.current && compactPlayerInTabView) {
        CompactPlayerMaxWidth
    } else {
        Dp.Unspecified
    }
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.GRADIENT
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val preferredDark = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val useDarkTheme = com.xyonmusic.music.ui.theme.LocalForcedDark.current || preferredDark

    val dataSaverEnabled by rememberPreference(DataSaverEnabledKey, false)
    val enableCanvasPref by rememberPreference(CanvasThumbnailAnimationKey, true)
    // Canvas videos are the app's single largest data consumer; DataSaver and
    // battery saver both get them disabled.
    val powerSaverActive = remember {
        context.getSystemService(android.os.PowerManager::class.java)?.isPowerSaveMode ?: false
    }
    val enableCanvas = if (dataSaverEnabled || powerSaverActive) false else enableCanvasPref
    val (canvasSource) = rememberEnumPreference(CanvasSourceKey, defaultValue = CanvasSource.AUTO)

    val shouldUseDarkButtonColors = remember(playerBackground, useDarkTheme) {
        playerBackground != PlayerBackgroundStyle.DEFAULT || useDarkTheme
    }

    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val swipeLyrics by rememberPreference(SwipeLyricsKey, false)
    val enableLyricsThumbnailPlayPause by rememberPreference(EnableLyricsThumbnailPlayPauseKey, false)
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    PlayerWindowEffects(state, isPreviewOnly, playerBackground, useDarkTheme, keepScreenOn)
    val staticColor by rememberPreference(PlayerStaticColorKey, defaultValue = 0xFF1A1A1A.toInt())
    val gradientStopsRaw by rememberPreference(PlayerGradientStopsKey, defaultValue = "")
    val gradientStops = remember(gradientStopsRaw) { decodeGradientStops(gradientStopsRaw) }
    val gradientAngle by rememberPreference(PlayerGradientAngleKey, defaultValue = 90f)

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> AppTheme.colorScheme.secondary
        else -> AppTheme.colorScheme.onSurface
    }
    val useBlackBackground = remember(useDarkTheme, pureBlack) { useDarkTheme && pureBlack }

    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue =null)
    val automix by playerConnection.service.automixItems.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()
    val playerVolume by playerConnection.service.playerVolume.collectAsStateWithLifecycle()

    val (audioQuality) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue =null)
    
    val playerFormat by rememberPlayerAudioFormat(playerConnection)

    val isLosslessStream = currentFormat?.mimeType?.contains("flac", ignoreCase = true) == true || 
                          playerFormat?.sampleMimeType?.contains("flac", ignoreCase = true) == true
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SLIM)

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue =RoomRole.NONE)
    // "Guest" here means "may not drive playback", not merely "is not host":
    // with the room set to everyone-control a member drives playback too, so
    // keying the UI off the role alone left their controls greyed out.
    val canControlTogether = listenTogetherManager?.canControl?.collectAsStateWithLifecycle(initialValue = true)
    val isListenTogetherGuest = canControlTogether?.value == false
    
    // Cast state can be absent while the service is starting or stopping.
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsStateWithLifecycle() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsStateWithLifecycle() ?: remember { mutableFloatStateOf(1f) }
    
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }
    
    var position by positionState
    var duration by durationState
    
    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) {
                castPosition
            } else {
                position
            }
        }
    }
    
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }
    
    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    // Keep the queue topped up from automix without mutating the player mid-composition.
    // isPreviewOnly: the DIY editor mounts a second copy of this composable: without this
    // guard both instances race to append the same automix item to the real queue.
    LaunchedEffect(canSkipNext, automix, isPreviewOnly) {
        if (isPreviewOnly) return@LaunchedEffect
        if (!canSkipNext && automix.isNotEmpty()) {
            playerConnection.service.addToQueueAutomix(automix[0], 0)
        }
    }

    val audioOutput by rememberActiveAudioOutput()
    val audioDeviceIcons = com.xyonmusic.music.core.rememberAudioDeviceIcons()
    val audioDeviceNames = com.xyonmusic.music.core.rememberAudioDeviceNames()

    val systemVolumeHolder = rememberSystemVolume()
    val audioManager = systemVolumeHolder.audioManager
    val maxSystemVolume = systemVolumeHolder.max
    val systemVolume by systemVolumeHolder.volume

    val defaultGradientColors = listOf(AppTheme.colorScheme.surface, AppTheme.colorScheme.surfaceVariant)
    val fallbackColor = AppTheme.colorScheme.surface.toArgb()

    // Match backdrop timing to the audio crossfade; use a slower default otherwise.
    val crossfadeMs by playerConnection.crossfadeDurationMs.collectAsStateWithLifecycle()
    val bgFadeMs = ambientTransitionMillis(crossfadeMs)
    // Sample video colors once per video, then transition the backdrop to them.
    // Transition the backdrop after the cover and lyrics during a track change.
    val trackChoreography = rememberTrackChoreography(mediaMetadata)
    val ambientMetadata = trackChoreography.ambient.value
    val videoLook = CanvasVideoLooks.forMedia(ambientMetadata?.id)

    suspend fun extractGradientColors(source: Any, style: PlayerBackgroundStyle): List<Color>? =
        withContext(Dispatchers.IO) {
            val bitmap = when (source) {
                is android.graphics.Bitmap -> source
                else -> {
                    val request = ImageRequest.Builder(context)
                        .data(source)
                        .size(200, 200)
                        .allowHardware(false)
                        .build()
                    runCatching { context.imageLoader.execute(request) }.getOrNull()?.image?.toBitmap()
                }
            } ?: return@withContext null
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap)
                    .maximumColorCount(8)
                    .resizeBitmapArea(100 * 100)
                    .generate()
            }
            if (style == PlayerBackgroundStyle.GLOW_ANIMATED) {
                listOf(
                    Color(palette.getVibrantColor(fallbackColor)),
                    Color(palette.getLightVibrantColor(fallbackColor)),
                    Color(palette.getDarkVibrantColor(fallbackColor)),
                    Color(palette.getMutedColor(fallbackColor)),
                    Color(palette.getLightMutedColor(fallbackColor)),
                    Color(palette.getDarkMutedColor(fallbackColor)),
                ).distinct()
            } else {
                PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor)
            }
        }

    LaunchedEffect(ambientMetadata?.id, playerBackground, videoLook) {
        // Old skins only: the glass player takes its own colour from the artwork.
        if (!isPreviewOnly) return@LaunchedEffect
        if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED) {
            val currentMetadata = ambientMetadata ?: return@LaunchedEffect
            val look = videoLook
            val key = if (look != null) {
                "${currentMetadata.id}#video:$playerBackground:${look.liveFrameVersion}"
            } else {
                "${currentMetadata.id}:$playerBackground"
            }
            if (look != null) {
                val liveVideoPrefix = "${currentMetadata.id}#video:$playerBackground:"
                gradientColorsCache.keys.removeAll { it.startsWith(liveVideoPrefix) && it != key }
            }
            gradientColorsCache[key]?.let {
                gradientColors = it
                return@LaunchedEffect
            }
            val source: Any = look?.stitched ?: currentMetadata.thumbnailUrl ?: return@LaunchedEffect
            extractGradientColors(source, playerBackground)?.let {
                gradientColorsCache[key] = it
                gradientColors = it
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Precompute the next track's colors so its backdrop can transition immediately.
    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (!isPreviewOnly) return@LaunchedEffect
        val player = playerConnection.player
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == C.INDEX_UNSET || nextIndex >= player.mediaItemCount) return@LaunchedEffect
        val next = player.getMediaItemAt(nextIndex)
        val nextUrl = next.mediaMetadata.artworkUri?.toString() ?: return@LaunchedEffect
        when (playerBackground) {
            PlayerBackgroundStyle.FLUID_MESH -> prewarmMeshPalette(context, nextUrl)
            PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED -> {
                val key = "${next.mediaId}:$playerBackground"
                if (gradientColorsCache[key] == null) {
                    extractGradientColors(nextUrl, playerBackground)?.let { gradientColorsCache[key] = it }
                }
            }
            else -> Unit
        }
    }

    val (showUpNext) = rememberPreference(ShowUpNextKey, defaultValue = false)
    val (appTextColorInt) = rememberPreference(AppTextColorKey, defaultValue = 0)
    val TextBackgroundColor by animateColorAsState(
        targetValue = if (appTextColorInt != 0) Color(appTextColorInt) else when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> AppTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
            PlayerBackgroundStyle.FLUID_MESH -> Color.White
            PlayerBackgroundStyle.GLASS -> Color.White
            PlayerBackgroundStyle.STATIC, PlayerBackgroundStyle.CUSTOM_GRADIENT -> Color.White
        },
        label = "TextBackgroundColor"
    )

    val icBackgroundColor by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> AppTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            PlayerBackgroundStyle.GRADIENT -> Color.Black
            PlayerBackgroundStyle.GLOW_ANIMATED -> Color.Black
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.Black
            PlayerBackgroundStyle.LIVE_MESH -> Color.Black
            PlayerBackgroundStyle.FLUID_MESH -> Color.Black
            PlayerBackgroundStyle.GLASS -> Color.Black
            PlayerBackgroundStyle.STATIC, PlayerBackgroundStyle.CUSTOM_GRADIENT -> Color.Black
        },
        label = "icBackgroundColor"
    )

    // Keep all valid candidates so playback can try the next one after a video error.
    val classicCanvas = rememberClassicCanvasState(mediaMetadata, playerBackground, canvasSource, useAppleMusicPlayer, enableCanvas)
    var canvasCandidates by classicCanvas.candidates
    var canvasCandidateIndex by classicCanvas.index
    val canvasArtwork = canvasCandidates.getOrNull(canvasCandidateIndex)

    val (textButtonColor, iconButtonColor) = when {
        playerBackground == PlayerBackgroundStyle.BLUR ||
        playerBackground == PlayerBackgroundStyle.GRADIENT ||
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED ||
        playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
        playerBackground == PlayerBackgroundStyle.LIVE_MESH ||
        playerBackground == PlayerBackgroundStyle.FLUID_MESH ||
        playerBackground == PlayerBackgroundStyle.GLASS -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    AppTheme.colorScheme.primary,
                    AppTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    AppTheme.colorScheme.tertiary,
                    AppTheme.colorScheme.onTertiary
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT ->
                    if (useDarkTheme) Pair(Color.White, Color.Black)
                    else Pair(Color.Black, Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    AppTheme.colorScheme.primary,
                    AppTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    AppTheme.colorScheme.tertiary,
                    AppTheme.colorScheme.onTertiary
                )
            }
        }
    }

    val seekBarActiveColor = if (appTextColorInt != 0) Color(appTextColorInt) else textButtonColor
    val additiveChrome = textButtonColor.luminance() > 0.5f

    val (sideButtonContainerColor, sideButtonContentColor) = when {
        playerBackground == PlayerBackgroundStyle.BLUR ||
        playerBackground == PlayerBackgroundStyle.GRADIENT ||
        playerBackground == PlayerBackgroundStyle.GLASS -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    Color.White.copy(alpha = 0.2f),
                    Color.White
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    AppTheme.colorScheme.primaryContainer,
                    AppTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    AppTheme.colorScheme.tertiaryContainer,
                    AppTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        playerBackground == PlayerBackgroundStyle.GLOW_ANIMATED -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    Color.White.copy(alpha = 0.2f),
                    Color.White
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    AppTheme.colorScheme.primaryContainer,
                    AppTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    AppTheme.colorScheme.tertiaryContainer,
                    AppTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(
                    AppTheme.colorScheme.surfaceContainerHighest,
                    AppTheme.colorScheme.onSurface
                )
                PlayerButtonsStyle.PRIMARY -> Pair(
                    AppTheme.colorScheme.primaryContainer,
                    AppTheme.colorScheme.onPrimaryContainer
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    AppTheme.colorScheme.tertiaryContainer,
                    AppTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    val downloadActions = LocalDownloadUtil.current.get()
    val download by LocalDownloadUtil.current.get().getDownload(mediaMetadata?.id ?: "")
        .collectAsStateWithLifecycle(initialValue =null)

    val sleepTimer = rememberSleepTimerState(playerConnection)
    val sleepTimerEnabled = sleepTimer.enabled
    val sleepTimerTimeLeft by sleepTimer.timeLeft
    var showSleepTimerDialog by sleepTimer.showDialog
    SleepTimerDialogHost(playerConnection, sleepTimer)

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val classicPages = remember { PlayerAuxiliaryPageState() }
    val classicVideo = rememberClassicVideo(
        connection = playerConnection,
        metadata = mediaMetadata,
        isPlaying = isPlaying && !classicPages.showing,
        sheetCollapsed = state.isCollapsed,
        isPreviewOnly = isPreviewOnly,
    )
    val classicVideoStream = classicVideo.stream.takeIf { classicVideo.watch }
    val videoSwitchEnabled by rememberPreference(com.xyonmusic.music.constants.ShowVideoSwitchKey, true)
    var showRoutePopover by remember { mutableStateOf(false) }
    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }
    val enableGoogleCast by rememberPreference(EnableGoogleCastKey, defaultValue = true)
    var showInlineLyrics by remember {
        mutableStateOf(false)
    }

    var isFullScreen by remember {
        mutableStateOf(false)
    }
    val (oneTapFullscreenLyrics) = rememberPreference(OneTapFullscreenLyricsKey, defaultValue = false)
    val (fullscreenLyricsCollapseTop) = rememberPreference(FullscreenLyricsCollapseTopKey, defaultValue = true)
    val (hideVolumeBar) = rememberPreference(HideVolumeBarKey, defaultValue = false)
    // Poll local playback while playing, less often when the player is collapsed.
    LaunchedEffect(isPlaying, isCasting, state.isCollapsed) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(if (state.isCollapsed) 250L else 100L)
                if (sliderPosition == null) { // Only update if user isn't dragging
                    position = playerConnection.player.currentPosition
                    duration = playerConnection.player.duration
                }
            }
        }
    }
    
    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }
    
    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    // Mini-player actions arrive through PlayerConnection; previews have their own player instance.
    LaunchedEffect(useAppleMusicPlayer, isPreviewOnly) {
        if (useAppleMusicPlayer || isPreviewOnly) return@LaunchedEffect
        playerConnection.requestShowLyrics.collect { requested ->
            if (requested) {
                if (!state.isExpanded) {
                    withTimeoutOrNull(PARENT_SHEET_SETTLE_TIMEOUT_MS) {
                        snapshotFlow { state.isExpanded }.first { it }
                    }
                }
                classicPages.value = PlayerInternalState.LYRICS
                playerConnection.requestShowLyrics.value = false
            }
        }
    }
    LaunchedEffect(useAppleMusicPlayer, isPreviewOnly) {
        if (useAppleMusicPlayer || isPreviewOnly) return@LaunchedEffect
        playerConnection.requestShowQueue.collect { requested ->
            if (requested) {
                if (!state.isExpanded) {
                    withTimeoutOrNull(PARENT_SHEET_SETTLE_TIMEOUT_MS) {
                        snapshotFlow { state.isExpanded }.first { it }
                    }
                }
                classicPages.value = PlayerInternalState.QUEUE
                playerConnection.requestShowQueue.value = false
            }
        }
    }

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.GLOW_ANIMATED, PlayerBackgroundStyle.APPLE_MUSIC, PlayerBackgroundStyle.GLASS ->
            AppTheme.colorScheme.surfaceContainer
        PlayerBackgroundStyle.LIVE_MESH, PlayerBackgroundStyle.FLUID_MESH ->
            Color.Black
        else ->
            if (useBlackBackground) Color.Black
            else AppTheme.colorScheme.surfaceContainer
    }

    // Read animated progress in graphicsLayer lambdas to avoid recomposing the whole player.
    // Keep this layer opaque during the shared morph; the overlay animates its fade.
    val backgroundAlpha = { if (PlayerMorph.active) 1f else state.progress.coerceIn(0f, 1f) }
    // Use a derived flag for composition so only threshold changes trigger recomposition.
    val backgroundVisible by remember(state) { derivedStateOf { state.progress > 0.01f } }

    // Capture this player's background so foreground glass buttons do not sample the screen behind it.
    val playerBackdrop = rememberLayerBackdrop()
    // Menus opened from the expanded player sample this layer, not the whole app.
    val backdropHolder = LocalPlayerBackdropHolder.current
    androidx.compose.runtime.SideEffect { backdropHolder?.backdrop = playerBackdrop }

    val isLandscapeLayout =
        LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // PlayerV2 reports its page so the sticker overlay can hide on Lyrics and Queue.
    var v17ShowingCover by remember { mutableStateOf(true) }

    PlayerMorphHostEffects(state, isPreviewOnly)

    BottomSheet(
        state = state,
        modifier = modifier,
        // iOS 27 container morph: the player grows out of the mini player as one card.
        morphEnabled = PlayerMorph.enabled,
        // Keep content composed while collapsed so it is ready when the opening morph starts.
        keepContentComposed = true,
        // The editor preview controls its own anchor; prevent its gestures from collapsing
        // or dismissing this sheet and stopping playback.
        isExpandable = !isPreviewOnly,
        contentMaxWidth = playerContentMaxWidth,
        // The sheet rounds to the device's real screen radius as it reaches full size, so
        // its corners sit on the glass rather than near it, then opens out to square once
        // settled. 0.dp on a device that reports no rounded corners, or in split screen.
        expandedCornerRadius = rememberScreenCornerRadius(),
        // Mini-to-full morph flight (see PlayerMorphHost): container owns the
        // card, artwork rides above it.
        overlayContent = {
            PlayerMorphHost(
                progress = { state.progress },
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                mediaId = mediaMetadata?.id,
                containerColor = AppTheme.colorScheme.surfaceContainer,
                expandedCornerRadius = rememberScreenCornerRadius(),
            )

        },
        background = {
            // The glass player paints its own background over this.
            Box(Modifier.fillMaxSize().background(Color.Black))
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {},
    ) {
        // The glass player is the only full player: the classic and V2 skins were only ever
        // drawn in the DIY editor preview, which is gone.
        GlassNowPlaying(state = state, navController = navController)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    positionProvider: () -> Long
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue =null)
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current.get()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(mediaMetadata?.id, currentLyrics) {
        if (mediaMetadata != null && currentLyrics == null) {
            delay(500)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.xyonmusic.music.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata)
                    database.withTransaction {
                        if (this.lyrics(mediaMetadata.id).first() == null)
                            upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider))
                    }
                } catch (_: Exception) {
                    // Lyrics are optional; a provider failure should not interrupt playback.
                }
            }
        }
    }

    Box (
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    Lyrics(
                        sliderPositionProvider = positionProvider,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        showLyrics = showLyrics
                    )
                }
                ProvideTextStyle(
                    value = AppTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

@Composable
private fun BackgroundVideoView(
    mediaId: String,
    videoUrl: String,
    isPlaying: Boolean,
    onError: () -> Unit = {},
    onReady: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Polled into periodically below so glass surfaces sampling this video's
    // backdrop can cache their blur/lens output per loop-position bucket
    // instead of recomputing every frame of every loop — see VideoLoopClock.
    loopClock: VideoLoopClock? = null,
    onFrame: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current.get()
    var isVideoReady by remember(videoUrl) { mutableStateOf(false) }
    // The reused player listener must see the latest callbacks and media ID.
    val currentOnError by rememberUpdatedState(onError)
    val currentOnReady by rememberUpdatedState(onReady)
    val currentMediaId by rememberUpdatedState(mediaId)
    val currentOnFrame by rememberUpdatedState(onFrame)
    // The clip's colours, sampled once (CanvasVideoLooks) so the frost/mesh can follow it.
    val sampleScope = rememberCoroutineScope()
    val textureHolder = remember { arrayOfNulls<TextureView>(1) }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setMaxVideoSize(4096, 4096)
                .setForceHighestSupportedBitrate(true)
                .build()
        }
    }

    val mediaSourceFactory = remember<DefaultMediaSourceFactory>(downloadUtil) {
        DefaultMediaSourceFactory(
            CacheDataSource.Factory()
                .setCache(downloadUtil.downloadCache)
                .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context))
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR),
            androidx.media3.extractor.DefaultExtractorsFactory()
        )
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setTargetBufferBytes(20 * 1024 * 1024) // 20MB buffer for 4K
                    .build()
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                playWhenReady = isPlaying
            }
    }

    if (loopClock != null) {
        LaunchedEffect(exoPlayer, loopClock) {
            while (isActive) {
                loopClock.update(exoPlayer.currentPosition, exoPlayer.duration)
                delay(VideoLoopBucketMs)
            }
        }
    }

    val aspectRatioFrameLayout = remember {
        AspectRatioFrameLayout(context).apply {
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatioFrameLayout.setAspectRatio(videoSize.width.toFloat() / videoSize.height)
                }
            }
            override fun onRenderedFirstFrame() {
                isVideoReady = true
                currentOnReady()
                val id = currentMediaId
                if (id.isNotBlank()) sampleScope.launch { sampleCanvasVideoLook(id) { textureHolder[0] } }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("BackgroundVideoView", "Video playback failed: ${error.errorCodeName} ${error.message}")
                currentOnError()
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(videoUrl, mediaId) {
        isVideoReady = false
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setCustomCacheKey("$mediaId#canvas")
            .setMimeType(if (videoUrl.contains("m3u8")) MimeTypes.APPLICATION_M3U8 else MimeTypes.VIDEO_MP4)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(800, easing = SeamlessEasing),
        label = "videoAlpha"
    )

    AndroidView(
        factory = { _ ->
            aspectRatioFrameLayout.apply {
                // Ensure the view doesn't capture touches intended for other sections
                isEnabled = false
                isClickable = false
                isFocusable = false

                // Ensure TextureView is added only once
                if (childCount == 0) {
                    val textureView = com.xyonmusic.music.artistvideo.VideoFrameTextureView(context).apply {
                        this.onFrame = { currentOnFrame?.invoke() }
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    addView(textureView)
                    textureHolder[0] = textureView
                    exoPlayer.setVideoTextureView(textureView)
                }
            }
        },
        modifier = modifier.graphicsLayer { this.alpha = alpha; clip = true }
    )
}

