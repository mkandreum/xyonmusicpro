/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Mini reproductor portado de la barra de Convx (FloatingMiniPlayer): una cápsula de cristal encima de las
 * pestañas cuando la barra está abierta (carátula, título/artista, play/pausa y siguiente) y una tira fina
 * entre el círculo de la pestaña y el de buscar cuando se pliega (carátula, título/artista y play/pausa).
 * Las dos versiones se funden dentro de los mismos límites, que viajan como elemento compartido.
 * Deslizar en horizontal cambia de canción; tocar abre el reproductor; al pulsar crece un 4 %.
 */

package com.xyonmusic.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.SwipeSensitivityKey
import com.xyonmusic.music.constants.SwipeThumbnailKey
import com.xyonmusic.music.extensions.togglePlayPause
import com.xyonmusic.music.ui.component.AnimatedPlayPauseIcon
import com.xyonmusic.music.ui.component.backdrop.catalog.utils.InteractiveHighlight
import com.xyonmusic.music.ui.component.rememberNoArtworkPainter
import com.xyonmusic.music.ui.ios.compat.AppTheme
import com.xyonmusic.music.utils.rememberPreference
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun XyonCollapsedPlayer(
    backdrop: GlassBackdrop,
    glassStyle: LiquidGlassStyle,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    /** Opacidad de la versión plegada (tira entre los círculos). */
    inline: () -> Float = { 0f },
    /** Opacidad de la versión abierta (cápsula encima de las pestañas). */
    expanded: () -> Float = { 1f },
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    if (mediaMetadata == null) return

    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeEnabled by rememberPreference(SwipeThumbnailKey, true)
    val haptics = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val isDark = AppTheme.colorScheme.surface.luminance() <= 0.5f
    val miniGlass = glassStyle

    val offsetX = remember { Animatable(0f) }
    var dragStart by remember { mutableLongStateOf(0L) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val settle = remember { spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow) }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    // Distancia para cambiar de canción: 45 % del ancho con sensibilidad 0, 12 % con 1 (como Convx).
    val threshold = remember(swipeSensitivity, screenWidthDp, density) {
        (screenWidthDp * density * (0.45f - 0.33f * swipeSensitivity.coerceIn(0f, 1f))).roundToInt()
    }

    // Respuesta al pulsar de iOS 26: toda la cápsula crece un poco mientras se toca.
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accessoryPressScale",
    )
    val highlight = remember(scope) { InteractiveHighlight(animationScope = scope) }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(CircleShape)
            .liquidGlass(backdrop = backdrop, shape = CircleShape, style = miniGlass)
            .then(highlight.modifier)
            .clipToBounds()
            .then(highlight.gestureModifier)
            .then(
                if (!swipeEnabled) Modifier else Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragStart = System.currentTimeMillis(); dragDistance = 0f },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, settle) } },
                        onHorizontalDrag = { _, amount ->
                            val d = if (layoutDirection == LayoutDirection.Rtl) -amount else amount
                            val canPrev = playerConnection.player.previousMediaItemIndex != -1
                            val canNext = playerConnection.player.nextMediaItemIndex != -1
                            val back = (d > 0 && !canPrev && offsetX.value < 0) || (d < 0 && !canNext && offsetX.value > 0)
                            if ((d < 0 && canNext) || (d > 0 && canPrev) || back) {
                                dragDistance += abs(d)
                                scope.launch { offsetX.snapTo(offsetX.value + d) }
                            }
                        },
                        onDragEnd = {
                            val ms = System.currentTimeMillis() - dragStart
                            val velocity = if (ms > 0) dragDistance / ms else 0f
                            val current = offsetX.value
                            val dragged = abs(current)
                            if (dragged > threshold || (velocity > 0.55f && dragged > threshold * 0.25f)) {
                                if (current > 0 && playerConnection.player.previousMediaItemIndex != -1) playerConnection.seekToPrevious()
                                else if (current <= 0 && playerConnection.player.nextMediaItemIndex != -1) playerConnection.seekToNext()
                            }
                            scope.launch { offsetX.animateTo(0f, settle) }
                        },
                    )
                }
            ),
    ) {
        val rowModifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .combinedClickable(
                interactionSource = press,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            )
        val artist = mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
        val title = mediaMetadata?.title.orEmpty()
        val togglePlay = { playerConnection.player.togglePlayPause() }

        // Plegada: tira fija — carátula 36 dp (radio 9), título/artista pequeños y play/pausa.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier
                .graphicsLayer { alpha = inline() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            MiniArt(mediaMetadata?.thumbnailUrl, corner = 9)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = contentColor, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = contentColor.copy(alpha = 0.7f), fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = togglePlay, enabled = inline() > 0.5f, modifier = Modifier.size(36.dp)) {
                AnimatedPlayPauseIcon(isPlaying = isPlaying, tint = contentColor, size = 20.dp)
            }
        }

        // Abierta: cápsula ancha — carátula, título/artista, play/pausa y siguiente.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = rowModifier
                .graphicsLayer { alpha = expanded() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            MiniArt(mediaMetadata?.thumbnailUrl, corner = 8)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = contentColor, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            IconButton(onClick = togglePlay, enabled = expanded() > 0.5f, modifier = Modifier.size(36.dp)) {
                AnimatedPlayPauseIcon(isPlaying = isPlaying, tint = contentColor, size = 22.dp)
            }
            IconButton(
                onClick = { playerConnection.seekToNext() },
                enabled = canSkipNext && expanded() > 0.5f,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.fast_forward),
                    contentDescription = null,
                    tint = if (canSkipNext) contentColor else contentColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniArt(url: String?, corner: Int) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = rememberNoArtworkPainter(),
        error = rememberNoArtworkPainter(),
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(corner.dp)),
    )
}
