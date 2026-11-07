/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Horizontal CoverFlow for albums imported into XyonMusic. It is the same
 * interaction as the original XyonMusic library: drag to travel, tilt the
 * neighbouring covers in 3D, tap the centre card to open it.
 */

package com.xyonmusic.music.ui.screens.library.apple

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.xyonmusic.music.R
import com.xyonmusic.music.db.entities.Playlist
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.ui.theme.SfProFontFamily
import com.xyonmusic.music.ui.component.rememberNoArtworkPainter
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The XyonMusic horizontal CoverFlow. [playlists] are the imported albums, in the
 * order they arrived; [onOpen] receives the tapped centre card.
 */
@Composable
fun ImportedAlbumsCoverFlow(
    playlists: List<Playlist>,
    onOpen: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) return
    val colors = IosTheme.colors
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pos = remember(playlists.size) { Animatable(0f) }
    val centered by remember { derivedStateOf { pos.value.roundToInt().coerceIn(0, playlists.lastIndex) } }
    LaunchedEffect(centered) {
        if (pos.isRunning || pos.value != 0f) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(292.dp),
    ) {
        val cover = minOf(232.dp, maxWidth * 0.30f, maxHeight * 0.66f)
        val coverPx = with(density) { cover.toPx() }
        val spacing = coverPx * 0.46f
        val tracker = remember { VelocityTracker() }
        val placeholder = rememberNoArtworkPainter()

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(playlists.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = {
                            val v = tracker.calculateVelocity().x
                            val target = (pos.value - v / spacing * 0.25f)
                                .roundToInt()
                                .coerceIn(0, playlists.lastIndex)
                                .toFloat()
                            scope.launch {
                                pos.animateTo(
                                    target,
                                    spring(dampingRatio = 0.82f, stiffness = 150f),
                                    -v / spacing,
                                )
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                pos.animateTo(
                                    pos.value.roundToInt().toFloat().coerceIn(0f, playlists.lastIndex.toFloat()),
                                    spring(dampingRatio = 0.9f, stiffness = 240f),
                                )
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch {
                            pos.snapTo((pos.value - dragAmount / spacing).coerceIn(-0.35f, playlists.lastIndex + 0.35f))
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val p = pos.value
            val first = floor(p - 4).toInt().coerceAtLeast(0)
            val last = (p + 4).toInt().coerceAtMost(playlists.lastIndex)
            for (i in first..last) {
                val playlist = playlists[i]
                val d = i - p
                val ad = abs(d)
                key(playlist.id) {
                    Column(
                        modifier = Modifier
                            .zIndex(-ad)
                            .graphicsLayer {
                                cameraDistance = 14f * density.density
                                val t = minOf(ad / 0.5f, 1f)
                                translationX = sign(d) * coverPx * 0.25f * t + spacing * d
                                rotationY = -sign(d) * 65f * t
                                val s = 1f - t * 0.12f
                                scaleX = s
                                scaleY = s
                                alpha = 1f - minOf(ad, 1f) * 0.3f
                            }
                            .width(cover)
                            .padding(bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            Modifier
                                .size(cover)
                                .shadow(18.dp, RoundedCornerShape(10.dp), clip = false)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.secondarySystemFill)
                                .pointerInput(playlist.id) {
                                    awaitEachTap {
                                        if (i == centered) {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onOpen(playlist)
                                        } else {
                                            scope.launch {
                                                pos.animateTo(i.toFloat(), spring(dampingRatio = 0.85f, stiffness = 220f))
                                            }
                                        }
                                    }
                                },
                        ) {
                            AsyncImage(
                                model = playlist.thumbnails.firstOrNull(),
                                contentDescription = playlist.title,
                                contentScale = ContentScale.Crop,
                                placeholder = placeholder,
                                error = placeholder,
                                modifier = Modifier.fillMaxSize(),
                            )
                            // Dim the covers that are not the current one.
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = minOf(ad, 1f) * 0.35f)),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        val titleAlpha = (1f - ad * 2f).coerceIn(0f, 1f)
                        Text(
                            text = playlist.title,
                            modifier = Modifier
                                .width(cover)
                                .graphicsLayer { alpha = titleAlpha },
                            color = colors.label,
                            fontFamily = SfProFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Spotify · " + pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount),
                            modifier = Modifier
                                .width(cover)
                                .graphicsLayer { alpha = titleAlpha },
                            color = Color(0xFF1ED760),
                            fontFamily = SfProFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** A small tap detector that never steals the horizontal drags of the flow. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitEachTap(onTap: () -> Unit) {
    detectTapGestures(onTap = { onTap() })
}
