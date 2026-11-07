/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The collapsed player exactly as in the original XyonMusic: a 58 dp, 20 dp-corner
 * glass bar with the cover, title/artist, Play/Pause, Next and a hairline progress
 * line, sitting above the tab bar. Drag it up or tap it to open the full player.
 */

package com.xyonmusic.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.extensions.togglePlayPause
import com.xyonmusic.music.ui.component.rememberNoArtworkPainter
import com.xyonmusic.music.ui.component.rememberPlaybackFraction
import com.xyonmusic.music.ui.ios.compat.AppTheme
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.liquidGlass

@Composable
fun XyonCollapsedPlayer(
    backdrop: GlassBackdrop,
    glassStyle: LiquidGlassStyle,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    if (mediaMetadata == null) return

    val haptics = LocalHapticFeedback.current
    val isDark = AppTheme.colorScheme.surface.luminance() <= 0.5f
    val shape = RoundedCornerShape(20.dp)
    // Matches the original XyonMusic collapsed bar: 45 % surface in the dark,
    // 55 % in the light theme, with the same 20 dp radius and 58 dp height.
    val miniGlass = glassStyle.copy(
        surface = if (isDark) Color(0xFF141419).copy(alpha = 0.45f) else Color(0xFFF7F8FA).copy(alpha = 0.55f),
        fallback = if (isDark) Color(0xFF141419).copy(alpha = 0.94f) else Color(0xFFF7F8FA).copy(alpha = 0.94f),
    )
    val fraction by rememberPlaybackFraction(playerConnection.player, isPlaying)
    var drag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .graphicsLayer { translationY = drag.coerceAtMost(0f) * 0.4f }
            .clip(shape)
            .liquidGlass(backdrop = backdrop, shape = shape, style = miniGlass)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (drag < -50f) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                ) { change, dy -> change.consume(); drag += dy }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            ),
    ) {
        // Apple's soft top shine, as in the original bar.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.06f else 0.35f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberNoArtworkPainter(),
                error = rememberNoArtworkPainter(),
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mediaMetadata?.title.orEmpty(),
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiniControl(
                icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentColor = contentColor,
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); playerConnection.player.togglePlayPause() },
            )
            MiniControl(
                icon = Icons.Rounded.SkipNext,
                contentColor = contentColor,
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove); playerConnection.seekToNext() },
            )
        }

        // Hairline progress line at the bottom edge, same as the original.
        val progress = fraction.coerceIn(0f, 1f)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(contentColor.copy(alpha = 0.12f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(IosTheme.tint),
            )
        }
    }
}

@Composable
private fun MiniControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(30.dp))
    }
}
