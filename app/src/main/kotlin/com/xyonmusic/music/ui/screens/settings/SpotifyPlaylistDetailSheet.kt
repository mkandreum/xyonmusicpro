package com.xyonmusic.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.spotify.SpotifyMapper
import com.music.spotify.models.SpotifyPlaylist
import com.xyonmusic.music.db.entities.Song
import com.xyonmusic.music.ui.component.AnimatedActionButton
import com.xyonmusic.music.ui.ios.compat.AppTheme
import com.xyonmusic.music.ui.ios.compat.HorizontalDivider
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.viewmodels.SpotifyImportViewModel

private val SpotifyGreen = Color(0xFF1ED760)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlaylistDetailSheet(
    playlist: SpotifyPlaylist,
    viewModel: SpotifyImportViewModel,
    onDismiss: () -> Unit,
) {
    var songs by remember(playlist.id) { mutableStateOf<List<Song>?>(null) }
    var loadError by remember(playlist.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(playlist.id) {
        songs = null
        loadError = null
        runCatching { viewModel.fetchPlaylistTracks(playlist.id, maxTracks = 100) }
            .onSuccess { songs = it }
            .onFailure { loadError = it.message ?: "No se pudo cargar la playlist" }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val total = playlist.tracks?.total ?: 0
    val cover = SpotifyMapper.getPlaylistThumbnail(playlist)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = AppTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AsyncImage(
                    model = cover,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppTheme.colorScheme.surfaceContainerHigh),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = playlist.name,
                        style = AppTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = playlist.owner?.displayName.orEmpty(),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (total > 0) "$total canciones" else "Playlist de Spotify",
                        style = AppTheme.typography.bodySmall,
                        color = SpotifyGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            HorizontalDivider(
                color = AppTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            when {
                loadError != null -> Box(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = loadError.orEmpty(),
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.error,
                    )
                }
                songs == null -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
                songs.isNullOrEmpty() -> Box(
                    Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Esta playlist no tiene canciones disponibles.",
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(songs.orEmpty(), key = { index, song -> "$index:${song.song.title}" }) { index, song ->
                        SpotifyTrackRow(index + 1, song)
                    }
                    if (total > songs.orEmpty().size) {
                        item {
                            Text(
                                text = "Mostrando las primeras ${songs.orEmpty().size} de $total canciones.",
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = AppTheme.colorScheme.outlineVariant,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                AnimatedActionButton(
                    text = "Importar a mi biblioteca",
                    onClick = {
                        viewModel.startImport(listOf(playlist.id))
                        onDismiss()
                    },
                    modifier = Modifier.width(220.dp),
                    enabled = playlist.id.isNotBlank(),
                )
            }
        }
    }
}

@Composable
private fun SpotifyTrackRow(position: Int, song: Song) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = position.toString(),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
        )
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppTheme.colorScheme.surfaceContainerHigh),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = song.song.title,
                style = AppTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name },
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = formatDuration(song.song.duration),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatDuration(seconds: Int): String {
    if (seconds <= 0) return ""
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
