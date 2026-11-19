/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */
package com.xyonmusic.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.xyonmusic.music.ui.screens.library.LibraryScreen
import com.xyonmusic.music.ui.screens.settings.SettingsScreen

/** Main pager pages in tab-bar order. Search is an overlay, not a pager page. */
val MainTabsScreens = listOf(
    Screens.Home,
    Screens.Library,
    Screens.Settings,
)

const val MainTabsRoute = "main_tabs"

