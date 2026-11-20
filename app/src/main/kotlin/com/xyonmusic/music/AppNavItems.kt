/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.xyonmusic.music.constants.LocalOnlyModeKey
import com.xyonmusic.music.constants.HiddenNavigationRoutesKey
import com.xyonmusic.music.ui.screens.Screens
import com.xyonmusic.music.utils.rememberPreference

/** Whether the app runs on device files only. Provided once at the root; screens read this instead of the preference. */
val LocalLocalOnlyMode = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Destinations shown by the app's navigation bars. */
@Stable
class AppNavItems(
    val localOnlyMode: Boolean,
    val navigationItems: List<Screens>,
    val floatingNavigationItems: List<Screens>,
)

@Composable
fun rememberAppNavItems(): AppNavItems {
    // Read once here because both navigation bars use this preference.
    val (localOnlyMode) = rememberPreference(LocalOnlyModeKey, false)
    val (hiddenNavigationRoutes) = rememberPreference(HiddenNavigationRoutesKey, "")
    val hiddenRoutes = remember(hiddenNavigationRoutes) {
        hiddenNavigationRoutes.lineSequence().map(String::trim).filter(String::isNotEmpty).toSet()
    }
    val navigationItems = remember(localOnlyMode, hiddenRoutes) {
        Screens.mainScreens(localOnlyMode).filter { it == Screens.Search || it.route !in hiddenRoutes }
    }
    // Explore is only shown in the floating navigation bar, and is online-only.
    val floatingNavigationItems = remember(navigationItems, localOnlyMode, hiddenRoutes) {
        navigationItems
    }
    return remember(localOnlyMode, navigationItems, floatingNavigationItems) {
        AppNavItems(localOnlyMode, navigationItems, floatingNavigationItems)
    }
}
