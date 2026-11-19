/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.screens.library.apple

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.res.painterResource
import com.xyonmusic.music.ui.utils.sharedArtworkSource

import com.xyonmusic.music.ui.component.sf
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.composables.sfsymbols.SfSymbol
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFArrowDownToLine
import com.composables.sfsymbols.monochrome.SFChevronLeft
import com.composables.sfsymbols.monochrome.SFMusicNoteList
import com.composables.sfsymbols.monochrome.SFMusicMicrophone
import com.composables.sfsymbols.monochrome.SFMusicNoteHouse
import com.composables.sfsymbols.monochrome.SFPlayFill
import com.composables.sfsymbols.monochrome.SFPlus
import com.composables.sfsymbols.monochrome.SFShuffle
import com.composables.sfsymbols.monochrome.SFSquareAndArrowUp
import com.composables.sfsymbols.monochrome.SFSquareStack
import com.composables.sfsymbols.monochrome.SFStar
import com.composables.sfsymbols.monochrome.SFStarFill
import com.composables.sfsymbols.monochrome.SFTextBadgePlus
import com.composables.sfsymbols.monochrome.SFTextLineFirstAndArrowtriangleForward
import com.composables.sfsymbols.monochrome.SFTextLineLastAndArrowtriangleForward
import com.xyonmusic.music.LocalDatabase
import com.xyonmusic.music.LocalDownloadUtil
import com.xyonmusic.music.LocalPlayerAwareWindowInsets
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.AlbumSortDescendingKey
import com.xyonmusic.music.constants.AlbumSortType
import com.xyonmusic.music.constants.AlbumSortTypeKey
import com.xyonmusic.music.constants.ArtistSortDescendingKey
import com.xyonmusic.music.constants.ArtistSortType
import com.xyonmusic.music.constants.ArtistSortTypeKey
import com.xyonmusic.music.constants.LibraryViewType
import com.xyonmusic.music.constants.PlaylistSortDescendingKey
import com.xyonmusic.music.constants.PlaylistSortType
import com.xyonmusic.music.constants.PlaylistSortTypeKey
import com.xyonmusic.music.constants.PlaylistViewTypeKey
import com.xyonmusic.music.constants.SongSortDescendingKey
import com.xyonmusic.music.constants.SongSortType
import com.xyonmusic.music.constants.SongSortTypeKey
import com.xyonmusic.music.db.entities.Artist
import com.xyonmusic.music.db.entities.LocalItem
import com.xyonmusic.music.db.entities.Song
import com.xyonmusic.music.extensions.toMediaItem
import com.xyonmusic.music.playback.DownloadTarget
import com.xyonmusic.music.playback.downloadSongs
import com.xyonmusic.music.playback.queues.ListQueue
import com.xyonmusic.music.ui.component.CreatePlaylistDialog
import com.xyonmusic.music.ui.component.TopFadeBlur
import com.xyonmusic.music.ui.component.ListScrollRail
import com.xyonmusic.music.ui.component.rememberTitleCollapseProgressState
import com.xyonmusic.music.ui.ios.compat.DropdownMenu
import com.xyonmusic.music.ui.ios.compat.DropdownMenuItem
import com.xyonmusic.music.ui.ios.compat.IosMenuDivider
import com.xyonmusic.music.ui.ios.compat.menuTrigger
import com.xyonmusic.music.ui.ios.compat.LocalContentColor
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.ui.ios.component.AppleContextMenu
import com.xyonmusic.music.ui.ios.component.IosCheckmark
import com.xyonmusic.music.ui.ios.component.IosGlyph
import com.xyonmusic.music.ui.ios.component.IosMenuItem
import com.xyonmusic.music.ui.ios.component.IosMenuSection
import com.xyonmusic.music.ui.ios.foundation.IosMenuMaterial
import com.xyonmusic.music.ui.ios.foundation.IosMetrics
import com.xyonmusic.music.ui.ios.foundation.iosGlass
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.ui.ios.foundation.iosRowPress
import com.xyonmusic.music.ui.feature.SongsCoverFlowState
import com.xyonmusic.music.ui.menu.AddToPlaylistDialog
import com.xyonmusic.music.ui.utils.albumRoute
import com.xyonmusic.music.ui.utils.artistRoute
import com.xyonmusic.music.ui.utils.localPlaylistRoute
import com.xyonmusic.music.utils.rememberEnumPreference
import com.xyonmusic.music.utils.rememberPreference
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.ui.LiquidGlassButton
import com.music.spotify.SpotifyHomeItem
import com.music.spotify.SpotifyHomeItemType
import com.music.spotify.SpotifyHomeShelf
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyImage
import com.music.spotify.models.SpotifyPlaylist
import com.xyonmusic.music.ui.screens.settings.SpotifyPlaylistBottomSheet
import com.xyonmusic.music.ui.screens.settings.SpotifyPlaylistDetailSheet
import com.xyonmusic.music.viewmodels.LibraryAlbumsViewModel
import com.xyonmusic.music.viewmodels.LibraryArtistsViewModel
import com.xyonmusic.music.viewmodels.LibraryPlaylistsViewModel
import com.xyonmusic.music.viewmodels.LibrarySongsViewModel
import com.xyonmusic.music.viewmodels.SpotifyImportProgress
import com.xyonmusic.music.viewmodels.SpotifyImportViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.xyonmusic.music.ui.component.backdrop.backdrops.layerBackdrop
import kotlinx.coroutines.launch

// Shared subpage header: the large title scrolls beneath the toolbar and fades into its
// compact title. The toolbar also provides a back button and optional trailing actions.

