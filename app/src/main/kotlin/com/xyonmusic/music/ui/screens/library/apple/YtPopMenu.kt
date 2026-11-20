/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.screens.library.apple

import com.xyonmusic.music.ui.component.sf
import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.composables.sfsymbols.SfSymbol
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFDotRadiowavesLeftAndRight
import com.composables.sfsymbols.monochrome.SFEllipsis
import com.composables.sfsymbols.monochrome.SFMusicMicrophone
import com.composables.sfsymbols.monochrome.SFPlay
import com.composables.sfsymbols.monochrome.SFShuffle
import com.composables.sfsymbols.monochrome.SFSquareAndArrowUp
import com.composables.sfsymbols.monochrome.SFSquareStack
import com.composables.sfsymbols.monochrome.SFTextLineFirstAndArrowtriangleForward
import com.composables.sfsymbols.monochrome.SFTextLineLastAndArrowtriangleForward
import com.xyonmusic.music.R
import com.xyonmusic.music.extensions.toMediaItem
import com.xyonmusic.music.models.toMediaMetadata
import com.xyonmusic.music.playback.PlayerConnection
import com.xyonmusic.music.playback.queues.YouTubeAlbumRadio
import com.xyonmusic.music.playback.queues.YouTubeQueue
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.ui.ios.component.AppleContextMenu
import com.xyonmusic.music.ui.ios.component.IosGlyph
import com.xyonmusic.music.ui.ios.component.IosMenuItem
import com.xyonmusic.music.ui.ios.component.IosMenuSection
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.ui.utils.albumRoute
import com.xyonmusic.music.ui.utils.artistRoute
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem

/**
 * A YouTube Music tile: a tap runs [onClick], a press-and-hold opens the full item menu
 * ([onMore]), the same as every other menu in the app.
 */
@Composable
internal fun YtPopTile(
    item: YTItem,
    navController: NavController,
    player: PlayerConnection,
    subtitle: String,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // Holding a tile opens the full YouTube item menu, like every other menu in the app.
    androidx.compose.foundation.layout.Box(
        modifier.combinedClickable(
            interactionSource = null,
            indication = null,
            onClick = onClick,
            onLongClick = onMore,
            onLongClickLabel = item.title,
        ),
    ) { content() }
}
