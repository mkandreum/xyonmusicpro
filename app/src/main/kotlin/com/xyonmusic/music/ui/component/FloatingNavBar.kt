/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.xyonmusic.music.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.sfsymbols.SfRenderingMode
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.SearchSource
import com.xyonmusic.music.ui.component.backdrop.catalog.utils.InteractiveHighlight
import com.xyonmusic.music.ui.component.floatingtabbar.FloatingTabBarScrollConnection
import com.xyonmusic.music.ui.ios.compat.AppTheme
import com.xyonmusic.music.ui.ios.component.IosContextMenuOverlay
import com.xyonmusic.music.ui.ios.component.IosMenuSection
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.ui.player.XyonCollapsedPlayer
import com.xyonmusic.music.ui.screens.Screens
import com.xyonmusic.music.ui.screens.search.DynamicSearchPlaceholder
import com.xyonmusic.music.ui.utils.bounceClick
import com.xyonmusic.music.ui.utils.additiveChrome
import io.github.mkandreum.liquidbar.LiquidBar
import io.github.mkandreum.liquidbar.LiquidMediaBar
import io.github.mkandreum.liquidbar.LiquidBarAction
import io.github.mkandreum.liquidbar.LiquidBarColors
import io.github.mkandreum.liquidbar.LiquidBarFoldState
import io.github.mkandreum.liquidbar.LiquidBarItem
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.liquidGlass as nativeLiquidGlass
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import kotlinx.coroutines.delay

private val NavBarShape = CircleShape
private val NavBarSearchBarHeight = 48.dp
private val NavBarRowHeight = 64.dp
private const val KeyboardOpenDelayMs = 260L

/**
 * Bottom navigation bar built on the native XyonMusic LiquidBar: a draggable glass
 * lens, spring travel, elastic press and the collapse-on-scroll fold. The mini
 * player keeps its place directly above it.
 */