/** True when a library list is shown as a bar tab: no back button and no back handling. */
val LocalSubpageIsTab = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
internal fun AppleSubpage(
    title: String,
    onBack: () -> Unit,
    backLabel: String = stringResource(R.string.filter_library),
    state: LazyListState = rememberLazyListState(),
    trailing: @Composable RowScope.() -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val isTab = LocalSubpageIsTab.current
    BackHandler(enabled = !isTab, onBack = onBack)
    val colors = IosTheme.colors
    val haze = remember { HazeState() }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val sideInset = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        .calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
    val titleGutter = if (sideInset > 0.dp) 22.dp else 16.dp
    // Tab pages have no back button: put the large title on the actions row itself, at the very
    // top, and shrink it only enough for long titles to fit beside them.
    val headerHeight = if (isTab) 44.dp else com.xyonmusic.music.ui.component.TopBarContentHeight
    val titleTextSize = if (!isTab) 34.sp else when {
        title.length > 18 -> 24.sp
        title.length > 12 -> 30.sp
        else -> 34.sp
    }
    val titleLineHeight = if (!isTab) 41.sp else when {
        title.length > 18 -> 29.sp
        title.length > 12 -> 36.sp
        else -> 41.sp
    }
    val contentTop = if (isTab) statusTop + 2.dp else statusTop + headerHeight
    val collapse = rememberTitleCollapseProgressState(state, titleItemKey = LargeTitleKey)
    // Sample this page's list instead of the app root. The toolbar sits inside the root's
    // captured layer, so sampling the root would recurse through the toolbar and crash RenderNode.
    val pageColor = colors.systemBackground
    val pageBackdrop = com.xyonmusic.music.ui.component.backdrop.backdrops.rememberLayerBackdrop(
        onDraw = remember(pageColor) { { drawRect(pageColor); drawContent() } },
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.systemBackground),
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .layerBackdrop(pageBackdrop)
                .graphicsLayer()
                .hazeSource(haze),
            // The side inset is padding, not a narrower list, so rows scroll under the side bar.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = sideInset,
                top = contentTop,
                bottom = bottom + 16.dp,
            ),
        ) {
            item(key = LargeTitleKey, contentType = "large_title") {
                Text(
                    text = title,
                    style = SfStyle(titleTextSize, titleLineHeight, FontWeight.Bold, (-0.6).sp).copy(fontFamily = com.xyonmusic.music.ui.theme.DisplayFontFamily),
                    color = colors.label,
                    maxLines = 1,
                    modifier = Modifier.padding(
                        start = titleGutter,
                        // Leave the trailing glass capsule its own lane on tab pages.
                        end = if (isTab) 100.dp else 16.dp,
                        top = if (isTab) 2.dp else 10.dp,
                        bottom = 8.dp,
                    ),
                )
            }
            content()
        }

        // Tab pages put the title beside the actions, so the fade only covers the status bar.
        // Pushed pages keep the original fade under their full toolbar.
        TopFadeBlur(
            hazeState = haze,
            pageColor = colors.systemBackground,
            modifier = Modifier.align(Alignment.TopCenter),
            heightOverride = if (isTab) statusTop + 2.dp else statusTop + headerHeight + 8.dp,
        )

        androidx.compose.runtime.CompositionLocalProvider(com.xyonmusic.music.ui.ios.foundation.LocalIosBackdrop provides pageBackdrop) {
        com.xyonmusic.music.ui.component.FrostedTopBar(
            title = title,
            titleAlphaProvider = { collapse.value },
            titleColor = colors.label,
            largeTitle = isTab,
            leadingInset = sideInset + titleGutter,
            leading = {
                if (!isTab) ToolbarCapsule(onClick = onBack, clearBackdrop = true) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 14.dp)) {
                        IosGlyph(sf("SFChevronLeft"), ToolbarGlyphColor(), 18.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(backLabel, style = SfStyle(17.sp, 22.sp, FontWeight.Medium), color = ToolbarGlyphColor(), maxLines = 1)
                    }
                }
            },
            actions = { Row(verticalAlignment = Alignment.CenterVertically) { trailing() } },
        )
        }
        overlay()
    }
}

private const val LargeTitleKey = "apple_large_title"

@Composable
private fun ToolbarGlyphColor(): Color = if (IosTheme.colors.isDark) Color(0xFFE6E6E6) else Color(0xFF404040)

/** Glass capsule for toolbar controls, with refraction and press feedback. */
@Composable
private fun ToolbarCapsule(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    clearBackdrop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = IosTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(44.dp)
            .iosGlass(
                shape = CircleShape,
                spec = if (clearBackdrop) {
                    com.xyonmusic.music.ui.ios.foundation.IosGlassSpec.Control.copy(blurRadius = 0.dp)
                } else {
                    com.xyonmusic.music.ui.ios.foundation.IosGlassSpec.Control
                },
                interactive = true,
                wash = if (clearBackdrop) colors.systemBackground.copy(alpha = 0.62f) else Color.Transparent,
            )
            .then(if (onClick != null) Modifier.clickable(interactionSource = null, indication = null, onClick = onClick) else Modifier),
    ) { content() }
}

/** One 36dp glyph button inside a toolbar capsule, anchoring its own dropdown. */
@Composable
private fun ToolbarButton(icon: SfSymbol, menu: (@Composable (dismiss: () -> Unit) -> Unit)? = null, onClick: (() -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (menu != null) Modifier.menuTrigger { open = true }
                else Modifier.clickable(interactionSource = null, indication = null) { onClick?.invoke() },
            ),
    ) {
        IosGlyph(icon, ToolbarGlyphColor(), 20.dp)
        if (menu != null) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) { menu { open = false } }
        }
    }
}

@Composable
private fun ToolbarButtons(vararg buttons: @Composable () -> Unit) {
    ToolbarCapsule {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            buttons.forEach { it() }
        }
    }
}

private val SortGlyph: SfSymbol by lazy { sf("line.3.horizontal.decrease") }

/** A dropdown row with Apple's leading checkmark column (picker menus). */
@Composable
private fun CheckRow(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            Box(Modifier.size(19.dp), contentAlignment = Alignment.Center) {
                if (checked) IosCheckmark(color = LocalContentColor.current)
            }
        },
        onClick = onClick,
    )
}

// Rows

/** Library row with artwork, title, optional subtitle, and optional trailing content. */
@Composable
private fun ArtRow(
    art: Any?,
    artSize: Dp,
    rowHeight: Dp,
    title: String,
    subtitle: String?,
    circle: Boolean = false,
    radius: Dp = 5.dp,
    chevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    artworkId: String? = null,
) {
    val colors = IosTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .then(if (onClick != null) Modifier.iosRowPress(onClick = onClick) else Modifier)
            .padding(start = 16.dp),
    ) {
        val shape = if (circle) CircleShape else com.xyonmusic.music.ui.component.shapes.ContinuousRoundedRectangle(radius)
        AsyncImage(
            model = art,
            onSuccess = com.xyonmusic.music.ui.utils.sharedArtworkImageListener(
                com.xyonmusic.music.ui.utils.rememberArtworkSourceOwner(), art as? String,
            ),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .sharedArtworkSource(artworkId, art as? String)
                .size(artSize)
                .clip(shape)
                .background(colors.secondarySystemFill),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart).padding(end = 16.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = SfStyle(17.sp, 22.sp, tracking = (-0.43).sp), color = colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(subtitle, style = SfStyle(15.sp, 20.sp, tracking = (-0.23).sp), color = colors.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                trailing?.invoke()
                if (chevron) {
                    Spacer(Modifier.width(8.dp))
                    Chevron()
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(IosMetrics.SeparatorThickness)
                    .background(colors.separator),
            )
        }
    }
}

// Playlists

