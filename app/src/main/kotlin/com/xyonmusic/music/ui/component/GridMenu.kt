/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.component

import com.xyonmusic.music.ui.utils.MenuItemChrome
import com.xyonmusic.music.ui.utils.MenuIconChrome
import com.xyonmusic.music.ui.utils.additiveChrome
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import com.xyonmusic.music.ui.ios.compat.CircularProgressIndicator
import com.xyonmusic.music.ui.ios.compat.Icon
import com.xyonmusic.music.ui.ios.compat.LocalContentColor
import com.xyonmusic.music.ui.ios.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.xyonmusic.music.R
import com.xyonmusic.music.ui.component.shapes.ContinuousRoundedRectangle
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.utils.makeTimeString

val GridMenuItemHeight = 108.dp

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    tint: @Composable () -> Color = { LocalContentColor.current },
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = GridMenuItem(
    modifier = modifier,
    icon = {
        Icon(
            painter = painterResource(icon),
            tint = tint(),
            contentDescription = null
        )
    },
    title = title,
    enabled = enabled,
    onClick = onClick
)

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    item {
        Column(
            modifier = modifier
                .clip(ContinuousRoundedRectangle(18.dp))
                .height(GridMenuItemHeight)
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .alpha(if (enabled) 1f else 0.5f)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
                content = {
                    androidx.compose.runtime.CompositionLocalProvider(
                        LocalSymbolChrome provides MenuItemChrome,
                        LocalSymbolIconSize provides 22.dp,
                    ) { icon() }
                }
            )
            Text(
                text = stringResource(title),
                style = IosTheme.typography.caption,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) {
                        IosTheme.typography.caption.lineHeight.toDp() * 2
                    })
            )
        }
    }
}

