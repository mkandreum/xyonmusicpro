package com.xyonmusic.music.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.music.spotify.SpotifyHomeItem
import com.music.spotify.SpotifyHomeShelf
import com.xyonmusic.music.ui.component.NavigationTitle

/** How many Spotify shelves Home shows, so Spotify complements Home instead of taking it over. */
private const val SpotifyShelfLimit = 3

/**
 * "De tu Spotify": the shelves of the user's own Spotify home (made for you, daily mixes, recent
 * mixes...). A tap plays the playlist through YouTube; the card being resolved says so.
 */
internal fun LazyListScope.spotifyHomeSection(
    deps: HomeSectionDeps,
    shelves: List<SpotifyHomeShelf>,
    resolvingUri: String?,
    onPlay: (SpotifyHomeItem) -> Unit,
) {
    shelves.filter { it.items.isNotEmpty() }.take(SpotifyShelfLimit).forEachIndexed { index, shelf ->
        item(key = "spotify_title_$index", contentType = "section_title") {
            NavigationTitle(
                label = "De tu Spotify",
                title = shelf.title,
                modifier = Modifier.animateItem(),
            )
        }
        shelfRow(
            key = "spotify_row_$index",
            deps = deps,
            entries = shelf.items.distinctBy { it.uri }.map { item ->
                ShelfEntry(
                    id = "spotify_${index}_${item.uri}",
                    thumbnailUrl = item.artworkUrl,
                    title = item.title,
                    subtitle = if (item.uri == resolvingUri) "Preparando…" else "Spotify",
                    onClick = { onPlay(item) },
                )
            },
            compact = true,
        )
    }
}