@Composable
fun ApplePlaylistsPage(navController: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModelOwner: ViewModelStoreOwner = remember(context) {
        context.findActivity() as? ViewModelStoreOwner
    } ?: LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner")
    val viewModel: LibraryPlaylistsViewModel = hiltViewModel(viewModelStoreOwner = viewModelOwner)
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val localOnly = com.xyonmusic.music.LocalLocalOnlyMode.current
    val playlists = allPlaylists.filter { !localOnly || it.playlist.browseId == null }
    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.LIST)
    var sortType by rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    var descending by rememberPreference(PlaylistSortDescendingKey, true)
    var newPlaylist by remember { mutableStateOf(false) }
    val colors = IosTheme.colors
    val liked = stringResource(R.string.liked_songs)
    // The Playlists tab is also the door to the Spotify account: its playlists, its
    // recommendations, and the same import flow as the Spotify settings page.
    val spotifyViewModel: SpotifyImportViewModel = hiltViewModel(viewModelStoreOwner = viewModelOwner)
    val spotifyState by spotifyViewModel.uiState.collectAsStateWithLifecycle()
    val spotifyImport by spotifyViewModel.importProgress.collectAsStateWithLifecycle()
    var showSpotifyImport by remember { mutableStateOf(false) }
    var openedSpotifyPlaylist by remember { mutableStateOf<SpotifyPlaylist?>(null) }
    LaunchedEffect(spotifyViewModel) { spotifyViewModel.refreshInBackground() }

    AppleSubpage(
        title = "Playlist",
        onBack = onBack,
        trailing = {
            ToolbarButtons(
                { ToolbarButton(sf("SFPlus"), onClick = { newPlaylist = true }) },
                {
                    ToolbarButton(SortGlyph, menu = { dismiss ->
                        CheckRow(stringResource(R.string.apple_view_list), viewType == LibraryViewType.LIST) { viewType = LibraryViewType.LIST; dismiss() }
                        CheckRow(stringResource(R.string.apple_view_grid), viewType == LibraryViewType.GRID) { viewType = LibraryViewType.GRID; dismiss() }
                        IosMenuDivider()
                        CheckRow(stringResource(R.string.apple_sort_title), sortType == PlaylistSortType.NAME) { sortType = PlaylistSortType.NAME; descending = false; dismiss() }
                        CheckRow(stringResource(R.string.recently_added), sortType == PlaylistSortType.CREATE_DATE) { sortType = PlaylistSortType.CREATE_DATE; descending = true; dismiss() }
                        CheckRow(stringResource(R.string.apple_sort_recently_updated), sortType == PlaylistSortType.LAST_UPDATED) { sortType = PlaylistSortType.LAST_UPDATED; descending = true; dismiss() }
                        CheckRow(stringResource(R.string.apple_sort_song_count), sortType == PlaylistSortType.SONG_COUNT) { sortType = PlaylistSortType.SONG_COUNT; descending = true; dismiss() }
                    })
                },
            )
        },
    ) {
        item(key = "spotify_section", contentType = "spotify") {
            SpotifySection(
                isAuthenticated = spotifyState.isAuthenticated,
                isLoading = spotifyState.isLoading,
                isHomeLoading = spotifyState.isHomeFeedLoading,
                accountName = spotifyState.accountName,
                playlists = spotifyState.playlists,
                shelves = spotifyState.homeShelves,
                importProgress = spotifyImport,
                onConnect = { navController.navigate("settings/spotify") },
                onImportAll = { showSpotifyImport = true },
                onOpenPlaylist = { openedSpotifyPlaylist = it },
                onImportHome = spotifyViewModel::importHomePlaylist,
            )
        }
        if (playlists.isEmpty() && !spotifyState.isAuthenticated && !spotifyState.isLoading) {
            item(key = "empty_playlists", contentType = "placeholder") {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IosGlyph(sf("SFMusicNoteList"), colors.secondaryLabel, 44.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.no_playlists), color = colors.label, style = SfStyle(22.sp, 28.sp, FontWeight.Bold))
                    com.xyonmusic.music.ui.ios.compat.TextButton(onClick = { newPlaylist = true }) {
                        Text(stringResource(R.string.create_playlist), color = IosTheme.tint)
                    }
                }
            }
        }
        if (playlists.isNotEmpty() && spotifyState.isAuthenticated) {
            item(key = "library_playlists_title", contentType = "title") {
                Text(
                    "En tu biblioteca",
                    color = colors.secondaryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                )
            }
        }
        if (viewType == LibraryViewType.LIST) {
            items(playlists, key = { it.id }, contentType = { "row" }) { playlist ->
                Box(Modifier.animateItem()) { LibraryItemMenu(playlist, onOpen = { navController.navigate(localPlaylistRoute(playlist.id, playlist.thumbnails.firstOrNull())) }) {
                    ArtRow(
                        art = playlist.thumbnails.firstOrNull(),
                        artworkId = playlist.id,
                        artSize = 60.dp,
                        rowHeight = 76.dp,
                        title = playlist.title,
                        subtitle = pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount),
                        onClick = { navController.navigate(localPlaylistRoute(playlist.id, playlist.thumbnails.firstOrNull())) },
                    )
                } }
            }
        } else {
            gridItems(playlists) { item ->
                LibraryItemMenu(item, onOpen = { navController.navigate(localPlaylistRoute(item.id, (item as com.xyonmusic.music.db.entities.Playlist).thumbnails.firstOrNull())) }) {
                    RecentTile(item)
                }
            }
        }
    }

    if (newPlaylist) CreatePlaylistDialog(onDismiss = { newPlaylist = false })
    if (showSpotifyImport) {
        SpotifyPlaylistBottomSheet(
            onDismiss = { showSpotifyImport = false },
            viewModel = spotifyViewModel,
        )
    }
    openedSpotifyPlaylist?.let { playlist ->
        SpotifyPlaylistDetailSheet(
            playlist = playlist,
            viewModel = spotifyViewModel,
            onDismiss = { openedSpotifyPlaylist = null },
        )
    }
}

private val SpotifyGreen = Color(0xFF1ED760)

/**
 * Spotify block at the top of Playlists: account, the user's playlists, and the home
 * recommendations, all with the same import action as the Spotify settings page.
 */
