/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.menu

import com.xyonmusic.music.ui.component.DownloadRingIcon
import com.xyonmusic.music.ui.component.sf
import com.xyonmusic.music.ui.component.SymbolIcon
import com.xyonmusic.music.ui.component.menuBottomInset
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.SFGaugeWithNeedle
import com.composables.sfsymbols.monochrome.SFSliderHorizontal3
import com.composables.sfsymbols.monochrome.SFBellFill
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.xyonmusic.music.ui.component.shapes.ContinuousCapsule
import com.xyonmusic.music.ui.component.shapes.ContinuousRoundedRectangle
import com.xyonmusic.music.ui.ios.compat.AlertDialog
import com.xyonmusic.music.ui.ios.compat.Button
import androidx.compose.material3.ButtonDefaults
import com.xyonmusic.music.ui.ios.compat.CircularProgressIndicator
import com.xyonmusic.music.ui.ios.compat.FilledTonalButton
import com.xyonmusic.music.ui.ios.compat.HorizontalDivider
import com.xyonmusic.music.ui.ios.compat.Icon
import com.xyonmusic.music.ui.ios.compat.IconButton
import com.xyonmusic.music.ui.ios.compat.LinearProgressIndicator
import com.xyonmusic.music.ui.ios.compat.AppTheme
import com.xyonmusic.music.ui.ios.compat.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.xyonmusic.music.ui.ios.compat.Surface
import com.xyonmusic.music.ui.ios.compat.Text
import com.xyonmusic.music.ui.ios.compat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.music.innertube.YouTube
import com.xyonmusic.music.LocalDatabase
import com.xyonmusic.music.LocalDownloadUtil
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.ListItemHeight
import com.xyonmusic.music.models.MediaMetadata
import com.xyonmusic.music.playback.ExoDownloadService
import com.xyonmusic.music.core.AudioDeviceMenu
import com.xyonmusic.music.ui.component.BottomSheetState
import com.xyonmusic.music.ui.component.ListDialog
import com.xyonmusic.music.ui.component.IosMenuGroup
import com.xyonmusic.music.ui.component.IosMenuItemData
import com.xyonmusic.music.ui.component.NewAction
import com.xyonmusic.music.ui.component.NewActionGrid
import com.xyonmusic.music.utils.rememberPreference
import com.xyonmusic.music.ui.player.rememberSleepTimerState
import com.xyonmusic.music.ui.player.SleepTimerDialogHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val ringtoneViewModel = com.xyonmusic.music.LocalRingtoneViewModel.current
    val database = LocalDatabase.current.get()
    val playerConnection = LocalPlayerConnection.current ?: return
    val sleepTimer = rememberSleepTimerState(playerConnection)
    SleepTimerDialogHost(playerConnection, sleepTimer)
    var showAudioDevices by remember { mutableStateOf(false) }
    if (showAudioDevices) AudioDeviceMenu(onDismiss = { showAudioDevices = false })
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val saavnEnabled = false

    val download by LocalDownloadUtil.current.get().getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    // Listen Together is gone: playback is never locked to a room host.
    val isListenTogetherGuest = false

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )


    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists, key = { it.id!! }) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                    Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + menuBottomInset(),
        ),
    ) {
        item {
            val startingRadioText = stringResource(R.string.starting_radio)
            NewActionGrid(
                actions = listOfNotNull(
                    if (!isListenTogetherGuest && !mediaMetadata.isLocalFile) {
                        NewAction(
                            icon = {
                                SymbolIcon(symbol = sf("dot.radiowaves.left.and.right"), contentDescription = null, size = 32.dp)
                            },
                            text = stringResource(R.string.start_an_radio),
                            onClick = {
                                Toast.makeText(context, startingRadioText, Toast.LENGTH_SHORT).show()
                                playerConnection.startRadioSeamlessly()
                                onDismiss()
                            }
                        )
                    } else null,
                    NewAction(
                        icon = {
                            SymbolIcon(symbol = sf("text.badge.plus"), contentDescription = null, size = 32.dp)
                        },
                        text = stringResource(R.string.add_to_an_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    // Escuchar juntos: opens the LiquidUI glass sheet at the root.
                    NewAction(
                        icon = {
                            SymbolIcon(symbol = sf("person.2.fill"), contentDescription = null, size = 32.dp)
                        },
                        text = "Juntos",
                        onClick = {
                            onDismiss()
                            com.xyonmusic.music.ui.feature.FeatureSheets.juntos = true
                        }
                    ),
                    if (mediaMetadata.isLocalFile) null else NewAction(
                        icon = {
                            SymbolIcon(symbol = sf("square.and.arrow.up"), contentDescription = null, size = 32.dp)
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            val intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    com.xyonmusic.music.utils.ShareUtils.songUrl(mediaMetadata.id)
                                )
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                columns = if (isListenTogetherGuest) 3 else 4,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            // Karaoke: the real-time vocal remover, switched on or off for whatever plays.
            val (vocalLevel, setVocalLevel) = com.xyonmusic.music.utils.rememberPreference(com.xyonmusic.music.constants.VocalSuppressionKey, 0f)
            IosMenuGroup(
                items = listOf(
                    IosMenuItemData(
                        title = { Text(text = "Quitar la voz") },
                        description = { Text(text = "Karaoke con esta canción") },
                        icon = { SymbolIcon(symbol = sf("mic.slash.fill"), contentDescription = null, size = 24.dp) },
                        trailingContent = {
                            io.github.mkandreum.liquidbar.ui.LiquidToggle(
                                checked = vocalLevel > 0.5f,
                                onCheckedChange = { on -> setVocalLevel(if (on) 1f else 0f) },
                                accent = androidx.compose.ui.graphics.Color(0xFFFA2D48),
                            )
                        },
                        onClick = { setVocalLevel(if (vocalLevel > 0.5f) 0f else 1f) },
                    ),
                ),
            )
        }

        item {
            IosMenuGroup(
                items = buildList {


                    if (artists.isNotEmpty()) {
                        add(
                            IosMenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    SymbolIcon(symbol = sf("music.mic"), contentDescription = null, size = 24.dp)
                                },
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }
                    add(
                        IosMenuItemData(
                            title = { Text(text = stringResource(R.string.sleep_timer)) },
                            icon = { SymbolIcon(symbol = sf("moon.zzz.fill"), contentDescription = null, size = 24.dp) },
                            onClick = {
                                onDismiss()
                                sleepTimer.showDialog.value = true
                            },
                        )
                    )
                    if (mediaMetadata.album != null) {
                        add(
                            IosMenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.album.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    SymbolIcon(symbol = sf("square.stack"), contentDescription = null, size = 24.dp)
                                },
                                onClick = {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        )
                    }
                    val isInLibrary = librarySong?.song?.inLibrary != null
                    add(
                        IosMenuItemData(
                            title = { 
                                Text(
                                    text = stringResource(
                                        if (isInLibrary) R.string.remove_from_library
                                        else R.string.add_to_library
                                    )
                                )
                            },
                            icon = {
                                SymbolIcon(
                                    symbol = if (isInLibrary) sf("text.badge.checkmark") else sf("text.badge.plus"),
                                    contentDescription = null,
                                    size = 24.dp,
                                )
                            },
                            onClick = {
                                playerConnection.toggleLibrary()
                                onDismiss()
                            }
                        )
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            // Compact menu: only the equalizer down here; download, details, tempo, ringtone and
            // ambient mode are gone (download lives in the player itself).
            if (isQueueTrigger != true) IosMenuGroup(
                items = listOf(
                    IosMenuItemData(
                        title = { Text(text = stringResource(R.string.equalizer)) },
                        icon = {
                            SymbolIcon(symbol = sf("slider.vertical.3"), contentDescription = null, size = 24.dp)
                        },
                        onClick = {
                            navController.navigate("settings/equalizer")
                            onDismiss()
                        }
                    )
                )
            )
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }
    val isInRoom = false

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                if (!isInRoom) {
                    ValueAdjuster(
                        icon = SfSymbols.Monochrome.SFGaugeWithNeedle,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ValueAdjuster(
                    icon = SfSymbols.Monochrome.SFSliderHorizontal3,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    icon: ImageVector,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            SymbolIcon(symbol = sf("minus"), contentDescription = null)
        }

        Text(
            text = valueText(currentValue),
            style = AppTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            SymbolIcon(symbol = sf("plus"), contentDescription = null)
        }
    }
}

