/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * The app's mini player: LiquidUI's LiquidMiniPlayer fed with the current song. All the look and motion live
 * in LiquidUI (glass capsule, open/folded versions, swipe to skip, press bounce); this only wires the player.
 */

package com.xyonmusic.music.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.R
import com.xyonmusic.music.ui.component.AnimatedPlayPauseIcon
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.size
import com.xyonmusic.music.constants.SwipeThumbnailKey
import com.xyonmusic.music.extensions.togglePlayPause
import com.xyonmusic.music.ui.component.rememberNoArtworkPainter
import com.xyonmusic.music.utils.rememberPreference
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.ui.LiquidMiniPlayer

@Composable
fun XyonCollapsedPlayer(
    backdrop: GlassBackdrop,
    glassStyle: LiquidGlassStyle,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    inline: () -> Float = { 0f },
    expanded: () -> Float = { 1f },
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val swipeEnabled by rememberPreference(SwipeThumbnailKey, true)
    val song = metadata ?: return

    LiquidMiniPlayer(
        backdrop = backdrop,
        title = song.title,
        subtitle = song.artists.joinToString { it.name },
        isPlaying = isPlaying,
        onPlayPause = { playerConnection.player.togglePlayPause() },
        onOpen = onClick,
        modifier = modifier,
        artwork = { m ->
            val placeholder = rememberNoArtworkPainter()
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = placeholder,
                error = placeholder,
                modifier = m,
            )
        },
        onNext = if (canSkipNext) ({ playerConnection.seekToNext() }) else null,
        onSkip = if (swipeEnabled) { next ->
            if (next) {
                if (playerConnection.player.nextMediaItemIndex != -1) playerConnection.seekToNext()
            } else if (playerConnection.player.previousMediaItemIndex != -1) {
                playerConnection.seekToPrevious()
            }
        } else null,
        onLongPress = onLongClick,
        inline = inline,
        expanded = expanded,
        glass = glassStyle,
        contentColor = contentColor,
        // Same glyphs as before: the animated SF play/pause and the fast-forward icon.
        playGlyph = { playing, size -> AnimatedPlayPauseIcon(isPlaying = playing, tint = contentColor, size = size) },
        nextGlyph = { size ->
            Icon(painterResource(R.drawable.fast_forward), null, tint = contentColor, modifier = Modifier.size(size))
        },
    )
}