@Composable
private fun SpotifySection(
    isAuthenticated: Boolean,
    isLoading: Boolean,
    isHomeLoading: Boolean,
    accountName: String,
    playlists: List<SpotifyPlaylist>,
    shelves: List<SpotifyHomeShelf>,
    importProgress: SpotifyImportProgress?,
    onConnect: () -> Unit,
    onImportAll: () -> Unit,
    onOpenPlaylist: (SpotifyPlaylist) -> Unit,
    onImportHome: (SpotifyHomeItem) -> Unit,
) {
    val colors = IosTheme.colors
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.spotify),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Spotify", color = colors.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isAuthenticated) accountName.ifBlank { "Conectado" } else "Conecta tu cuenta",
                    color = colors.secondaryLabel,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isAuthenticated) {
                Text(
                    "Importar",
                    color = SpotifyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onImportAll)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }

        if (!isAuthenticated) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.secondarySystemFill)
                    .iosRowPress(onClick = onConnect)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.spotify),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isLoading) "Conectando con Spotify…" else "Conecta Spotify",
                        color = colors.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Tus playlists y recomendaciones aparecerán aquí.",
                        color = colors.secondaryLabel,
                        fontSize = 12.sp,
                    )
                }
                IosGlyph(sf("chevron.right"), colors.tertiaryLabel, 16.dp)
            }
            return@Column
        }

        if (isLoading && playlists.isEmpty() && shelves.isEmpty()) {
            Text(
                "Cargando tus playlists de Spotify…",
                color = colors.secondaryLabel,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (playlists.isNotEmpty()) {
            SpotifyShelfRow("Tus playlists", playlists, onOpenPlaylist)
        } else if (!isLoading) {
            Text(
                "No hay playlists en tu cuenta de Spotify.",
                color = colors.secondaryLabel,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }

        shelves.forEach { shelf -> SpotifyHomeShelfRow(shelf, onOpenPlaylist, onImportHome) }

        if (isHomeLoading && shelves.isEmpty()) {
            Text(
                "Cargando recomendaciones de Spotify…",
                color = colors.secondaryLabel,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        importProgress?.takeIf { !it.isFinished }?.let { progress ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text(
                    "Importando ${progress.playlistName}… ${(progress.percent * 100).toInt()} %",
                    color = colors.label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.secondarySystemFill)) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.percent.coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(SpotifyGreen),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyShelfRow(
    title: String,
    playlists: List<SpotifyPlaylist>,
    onOpen: (SpotifyPlaylist) -> Unit,
) {
    val colors = IosTheme.colors
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            title,
            color = colors.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(playlists, key = { "sp_" + it.id }) { playlist ->
                SpotifyCard(
                    art = SpotifyMapper.getPlaylistThumbnail(playlist),
                    title = playlist.name,
                    subtitle = listOfNotNull(
                        playlist.owner?.displayName,
                        playlist.tracks?.total?.let { "$it canciones" },
                    ).joinToString(" · "),
                    onClick = { onOpen(playlist) },
                )
            }
        }
    }
}

@Composable
private fun SpotifyHomeShelfRow(
    shelf: SpotifyHomeShelf,
    onOpenPlaylist: (SpotifyPlaylist) -> Unit,
    onImport: (SpotifyHomeItem) -> Unit,
) {
    val colors = IosTheme.colors
    Column(Modifier.padding(top = 4.dp)) {
        Text(
            shelf.title,
            color = colors.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shelf.items, key = { "sp_home_" + it.uri }) { item ->
                SpotifyCard(
                    art = item.artworkUrl,
                    title = item.title,
                    subtitle = "Spotify",
                    onClick = {
                        val playlistId = item.playlistId
                        if (item.type == SpotifyHomeItemType.PLAYLIST && playlistId != null) {
                            onOpenPlaylist(
                                SpotifyPlaylist(
                                    id = playlistId,
                                    name = item.title,
                                    images = listOfNotNull(
                                        item.artworkUrl?.takeIf { it.isNotBlank() }?.let { SpotifyImage(url = it) },
                                    ),
                                ),
                            )
                        } else {
                            onImport(item)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SpotifyCard(
    art: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = IosTheme.colors
    Column(Modifier.width(132.dp).iosRowPress(onClick = onClick)) {
        Box(
            Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.secondarySystemFill),
        ) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = colors.label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = colors.secondaryLabel,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Places library items in two-column rows. */
private fun LazyListScope.gridItems(list: List<LocalItem>, tile: @Composable (LocalItem) -> Unit) {
    items(list.chunked(2), key = { "grid_" + it.first().id }, contentType = { "grid_row" }) { pair ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.animateItem().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        ) {
            pair.forEach { Box(Modifier.weight(1f)) { tile(it) } }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

// Artists

@Composable
fun AppleArtistsPage(navController: NavController, onBack: () -> Unit, viewModel: AppleLibraryViewModel = hiltViewModel()) {
    val artists by viewModel.localArtists.collectAsStateWithLifecycle()
    val localOnly by viewModel.localOnly.collectAsStateWithLifecycle()
    val permission = com.xyonmusic.music.ui.component.rememberLocalAudioPermission { }
    val needsAccess = localOnly && !permission.granted
    androidx.compose.runtime.LaunchedEffect(permission.granted) { if (permission.granted) viewModel.scanDevice() }
    var sortType by rememberEnumPreference(ArtistSortTypeKey, ArtistSortType.NAME)
    var descending by rememberPreference(ArtistSortDescendingKey, false)

    AppleSubpage(
        title = stringResource(R.string.filter_artists),
        onBack = onBack,
        trailing = {
            ToolbarCapsule {
                Box(Modifier.padding(horizontal = 4.dp)) {
                    ToolbarButton(SortGlyph, menu = { dismiss ->
                        CheckRow(stringResource(R.string.apple_sort_title), sortType == ArtistSortType.NAME) { sortType = ArtistSortType.NAME; descending = false; dismiss() }
                        CheckRow(stringResource(R.string.recently_added), sortType == ArtistSortType.CREATE_DATE) { sortType = ArtistSortType.CREATE_DATE; descending = true; dismiss() }
                        CheckRow(stringResource(R.string.apple_sort_song_count), sortType == ArtistSortType.SONG_COUNT) { sortType = ArtistSortType.SONG_COUNT; descending = true; dismiss() }
                    })
                }
            }
        },
    ) {
        if (needsAccess || artists.isEmpty()) {
            item(key = "artist_access", contentType = "placeholder") {
                if (needsAccess) LocalAccessCard(permission.blocked, permission.request)
                else com.xyonmusic.music.ui.component.LocalMusicEmptyState(onScan = viewModel::scanDevice)
            }
            return@AppleSubpage
        }
        items(artists, key = { it.id }, contentType = { "row" }) { artist: Artist ->
            Box(Modifier.animateItem()) { ArtRow(
                art = artist.artist.thumbnailUrl,
                artSize = 48.dp,
                rowHeight = 56.dp,
                title = artist.artist.name,
                subtitle = null,
                circle = true,
                onClick = { navController.navigate(artistRoute(artist.id, artist.artist.thumbnailUrl)) },
            ) }
        }
    }
}

// Albums

@Composable
fun AppleAlbumsPage(navController: NavController, onBack: () -> Unit, viewModel: AppleLibraryViewModel = hiltViewModel()) {
    val albums by viewModel.localAlbums.collectAsStateWithLifecycle()
    val localOnly by viewModel.localOnly.collectAsStateWithLifecycle()
    val permission = com.xyonmusic.music.ui.component.rememberLocalAudioPermission { }
    val needsAccess = localOnly && !permission.granted
    androidx.compose.runtime.LaunchedEffect(permission.granted) { if (permission.granted) viewModel.scanDevice() }
    var sortType by rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.NAME)
    var descending by rememberPreference(AlbumSortDescendingKey, false)

    AppleSubpage(
        title = stringResource(R.string.filter_albums),
        onBack = onBack,
        trailing = {
            ToolbarCapsule {
                Box(Modifier.padding(horizontal = 4.dp)) {
                    ToolbarButton(SortGlyph, menu = { dismiss ->
                        CheckRow(stringResource(R.string.apple_sort_title), sortType == AlbumSortType.NAME) { sortType = AlbumSortType.NAME; descending = false; dismiss() }
                        CheckRow(stringResource(R.string.recently_added), sortType == AlbumSortType.CREATE_DATE) { sortType = AlbumSortType.CREATE_DATE; descending = true; dismiss() }
                        CheckRow(stringResource(R.string.artist), sortType == AlbumSortType.ARTIST) { sortType = AlbumSortType.ARTIST; descending = false; dismiss() }
                        CheckRow(stringResource(R.string.sort_by_year), sortType == AlbumSortType.YEAR) { sortType = AlbumSortType.YEAR; descending = true; dismiss() }
                    })
                }
            }
        },
    ) {
        if (needsAccess || albums.isEmpty()) {
            item(key = "album_access", contentType = "placeholder") {
                if (needsAccess) LocalAccessCard(permission.blocked, permission.request)
                else com.xyonmusic.music.ui.component.LocalMusicEmptyState(onScan = viewModel::scanDevice)
            }
            return@AppleSubpage
        }
        gridItems(albums) { item ->
            LibraryItemMenu(item, onOpen = { navController.navigate(albumRoute(item.id, item.thumbnailUrl)) }) {
                RecentTile(item)
            }
        }
    }
}

// Standard and local songs share the same list screen.

@Composable
fun AppleSongsPage(navController: NavController, onBack: () -> Unit, viewModel: AppleLibraryViewModel = hiltViewModel()) {
    val permission = com.xyonmusic.music.ui.component.rememberLocalAudioPermission { }
    val songs by viewModel.localSongs.collectAsStateWithLifecycle()
    val localOnly by viewModel.localOnly.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(permission.granted) { if (permission.granted) viewModel.scanDevice() }
    var sortType by rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    var descending by rememberPreference(SongSortDescendingKey, true)
    // Earlier downloads predate "downloads join the library": bring them in once.
    val db = LocalDatabase.current.get()
    androidx.compose.runtime.LaunchedEffect(Unit) { db.query { addDownloadedToLibrary(System.currentTimeMillis()) } }
    AppleSongList(
        title = stringResource(R.string.filter_songs),
        songs = songs,
        sortType = sortType,
        onSort = { type, desc -> sortType = type; descending = desc },
        navController = navController,
        onBack = onBack,
        placeholder = when {
            localOnly && !permission.granted -> ({ LocalAccessCard(permission.blocked, permission.request) })
            songs.isEmpty() -> ({ com.xyonmusic.music.ui.component.LocalMusicEmptyState(onScan = viewModel::scanDevice) })
            else -> null
        },
    )
}

/** Songs saved on this phone: the same list, behind a permission prompt when needed. */
@Composable
fun AppleLocalSongsPage(navController: NavController, onBack: () -> Unit, viewModel: AppleLocalSongsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val scanning by viewModel.scanning.collectAsStateWithLifecycle()
    var granted by remember { mutableStateOf(com.xyonmusic.music.ui.component.hasLocalAudioPermission(context)) }
    val permission = com.xyonmusic.music.ui.component.rememberLocalAudioPermission { ok ->
        granted = ok
        if (ok) viewModel.scan(context)
    }
    // Picks up new files every time the page opens with access.
    androidx.compose.runtime.LaunchedEffect(granted) { if (granted) viewModel.scan(context) }
    var sortType by remember { mutableStateOf(SongSortType.NAME) }

    AppleSongList(
        title = stringResource(R.string.filter_local),
        songs = songs,
        sortType = sortType,
        onSort = { type, desc -> sortType = type; viewModel.sort(type, desc) },
        navController = navController,
        onBack = onBack,
        header = if (granted) ({
            com.xyonmusic.music.ui.screens.library.LocalMusicFoldersEntry(count = null, navController = navController)
        }) else null,
        placeholder = when {
            !granted -> ({ LocalAccessCard(blocked = permission.blocked, onAllow = permission.request) })
            songs.isEmpty() && !scanning -> ({ com.xyonmusic.music.ui.component.LocalMusicEmptyState(onScan = { viewModel.scan(context) }) })
            else -> null
        },
    )
}

/**
 * Apple's "allow access" card: a tinted glyph in a soft circle, a title, why it is
 * asked for, and one capsule button — "Allow Access", or "Open Settings" once Android
 * has stopped showing the system dialog.
 */
@Composable
internal fun LocalAccessCard(blocked: Boolean, onAllow: () -> Unit) {
    val colors = IosTheme.colors
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(IosTheme.tint.copy(alpha = 0.14f)),
        ) { IosGlyph(sf("SFMusicNoteHouse"), IosTheme.tint, 36.dp) }
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.apple_local_access_title),
            style = SfStyle(22.sp, 28.sp, FontWeight.Bold, (-0.26).sp).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            color = colors.label,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (blocked) R.string.local_permission_blocked_body else R.string.apple_local_access_body),
            style = SfStyle(15.sp, 20.sp, tracking = (-0.23).sp).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
            color = colors.secondaryLabel,
        )
        Spacer(Modifier.height(22.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(50.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(IosTheme.tint)
                .iosRowPress(onClick = {
                    if (blocked) {
                        context.startActivity(
                            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        onAllow()
                    }
                }),
        ) {
            Text(
                stringResource(if (blocked) R.string.open_settings else R.string.apple_local_allow),
                style = SfStyle(17.sp, 22.sp, FontWeight.SemiBold, (-0.43).sp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(
        text,
        style = SfStyle(15.sp, 20.sp).copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        color = IosTheme.colors.secondaryLabel,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
    )
}

/**
 * The shared Songs list: Play / Shuffle, then rows — under letter headers when sorted
 * by title — with the app's A–Z scrub rail on the trailing edge. [placeholder], when
 * set, stands in for the buttons and rows (no access yet, nothing found).
 */
@Composable
private fun AppleSongList(
    title: String,
    songs: List<Song>,
    sortType: SongSortType,
    onSort: (SongSortType, Boolean) -> Unit,
    navController: NavController,
    onBack: () -> Unit,
    placeholder: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current
    val state = rememberLazyListState()
    val allSongs = songs
    val isTab = LocalSubpageIsTab.current
    // Live filter from the glass search field, collapsed behind the round magnifier until needed.
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    @Suppress("NAME_SHADOWING")
    val songs = remember(allSongs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allSongs
        else allSongs.filter { s -> s.song.title.lowercase().contains(q) || s.artists.any { it.name.lowercase().contains(q) } }
    }
    // All albums of this list for the full CoverFlow, plus two random covers for the preview.
    val coverAlbums = remember(allSongs) { songsCoverAlbums(allSongs) }
    val previewAlbums = remember(coverAlbums) { coverAlbums.shuffled().take(2) }
    val missingDownloads = rememberMissingDownloads(allSongs)
    val onlineCount = remember(allSongs) { allSongs.count { !it.song.isLocal } }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var previousOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }
    var coverFlowOpen by remember { mutableStateOf(false) }
    val closeCoverFlow = {
        coverFlowOpen = false
        SongsCoverFlowState.open = false
        activity?.requestedOrientation = previousOrientation
    }
    val openCoverFlow = {
        if (isTab && coverAlbums.isNotEmpty()) {
            previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            coverFlowOpen = true
            // The root hides the chrome and the activity turns the phone: the flow owns the screen.
            SongsCoverFlowState.open = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            if (coverFlowOpen) {
                SongsCoverFlowState.open = false
                activity?.requestedOrientation = previousOrientation
            }
        }
    }
    if (isTab) {
        BackHandler(enabled = coverFlowOpen) { closeCoverFlow() }
    }

    // Landscape CoverFlow with the albums of this list, exactly like the original XyonMusic.
    if (isTab && coverFlowOpen) {
        val colors = IosTheme.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.systemBackground)
                .systemBarsPadding(),
        ) {
            XyonCoverFlow(
                entries = coverAlbums.map {
                    CoverFlowEntry("a:" + it.albumId, it.title, it.artist, it.art)
                },
                onOpen = { index ->
                    val album = coverAlbums[index]
                    closeCoverFlow()
                    navController.navigate(albumRoute(album.albumId, album.art))
                },
                fullScreen = true,
            )
            CoverFlowCloseButton(
                modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp),
                onClick = closeCoverFlow,
            )
        }
        return
    }

    // By title, Apple groups under letter headers.
    val sections = remember(songs, sortType) {
        if (sortType != SongSortType.NAME) null
        else songs.groupBy { sectionLetter(it.song.title) }.toList()
    }
    // Rail map: letter -> index among the rail's items (headers + rows), which are the
    // last items of the list, as ListScrollRail expects.
    val railIndex = remember(sections, songs) {
        if (sections == null) com.xyonmusic.music.ui.component.buildAlphabetSectionIndex(songs) { it.song.title }
        else {
            var i = 0
            sections.associate { (letter, list) -> letter to i.also { i += 1 + list.size } }
        }
    }
    val railCount = if (sections == null) songs.size else songs.size + sections.size

    fun play(start: Int, shuffle: Boolean = false) {
        val items = songs.map(Song::toMediaItem)
        if (items.isEmpty()) return
        playerConnection?.playQueue(
            if (shuffle) ListQueue(title = title, items = items.shuffled()) else ListQueue(title = title, items = items, startIndex = start),
        )
    }

    AppleSubpage(
        title = title,
        onBack = onBack,
        state = state,
        trailing = {
            ToolbarCapsule {
                Row(
                    Modifier.padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // The magnifier sits with the filters, in the top bar.
                    ToolbarButton(
                        icon = sf(if (searchOpen) "xmark" else "magnifyingglass"),
                        onClick = {
                            searchOpen = !searchOpen
                            if (!searchOpen) query = ""
                        },
                    )
                    ToolbarButton(SortGlyph, menu = { dismiss ->
                        CheckRow(stringResource(R.string.apple_sort_title), sortType == SongSortType.NAME) { onSort(SongSortType.NAME, false); dismiss() }
                        CheckRow(stringResource(R.string.recently_added), sortType == SongSortType.CREATE_DATE) { onSort(SongSortType.CREATE_DATE, true); dismiss() }
                        CheckRow(stringResource(R.string.artist), sortType == SongSortType.ARTIST) { onSort(SongSortType.ARTIST, false); dismiss() }
                    })
                }
            }
        },
        overlay = {
            if (placeholder == null) {
                ListScrollRail(
                    lazyListState = state,
                    itemCount = railCount,
                    sectionIndexMap = railIndex,
                )
            }
        },
    ) {
        if (placeholder != null) {
            item(key = "placeholder", contentType = "placeholder") { placeholder() }
            return@AppleSubpage
        }
        if (header != null) item(key = "folders", contentType = "folders") { header() }
        if (isTab && coverAlbums.isNotEmpty()) {
            item(key = "albums_coverflow", contentType = "coverflow") {
                AlbumsCoverFlowHeader(
                    albums = previewAlbums,
                    onOpenAlbum = { album -> navController.navigate(albumRoute(album.albumId, album.art)) },
                    onOpenFlow = openCoverFlow,
                )
            }
        }
        if (searchOpen) {
            item(key = "search", contentType = "search") {
                GlassSearchField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
        // Only when there are missing downloads. When there are none, the row disappears
        // completely and the first song follows the coverflow without a black gap.
        if (missingDownloads.isNotEmpty()) {
            item(key = "downloadMissing", contentType = "download") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    DownloadMissingButton(missingDownloads, onlineCount)
                }
            }
        }
        if (sections != null) {
            var offset = 0
            sections.forEach { (letter, list) ->
                val base = offset
                item(key = "h_$letter", contentType = "header") { Box(Modifier.animateItem()) { LetterHeader(letter) } }
                itemsIndexed(list, key = { _, s -> s.id }, contentType = { _, _ -> "song" }) { i, song ->
                    Box(Modifier.animateItem()) { SongRow(song, navController) { play(base + i) } }
                }
                offset += list.size
            }
        } else {
            itemsIndexed(songs, key = { _, s -> s.id }, contentType = { _, _ -> "song" }) { i, song ->
                Box(Modifier.animateItem()) { SongRow(song, navController) { play(i) } }
            }
        }
    }
}

private data class SongsCoverAlbum(
    val albumId: String,
    val title: String,
    val artist: String,
    val art: String?,
)

/** Unique albums of the current Songs list, keeping the list's order (first occurrence wins). */
private fun songsCoverAlbums(songs: List<Song>): List<SongsCoverAlbum> {
    val seen = LinkedHashMap<String, SongsCoverAlbum>()
    songs.forEach { song ->
        val albumId = song.album?.id ?: song.song.albumId ?: return@forEach
        if (seen.containsKey(albumId)) return@forEach
        val title = song.album?.title ?: song.song.albumName ?: return@forEach
        seen[albumId] = SongsCoverAlbum(
            albumId = albumId,
            title = title,
            artist = song.artists.joinToString { it.name },
            art = song.album?.thumbnailUrl ?: song.song.thumbnailUrl,
        )
    }
    return seen.values.toList()
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * The row from the sketch: the first two album covers of this list, then the liquid-glass
 * "Albums CoverFlow" button. The trailing gutter keeps the A–Z rail clear of the crystal.
 */
@Composable
private fun AlbumsCoverFlowHeader(
    albums: List<SongsCoverAlbum>,
    onOpenAlbum: (SongsCoverAlbum) -> Unit,
    onOpenFlow: () -> Unit,
) {
    val backdrop = rememberGlassBackdrop()
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 34.dp, top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        albums.take(2).forEach { album ->
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(IosTheme.colors.secondarySystemFill)
                    .border(0.5.dp, IosTheme.colors.label.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                    .iosRowPress(onClick = { onOpenAlbum(album) }),
            ) {
                AsyncImage(
                    model = album.art,
                    contentDescription = album.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Box(Modifier.weight(1f).height(72.dp)) {
            Box(
                Modifier
                    .matchParentSize()
                    .glassSource(backdrop)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF2A1016), Color(0xFF141113))),
                        shape,
                    )
                    .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape),
            )
            LiquidGlassButton(
                onClick = onOpenFlow,
                backdrop = backdrop,
                modifier = Modifier.matchParentSize(),
                shape = shape,
                glass = LiquidGlassStyle.Dark.copy(blur = 10.dp, refractionHeight = 24.dp, refractionAmount = 24.dp),
                tint = Color(0xFFFA2D48).copy(alpha = 0.20f),
                onClickLabel = "Albums CoverFlow",
            ) {
                Text(
                    text = "Albums CoverFlow",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Glass way out of the full-screen CoverFlow, always reachable with the left thumb. */
@Composable
private fun CoverFlowCloseButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backdrop = rememberGlassBackdrop()
    Box(modifier.height(40.dp).width(104.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .glassSource(backdrop)
                .background(Color.Black.copy(alpha = 0.30f), CircleShape),
        )
        LiquidGlassButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = Modifier.matchParentSize(),
            shape = CircleShape,
            tint = Color.White.copy(alpha = 0.06f),
            onClickLabel = "Cerrar Albums CoverFlow",
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IosGlyph(sf("SFChevronLeft"), Color.White, 16.dp)
                Spacer(Modifier.width(4.dp))
                Text("Cerrar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun sectionLetter(title: String): String {
    val c = title.trim().firstOrNull()?.uppercaseChar() ?: return "#"
    return if (c in 'A'..'Z') c.toString() else "#"
}

@Composable
private fun PlayButton(icon: SfSymbol, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(50.dp)
            .clip(CircleShape)
            .background(Color(0xFF767680).copy(alpha = 0.12f))
            .iosRowPress(onClick = onClick),
    ) {
        IosGlyph(icon, IosTheme.tint, 17.dp)
        Spacer(Modifier.width(4.dp))
        Text(label, style = SfStyle(17.sp, 22.sp, tracking = (-0.43).sp), color = IosTheme.tint)
    }
}

@Composable
private fun LetterHeader(letter: String) {
    val colors = IosTheme.colors
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp)) {
        Text(letter, style = SfStyle(15.sp, 20.sp, FontWeight.SemiBold), color = colors.secondaryLabel)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(IosMetrics.SeparatorThickness).background(colors.separator))
    }
}

@Composable
private fun SongRow(song: Song, navController: NavController, onPlay: () -> Unit) {
    val entries = songMenu(song, navController)
    var dots by remember { mutableStateOf(false) }
    com.xyonmusic.music.ui.utils.ArtworkSourceHost(Modifier) { sourceModifier ->
    AppleContextMenu(
        sections = entries.sections,
        quickActions = entries.quick,
        preview = {
            Cover(song.song.thumbnailUrl, 12.dp)
            Text(song.song.title, style = SfStyle(15.sp, 20.sp, FontWeight.SemiBold, (-0.23).sp), color = IosTheme.colors.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp, start = 2.dp))
            Text(song.artists.joinToString { it.name }, style = SfStyle(13.sp, 18.sp), color = IosTheme.colors.secondaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        },
        onClick = onPlay,
        modifier = sourceModifier,
    ) {
        ArtRow(
            art = song.song.thumbnailUrl,
            artworkId = song.album?.id ?: song.song.albumId,
            artSize = 48.dp,
            rowHeight = 64.dp,
            title = song.song.title,
            subtitle = song.artists.joinToString { it.name },
            chevron = false,
            trailing = {
                // Every song shows where it stands: downloaded, downloading (ring), failed, or a
                // faint arrow that downloads it on tap.
                SongDownloadState(song)
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .menuTrigger { dots = true },
                    ) {
                        // Plain ink: the chromed (dodge) glyph disappears on a pure black list.
                        com.xyonmusic.music.ui.component.SymbolIcon(sf("ellipsis"), contentDescription = "Más", size = 20.dp, tint = IosTheme.colors.label, chrome = 0f)
                    }
                    DropdownMenu(expanded = dots, onDismissRequest = { dots = false }) {
                        (entries.quick + entries.sections.flatMap { it.items }).forEachIndexed { i, item ->
                            if (i == entries.quick.size && i > 0) IosMenuDivider()
                            DropdownMenuItem(
                                text = { Text(item.title) },
                                leadingIcon = { item.icon?.invoke(LocalContentColor.current) },
                                onClick = { dots = false; item.onClick() },
                            )
                        }
                    }
                }
                // Keep the three dots left of the A–Z rail instead of under its letters.
                Spacer(Modifier.width(18.dp))
            },
        )
    }
    }
    if (entries.addToPlaylist.value) {
        AddToPlaylistDialog(isVisible = true, onGetSong = { listOf(song.id) }, onDismiss = { entries.addToPlaylist.value = false })
    }
}

/** Frosted glass search capsule: translucent fill, light rim, search glyph and an inline field. */
@Composable
private fun GlassSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = IosTheme.colors
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.label.copy(alpha = 0.06f))
            .border(1.dp, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colors.label.copy(alpha = 0.14f), colors.label.copy(alpha = 0.04f))), shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IosGlyph(sf("magnifyingglass"), colors.secondaryLabel, 18.dp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Buscar en canciones", style = SfStyle(16.sp, 20.sp), color = colors.secondaryLabel)
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SfStyle(16.sp, 20.sp).copy(color = colors.label),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(IosTheme.tint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Box(Modifier.size(28.dp).clip(CircleShape).clickable { onValueChange("") }, contentAlignment = Alignment.Center) {
                IosGlyph(sf("xmark.circle.fill"), colors.secondaryLabel, 16.dp)
            }
        }
    }
}

/**
 * Download action for the whole list: "Descargar todas" when nothing is downloaded, "Descargar las N que faltan"
 * when only some are, and nothing at all once every song is on the phone.
 */
/** The online songs in this list that are not downloaded, downloading or queued. */
@Composable
private fun rememberMissingDownloads(songs: List<Song>): List<Song> {
    val downloadUtil = LocalDownloadUtil.current.get()
    val downloads by downloadUtil.downloads.collectAsStateWithLifecycle()
    return remember(songs, downloads) {
        songs.filterNot { it.song.isLocal }.filter { s ->
            !s.song.isDownloaded &&
                downloads[s.id]?.state != androidx.media3.exoplayer.offline.Download.STATE_COMPLETED &&
                downloads[s.id]?.state != androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING &&
                downloads[s.id]?.state != androidx.media3.exoplayer.offline.Download.STATE_QUEUED
        }
    }
}

@Composable
private fun DownloadMissingButton(
    missing: List<Song>,
    onlineCount: Int,
    modifier: Modifier = Modifier,
) {
    if (missing.isEmpty()) return
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current.get()
    val label = when {
        missing.size == onlineCount -> "Descargar todas"
        missing.size == 1 -> "Descargar la que falta"
        else -> "Descargar las ${missing.size} que faltan"
    }
    Row(
        modifier
            .clip(CircleShape)
            .background(IosTheme.tint.copy(alpha = 0.12f))
            .clickable {
                downloadSongs(context, missing.map { DownloadTarget(it.id, it.song.title, it.artists.firstOrNull()?.name) }, downloadUtil.downloads.value)
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IosGlyph(sf("SFArrowDownToLine"), IosTheme.tint, 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = SfStyle(15.sp, 20.sp, FontWeight.SemiBold), color = IosTheme.tint)
    }
}

internal class SongMenuEntries(
    val quick: List<IosMenuItem>,
    val sections: List<IosMenuSection>,
    val addToPlaylist: androidx.compose.runtime.MutableState<Boolean>,
)

/** Apple Music's song menu: Download · Favorite · Share, then queue, playlist and navigation. */
@Composable
internal fun songMenu(song: Song, navController: NavController): SongMenuEntries {
    val context = LocalContext.current
    val database = LocalDatabase.current.get()
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current.get()
    val addToPlaylist = remember { mutableStateOf(false) }
    fun glyph(v: SfSymbol): @Composable (Color) -> Unit = { tint -> IosGlyph(v, tint, 20.dp) }

    // A file on the device has no YouTube video to download or link to.
    val isLocal = song.song.isLocal
    val songDownload by remember(song.id) { downloadUtil.getDownload(song.id) }.collectAsStateWithLifecycle(initialValue = null)
    val isOnDevice = songDownload?.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED || (songDownload == null && song.song.isDownloaded)
    val quick = listOfNotNull(
        // Download, or remove it when the song is already on the device.
        if (isLocal) null else if (isOnDevice) IosMenuItem(stringResource(R.string.remove_download), icon = glyph(sf("SFTrash"))) {
            androidx.media3.exoplayer.offline.DownloadService.sendRemoveDownload(context, com.xyonmusic.music.playback.ExoDownloadService::class.java, song.id, false)
        } else IosMenuItem(stringResource(R.string.action_download), icon = glyph(sf("SFArrowDownToLine"))) {
            downloadSongs(context, listOf(DownloadTarget(song.id, song.song.title)), downloadUtil.downloads.value)
        },
        IosMenuItem(
            stringResource(R.string.favorite),
            icon = glyph(if (song.song.liked) sf("SFStarFill") else sf("SFStar")),
        ) { database.query { update(song.song.toggleLike()) } },
        if (isLocal) null else IosMenuItem(stringResource(R.string.share), icon = glyph(sf("SFSquareAndArrowUp"))) {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, com.xyonmusic.music.utils.ShareUtils.songUrl(song.id)),
                    null,
                ),
            )
        },
    )
    val sections = listOf(
        IosMenuSection(
            listOf(
                IosMenuItem(stringResource(R.string.play_next), icon = glyph(sf("SFTextLineFirstAndArrowtriangleForward"))) {
                    playerConnection?.playNext(listOf(song.toMediaItem()))
                },
                IosMenuItem(stringResource(R.string.play_last), icon = glyph(sf("SFTextLineLastAndArrowtriangleForward"))) {
                    playerConnection?.addToQueue(listOf(song.toMediaItem()))
                },
            ),
        ),
        IosMenuSection(
            listOf(
                IosMenuItem(stringResource(R.string.add_to_playlist_ellipsis), icon = glyph(sf("SFTextBadgePlus"))) { addToPlaylist.value = true },
            ),
        ),
        IosMenuSection(
            listOfNotNull(
                song.song.albumId?.let { albumId ->
                    IosMenuItem(stringResource(R.string.apple_go_to_album), icon = glyph(sf("SFSquareStack"))) {
                        navController.navigate(albumRoute(albumId, song.song.thumbnailUrl))
                    }
                },
                song.artists.firstOrNull()?.let { artist ->
                    IosMenuItem(stringResource(R.string.apple_go_to_artist), icon = glyph(sf("microphone.dynamic.on.stand"))) {
                        navController.navigate(artistRoute(artist.id, artist.thumbnailUrl))
                    }
                },
            ),
        ),
    ).filter { it.items.isNotEmpty() }
    return SongMenuEntries(quick, sections, addToPlaylist)
}


/** Download state of one song in the list, with a tap to download (or retry) when it is not on the device. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun SongDownloadState(song: Song) {
    val context = LocalContext.current
    val downloadUtil = LocalDownloadUtil.current.get()
    val download by remember(song.id) { downloadUtil.getDownload(song.id) }.collectAsStateWithLifecycle(initialValue = null)
    val state = download?.state
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val startDownload = {
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        com.xyonmusic.music.playback.downloadSongs(
            context,
            listOf(com.xyonmusic.music.playback.DownloadTarget(song.id, song.song.title, song.artists.firstOrNull()?.name)),
            downloadUtil.downloads.value,
        )
    }
    androidx.compose.animation.AnimatedContent(
        targetState = when {
            state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED || (download == null && song.song.isDownloaded) -> 2
            state == androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING ||
                state == androidx.media3.exoplayer.offline.Download.STATE_QUEUED ||
                state == androidx.media3.exoplayer.offline.Download.STATE_RESTARTING -> 1
            state == androidx.media3.exoplayer.offline.Download.STATE_FAILED -> 3
            else -> 0
        },
        transitionSpec = {
            (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) +
                androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(0.55f, 500f), initialScale = 0.6f)) togetherWith
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
        },
        label = "songDownloadState",
    ) { s ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .then(if (s == 0 || s == 3) Modifier.clickable(onClickLabel = "Descargar") { startDownload() } else Modifier),
        ) {
            when (s) {
                2 -> com.xyonmusic.music.ui.component.SymbolIcon(sf("arrow.down.circle.fill"), contentDescription = "Descargada", size = 20.dp, tint = IosTheme.tint, chrome = 0f)
                1 -> com.xyonmusic.music.ui.component.DownloadRingIcon(download, size = 20.dp)
                3 -> com.xyonmusic.music.ui.component.SymbolIcon(sf("exclamationmark.circle"), contentDescription = "Error al descargar", size = 20.dp, tint = androidx.compose.ui.graphics.Color(0xFFFF6B60), chrome = 0f)
                else -> com.xyonmusic.music.ui.component.SymbolIcon(sf("arrow.down.circle"), contentDescription = "Descargar", size = 20.dp, tint = IosTheme.colors.secondaryLabel.copy(alpha = 0.55f), chrome = 0f)
            }
        }
    }
}
