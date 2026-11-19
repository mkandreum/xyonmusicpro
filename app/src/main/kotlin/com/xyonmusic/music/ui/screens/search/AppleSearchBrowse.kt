/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.screens.search

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.xyonmusic.music.ui.utils.youtubeBrowseRoute
import coil3.compose.AsyncImage
import com.xyonmusic.music.ui.component.SymbolIcon
import com.xyonmusic.music.ui.component.sf
import com.xyonmusic.music.ui.component.shapes.ContinuousRoundedRectangle
import com.xyonmusic.music.ui.feature.FeatureSheets
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.ui.ios.foundation.iosRowPress
import com.xyonmusic.music.ui.screens.library.apple.SfStyle
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.ui.LiquidGlassButton
import com.music.innertube.YouTube
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import com.music.innertube.pages.MoodAndGenres
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

/**
 * Apple Music's Search landing (iOS 26 reference, `938cf0…png`): two across, every
 * category a rounded colour tile with a duotone photo in that colour and its name in
 * white at the bottom-left. YouTube Music's moods & genres carry a colour but no picture,
 * so each tile borrows the lead artwork of its own category page, fetched as the tile
 * scrolls into view and cached for the session.
 */
@HiltViewModel
class AppleSearchViewModel @Inject constructor() : ViewModel() {
    private val _categories = MutableStateFlow<List<MoodAndGenres.Item>?>(null)
    val categories = _categories.asStateFlow()

    private val _art = MutableStateFlow<Map<String, String?>>(artCache.toMap())
    val art = _art.asStateFlow()

    // Bound artwork requests as categories scroll into view.
    private val gate = Semaphore(4)
    private val pending = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    // IO, not Main: Ktor decodes the response on the calling dispatcher.
    init {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.moodAndGenres().onSuccess { sections ->
                _categories.value = sections.flatMap { it.items }.distinctBy { it.endpoint.browseId + it.endpoint.params }
            }
        }
    }

    fun loadArt(item: MoodAndGenres.Item) {
        val key = item.key
        if (_art.value.containsKey(key) || !pending.add(key)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                gate.withPermit {
                    val url = YouTube.browse(item.endpoint.browseId, item.endpoint.params).getOrNull()
                        ?.items?.asSequence()?.flatMap { it.items.asSequence() }
                        // People read best in the duotone, then covers.
                        ?.sortedBy { if (it is ArtistItem) 0 else if (it is PlaylistItem || it is AlbumItem) 1 else 2 }
                        ?.firstOrNull()?.thumbnail
                    // ConcurrentHashMap rejects null; a category may have no artwork.
                    if (url != null) artCache[key] = url
                    _art.update { it + (key to url) }
                }
            } finally {
                pending.remove(key)
            }
        }
    }

    private companion object {
        val artCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    }
}

private val MoodAndGenres.Item.key get() = endpoint.browseId + "|" + endpoint.params

@Composable
fun AppleSearchBrowse(
    navController: NavController,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
    viewModel: AppleSearchViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val art by viewModel.art.collectAsStateWithLifecycle()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Two columns on phone; keep tiles near their reference width in the iPad pane.
        val startInset = contentPadding.calculateStartPadding(androidx.compose.ui.platform.LocalLayoutDirection.current)
        val columns = maxOf(2, ((maxWidth - startInset - 24.dp) / 189.dp).toInt())
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = startInset + 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                header()
            }
            // DJ con IA: the one highlighted card above the genres, as in the sketch.
            item(key = "dj_card", span = { GridItemSpan(maxLineSpan) }, contentType = "dj") {
                DjFeatureCard(
                    onClick = { FeatureSheets.dj = true },
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                )
            }
            items(categories.orEmpty(), key = { it.key }, contentType = { "tile" }) { item ->
                LaunchedEffect(item.key) { viewModel.loadArt(item) }
                CategoryTile(
                    title = item.title,
                    color = Color(item.stripeColor),
                    art = art[item.key],
                ) {
                    navController.navigate(youtubeBrowseRoute(item.endpoint.browseId, item.endpoint.params))
                }
            }
        }
    }
}

/**
 * Highlighted DJ card at the top of Discover: real LiquidUI glass over its own tinted plate, so it
 * keeps the crystal edge without needing a screen-sized backdrop. Opens the DJ sheet at the root.
 */
@Composable
private fun DjFeatureCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backdrop = rememberGlassBackdrop()
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().height(116.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .glassSource(backdrop)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3A0D15), Color(0xFF121012))),
                    shape,
                )
                .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape),
        )
        LiquidGlassButton(
            onClick = onClick,
            backdrop = backdrop,
            modifier = Modifier.matchParentSize(),
            shape = shape,
            glass = LiquidGlassStyle.Dark.copy(blur = 10.dp, refractionHeight = 26.dp, refractionAmount = 26.dp),
            tint = Color(0xFFFA2D48).copy(alpha = 0.24f),
            onClickLabel = "DJ con IA",
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(
                        symbol = sf("waveform"),
                        contentDescription = null,
                        size = 28.dp,
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "DJ con IA",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Una sesión continua según tu ambiente",
                        color = Color.White.copy(alpha = 0.66f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                SymbolIcon(
                    symbol = sf("chevron.right"),
                    contentDescription = null,
                    size = 16.dp,
                    tint = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

/**
 * Search category tile with a tinted grayscale image and a bottom-left white title.
 */
@Composable
internal fun CategoryTile(
    title: String,
    color: Color,
    art: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val ground = color.copy(alpha = 1f)
    val shape = ContinuousRoundedRectangle(15.dp)
    Box(
        modifier = modifier
            .aspectRatio(181f / 105f)
            .clip(shape)
            .background(ground)
            .border(0.25.dp, Color.Black.copy(alpha = 0.1f), shape)
            .iosRowPress(onClick = onClick),
    ) {
        if (art != null) {
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.72f)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        blendMode = BlendMode.Multiply
                    },
            )
            // Lifts the duotone's highlights back toward the tile colour.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(ground.copy(alpha = 0.25f), blendMode = BlendMode.Screen)
            }
        }
        // Keeps the name legible where the photo runs dark behind it.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.28f))),
        )
        Text(
            text = title,
            style = SfStyle(15.sp, 20.sp, FontWeight.SemiBold, (-0.23).sp),
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}
