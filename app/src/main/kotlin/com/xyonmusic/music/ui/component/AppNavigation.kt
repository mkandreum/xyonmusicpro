/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.xyonmusic.music.constants.AppBackgroundColorKey
import com.xyonmusic.music.constants.AppTextColorKey
import com.xyonmusic.music.utils.rememberPreference
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import com.xyonmusic.music.ui.ios.compat.Icon
import com.xyonmusic.music.ui.ios.compat.AppTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import com.xyonmusic.music.ui.ios.compat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xyonmusic.music.ui.screens.Screens
import com.xyonmusic.music.ui.utils.pressWobble
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

@Stable
internal fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    return navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")
}

/**
 * The nav item whose puck/indicator should be lit.
 *
 * Returns the tab matching [currentRoute], or — while on a non-tab destination
 * (a drilled-in detail, a settings sub-page, etc.) where nothing matches — the
 * last tab that did match, so the indicator holds its place instead of snapping
 * back to the first tab (Home).
 *
 * This drives the VISUAL puck only. For click handling call [isRouteSelected]
 * directly: tapping a held-but-not-current tab must still navigate to it.
 */
@Composable
internal fun rememberStickySelectedRoute(
    currentRoute: String?,
    navigationItems: List<Screens>,
): String? {
    val matched = navigationItems.firstOrNull {
        isRouteSelected(currentRoute, it.route, navigationItems)
    }?.route
    // Held outside composition state on purpose: the bar is rebuilt whenever search opens or closes, and a
    // remembered copy was lost each time, leaving the selected slot empty on a page reached from search.
    LaunchedEffect(matched) { if (matched != null) lastSelectedTabRoute = matched }
    return matched ?: lastSelectedTabRoute
}

private var lastSelectedTabRoute: String? by mutableStateOf(null)