@Composable
fun AppFloatingNavBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    glassBackdrop: GlassBackdrop,
    foldState: LiquidBarFoldState,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    showPlayerAccessory: Boolean = false,
    onAccessoryClick: () -> Unit = {},
    onAccessoryLongClick: (() -> Unit)? = null,
    tabContextMenu: (@Composable (Screens) -> List<IosMenuSection>)? = null,
) {
    val navSearch = LocalNavSearchState.current
    CompositionLocalProvider(LocalSampledChromeDark provides (if (com.xyonmusic.music.ui.theme.LocalForcedDark.current) true else null)) {
        AnimatedContent(
            targetState = navSearch.keyboardActive,
            modifier = modifier,
            transitionSpec = {
                if (targetState) {
                    // The field grows out of the search circle (bottom-right), like LiquidUI's search morph.
                    (androidx.compose.animation.scaleIn(
                        spring(dampingRatio = 0.7f, stiffness = 300f),
                        initialScale = 0.2f,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f),
                    ) + fadeIn(androidx.compose.animation.core.tween(160))) togetherWith
                        (androidx.compose.animation.scaleOut(
                            spring(dampingRatio = 1f, stiffness = 500f),
                            targetScale = 0.92f,
                        ) + fadeOut(androidx.compose.animation.core.tween(120)))
                } else {
                    (slideInVertically(
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    ) { it } + fadeIn()) togetherWith
                        (slideOutVertically(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        ) { it / 2 } + fadeOut())
                }
            },
            label = "navBarKeyboardActive",
        ) { keyboardActive ->
            if (keyboardActive) {
                NavBarSearchInputBar(
                    state = navSearch,
                    pureBlack = pureBlack,
                    glassBackdrop = glassBackdrop,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LiquidNavBarChrome(
                    navigationItems = navigationItems,
                    currentRoute = currentRoute,
                    onItemClick = onItemClick,
                    pureBlack = pureBlack,
                    showPlayerAccessory = showPlayerAccessory,
                    onAccessoryClick = onAccessoryClick,
                    onAccessoryLongClick = onAccessoryLongClick,
                    tabContextMenu = tabContextMenu,
                    searchModeActive = navSearch.visualActive,
                    navSearch = navSearch,
                    backdrop = glassBackdrop,
                    foldState = foldState,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LiquidNavBarChrome(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    pureBlack: Boolean,
    showPlayerAccessory: Boolean,
    onAccessoryClick: () -> Unit,
    onAccessoryLongClick: (() -> Unit)?,
    tabContextMenu: (@Composable (Screens) -> List<IosMenuSection>)?,
    searchModeActive: Boolean,
    navSearch: NavSearchState,
    backdrop: GlassBackdrop,
    foldState: LiquidBarFoldState,
    modifier: Modifier,
) {
    BackHandler(enabled = searchModeActive, onBack = navSearch.onExit)
    val navContext = androidx.compose.ui.platform.LocalContext.current

    val isDark = AppTheme.colorScheme.surface.luminance() <= 0.5f
    val selectedColor = IosTheme.tint
    val contentColor = AppTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val glassStyle = if (isDark) LiquidGlassStyle.Dark else LiquidGlassStyle.Light
    val colors = LiquidBarColors(
        accent = selectedColor,
        content = contentColor,
        indicator = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f),
    )
    val accessoryContentColor = when {
        pureBlack -> Color.White
        else -> AppTheme.colorScheme.onSurface
    }

    // Search is the bar's action: last cell while open, its own circle on the right when folded.
    val searchScreen = navigationItems.firstOrNull { it == Screens.Search }
    val tabScreens = remember(navigationItems) { navigationItems.filter { it != Screens.Search } }
    val selectedTabKey = rememberStickySelectedRoute(currentRoute, tabScreens)
    val barScreens = tabScreens
    val selectedIndex = barScreens.indexOfFirst { it.route == selectedTabKey }.coerceAtLeast(0)

    var heldTab by remember { mutableStateOf<Pair<Screens, Rect>?>(null) }
    val heldSections = heldTab?.let { (screen, _) -> tabContextMenu?.invoke(screen) }.orEmpty()
    IosContextMenuOverlay(
        sections = heldSections,
        anchor = heldTab?.second?.takeIf { heldSections.isNotEmpty() },
        onDismissRequest = { heldTab = null },
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LiquidMediaBar(
            backdrop = backdrop,
            items = barScreens.map { screen ->
                LiquidBarItem(
                    label = stringResource(screen.titleId),
                    iconContent = { tint ->
                        val name = screen.navSymbol()
                        if (name != null) {
                            SymbolIcon(
                                symbol = sf(name),
                                contentDescription = stringResource(screen.titleId),
                                size = screen.glyphSize() * 0.8f,
                                chrome = 0f,
                                tint = tint,
                                mode = SfRenderingMode.Monochrome,
                            )
                        } else {
                            Icon(
                                painter = painterResource(screen.icon(appleMusicUi = true)),
                                contentDescription = stringResource(screen.titleId),
                                tint = tint,
                                modifier = Modifier.size(screen.glyphSize()),
                            )
                        }
                    },
                )
            },
            selectedIndex = selectedIndex,
            onSelect = { index ->
                val screen = barScreens.getOrNull(index) ?: return@LiquidMediaBar
                // A firmer click when the tab actually changes (the global tap tick covers taps).
                com.xyonmusic.music.utils.Haptics.click(navContext)
                when {
                    screen == Screens.Search -> if (searchModeActive) navSearch.onTapBar() else navSearch.onTapSearchIcon()
                    // On Descubrir (the search landing) the bar is in search mode: staying put is a no-op,
                    // any other tab navigates there directly (tap or lens drag), instead of only closing search.
                    searchModeActive && screen == Screens.Discover -> Unit
                    else -> onItemClick(screen, isRouteSelected(currentRoute, screen.route, navigationItems))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            foldState = foldState,
            colors = colors,
            glass = glassStyle,
            rowHeight = NavBarRowHeight,
            labelStyle = TextStyle(fontSize = 10.sp, lineHeight = 12.sp),
            // Search is always its own circle on the right, apart from the tabs.
            detachAction = true,
            onItemLongClick = { index, rect ->
                val screen = barScreens.getOrNull(index)
                if (screen != null && tabContextMenu != null) heldTab = screen to rect
            },
            // Same dynamic as the reference bar: the mini player sits above the tabs and, when
            // the bar folds on scroll, slides down into the row beside the tab circle.
            action = searchScreen?.let { screen ->
                LiquidBarAction(
                    label = stringResource(screen.titleId),
                    iconContent = { tint ->
                        SymbolIcon(
                            symbol = sf("magnifyingglass"),
                            contentDescription = stringResource(screen.titleId),
                            size = screen.glyphSize() * 0.8f,
                            chrome = 0f,
                            tint = tint,
                            mode = SfRenderingMode.Monochrome,
                        )
                    },
                    onClick = {
                        // Always opens the search field: from Descubrir directly, from elsewhere after moving there.
                        if (!searchModeActive) navSearch.onTapSearchIcon()
                        navSearch.onTapBar()
                    },
                )
            },
            accessory = if (showPlayerAccessory) {
                { inline, expanded ->
                    XyonCollapsedPlayer(
                        backdrop = backdrop,
                        glassStyle = glassStyle,
                        contentColor = accessoryContentColor,
                        onClick = onAccessoryClick,
                        onLongClick = onAccessoryLongClick,
                        modifier = Modifier.fillMaxSize(),
                        inline = inline,
                        expanded = expanded,
                    )
                }
            } else null,
        )
    }
}

private val NavInactiveGreyDark = Color(0xFF8E8E93)
private val NavInactiveGreyLight = Color(0xFF6C6C70)

private fun Screens.glyphSize() = when (this) {
    Screens.Explore, Screens.Search -> 28.dp
    else -> 32.dp
}

internal fun Screens.navSymbol(): String? = when (this) {
    Screens.Home -> "house.fill"
    Screens.Library -> "music.note.square.stack.fill"
    Screens.Explore -> "square.grid.2x2.fill"
    Screens.Search -> "magnifyingglass"
    Screens.Songs, Screens.LibSongs -> "music.note"
    Screens.LibPlaylists -> "music.note.list"
    Screens.Discover -> "square.grid.2x2.fill"
    Screens.Downloads -> "arrow.down.circle.fill"
    Screens.Radio -> "dot.radiowaves.left.and.right"
    else -> null
}

/** Search input that replaces the navigation bar while focused and follows the keyboard. */
@Composable
fun NavBarSearchInputBar(
    state: NavSearchState,
    pureBlack: Boolean,
    modifier: Modifier,
    glassBackdrop: GlassBackdrop? = null,
) {
    val isDark = AppTheme.colorScheme.surface.luminance() <= 0.5f
    val onTint = when {
        pureBlack -> Color.White
        else -> AppTheme.colorScheme.onSurface
    }
    val searchIconTint = if (!isDark) {
        com.xyonmusic.music.ui.theme.GlassTintTokens.IconOpaqueBlackishLight
    } else onTint
    val pillShape = CircleShape
    val coroutineScope = rememberCoroutineScope()
    val pillGlow = remember(coroutineScope) { InteractiveHighlight(animationScope = coroutineScope) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    BackHandler {
        focusManager.clearFocus()
        keyboardController?.hide()
        state.onCloseKeyboard()
    }

    LaunchedEffect(Unit) {
        delay(KeyboardOpenDelayMs)
        state.focusRequester.requestFocus()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 2.dp)
            .height(NavBarSearchBarHeight)
            .graphicsLayer(elasticStretchLayerBlock(pillGlow))
            .clip(pillShape)
            .then(
                if (glassBackdrop != null) {
                    Modifier.nativeLiquidGlass(
                        backdrop = glassBackdrop,
                        shape = pillShape,
                        style = if (isDark) LiquidGlassStyle.Dark else LiquidGlassStyle.Light,
                    )
                } else {
                    Modifier.background(onTint.copy(alpha = 0.15f))
                }
            )
            .then(pillGlow.gestureModifier)
            .then(pillGlow.modifier)
            .padding(start = 11.dp, end = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .bounceClick { state.onExit() }
                .padding(12.dp)
        ) {
            SymbolIcon(bounceOnEnter = true, symbol = sf("arrow.left"), contentDescription = stringResource(R.string.dismiss), modifier = Modifier.fillMaxSize().additiveChrome(isDark), tint = searchIconTint)
        }
        Box(modifier = Modifier.weight(1f)) {
            if (state.query.text.isEmpty()) {
                DynamicSearchPlaceholder(
                    searchSource = state.searchSource,
                    style = TextStyle(color = onTint.copy(alpha = 0.6f), fontSize = 17.sp),
                )
            }
            BasicTextField(
                value = state.query,
                onValueChange = state.onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = onTint, fontSize = 17.sp),
                cursorBrush = SolidColor(onTint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { state.onSubmit(state.query.text) },
                    onDone = { state.onSubmit(state.query.text) },
                    onGo = { state.onSubmit(state.query.text) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        if (it.isFocused && !state.keyboardActive) state.onTapBar()
                    }
                    .focusRequester(state.focusRequester)
                    .onKeyEvent {
                        if (it.key == Key.Enter) {
                            state.onSubmit(state.query.text)
                            true
                        } else {
                            false
                        }
                    }
            )
        }
        if (state.query.text.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { state.onQueryChange(TextFieldValue("")) }
                    .padding(12.dp)
            ) {
                SymbolIcon(bounceOnEnter = true, symbol = sf("xmark"), contentDescription = null, modifier = Modifier.fillMaxSize().additiveChrome(isDark), tint = searchIconTint)
            }
        }
        if (state.canToggleSource) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .bounceClick { state.onToggleSource() }
                    .padding(12.dp)
            ) {
                SymbolIcon(
                    bounceOnEnter = true,
                    symbol = when (state.searchSource) {
                        SearchSource.LOCAL -> sf("music.note.list")
                        SearchSource.ONLINE -> sf("globe")
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().additiveChrome(isDark),
                    tint = searchIconTint,
                )
            }
        }
    }
}
