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
import com.xyonmusic.music.LocalListenTogetherManager
import com.xyonmusic.music.LocalPlayerConnection
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.ListItemHeight
import com.xyonmusic.music.listentogether.ConnectionState
import com.xyonmusic.music.listentogether.ListenTogetherEvent
import com.xyonmusic.music.listentogether.MAX_ROOM_CODE_LENGTH
import com.xyonmusic.music.listentogether.MIN_ROOM_CODE_LENGTH
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
    
    var showListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = com.xyonmusic.music.listentogether.RoomRole.NONE)
    // See Player.kt: gated on control mode, not on role.
    val canControlTogether = listenTogetherManager?.canControl?.collectAsState(initial = true)
    val isListenTogetherGuest = canControlTogether?.value == false
    val pendingSuggestions by listenTogetherManager?.pendingSuggestions?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

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

    ListenTogetherDialog(
        visible = showListenTogetherDialog,
        mediaMetadata = mediaMetadata,
        onDismiss = { showListenTogetherDialog = false }
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
    val listenTogetherManager = com.xyonmusic.music.LocalListenTogetherManager.current
    val isInRoom = listenTogetherManager?.isInRoom ?: false

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

@Composable
fun ListenTogetherDialog(
    visible: Boolean,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    if (!visible) return
    
    val context = LocalContext.current
    val listenTogetherManager = com.xyonmusic.music.LocalListenTogetherManager.current
    
    if (listenTogetherManager == null) {
        ListDialog(onDismiss = onDismiss) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SymbolIcon(symbol = sf("person.2.fill"), contentDescription = null, size = 48.dp, tint = AppTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.listen_together),
                        style = AppTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.listen_together_not_configured),
                        style = AppTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = AppTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
        return
    }
    
    val connectionState by listenTogetherManager.connectionState.collectAsStateWithLifecycle()
    val roomState by listenTogetherManager.roomState.collectAsStateWithLifecycle()
    val userId by listenTogetherManager.userId.collectAsStateWithLifecycle()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsStateWithLifecycle()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsStateWithLifecycle()
    
    var savedUsername by rememberPreference(com.xyonmusic.music.constants.ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    // Local UI state for join/create actions
    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    
    // User action menu state
    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Localized helper strings
    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    // User action menu dialog
    if (selectedUserForMenu != null && selectedUsername != null) {
        ListDialog(
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            }
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    SymbolIcon(symbol = sf("person.2.fill"), contentDescription = null, size = 40.dp, tint = AppTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.manage_user),
                            style = AppTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colorScheme.primary
                        )
                        Text(
                            text = selectedUsername ?: "",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable {
                            selectedUserForMenu?.let {
                                listenTogetherManager.kickUser(it, "Removed by host")
                            }
                            selectedUserForMenu = null
                            selectedUsername = null
                        },
                    shape = ContinuousRoundedRectangle(12.dp),
                    color = AppTheme.colorScheme.errorContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SymbolIcon(symbol = sf("xmark"), contentDescription = null, size = 24.dp, tint = AppTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.kick_user),
                                style = AppTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.kick_user_desc),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable {
                            selectedUserForMenu?.let { userId ->
                                selectedUsername?.let { username ->
                                    listenTogetherManager.blockUser(username)
                                    listenTogetherManager.kickUser(userId, R.string.user_blocked_by_host.toString())
                                }
                            }
                            selectedUserForMenu = null
                            selectedUsername = null
                        },
                    shape = ContinuousRoundedRectangle(12.dp),
                    color = AppTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        SymbolIcon(symbol = sf("xmark"), contentDescription = null, size = 24.dp, tint = AppTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.permanently_kick_user),
                                style = AppTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.permanently_kick_user_desc),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clickable {
                            selectedUserForMenu?.let {
                                listenTogetherManager.transferHost(it)
                            }
                            selectedUserForMenu = null
                            selectedUsername = null
                        },
                    shape = ContinuousRoundedRectangle(12.dp),
                    color = AppTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.crown),
                            contentDescription = null,
                            tint = AppTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.transfer_ownership),
                                style = AppTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.transfer_ownership_desc),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        return
    }

    // Sync usernameInput when savedUsername changes
    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    // Listen to low level events to update UI state (join rejected, approved, room created)
    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage = when {
                        reason.isNullOrBlank() -> joinRequestDeniedText
                        reason.contains("invalid", ignoreCase = true) == true -> invalidRoomCodeText
                        else -> "$joinRequestDeniedText: $reason"
                    }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }

                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }

                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val clipboard =
                        context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ListenTogetherRoom", event.roomCode)
                    clipboard.setPrimaryClip(clip)
                }

                else -> { /* ignore other events here */ }
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId
    
    ListDialog(onDismiss = onDismiss) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                SymbolIcon(symbol = sf("person.2.fill"), contentDescription = null, size = 40.dp, tint = AppTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (isInRoom) {
                        if (isHost) stringResource(R.string.hosting_room) else stringResource(R.string.in_room)
                    } else {
                        stringResource(R.string.listen_together)
                    },
                    style = AppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colorScheme.primary
                )
            }
        }
        
        // Connection status
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = ContinuousRoundedRectangle(16.dp),
                color = when (connectionState) {
                    ConnectionState.CONNECTED -> AppTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AppTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ConnectionState.ERROR -> AppTheme.colorScheme.error.copy(alpha = 0.15f)
                    ConnectionState.DISCONNECTED -> AppTheme.colorScheme.surfaceVariant
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = when (connectionState) {
                                        ConnectionState.CONNECTED -> AppTheme.colorScheme.primary
                                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AppTheme.colorScheme.secondary
                                        ConnectionState.ERROR -> AppTheme.colorScheme.error
                                        ConnectionState.DISCONNECTED -> AppTheme.colorScheme.outline
                                    },
                                    shape = ContinuousCapsule()
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> stringResource(R.string.listen_together_connected)
                                ConnectionState.CONNECTING -> stringResource(R.string.listen_together_connecting)
                                ConnectionState.RECONNECTING -> stringResource(R.string.listen_together_reconnecting)
                                ConnectionState.ERROR -> stringResource(R.string.listen_together_error)
                                ConnectionState.DISCONNECTED -> stringResource(R.string.listen_together_disconnected)
                            },
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> AppTheme.colorScheme.primary
                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> AppTheme.colorScheme.secondary
                                ConnectionState.ERROR -> AppTheme.colorScheme.error
                                ConnectionState.DISCONNECTED -> AppTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    
                    if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                            Button(
                                onClick = { listenTogetherManager.connect() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.connect), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { listenTogetherManager.disconnect() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(R.string.disconnect), fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { listenTogetherManager.forceReconnect() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reconnect", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(12.dp)) }
        
        if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
            item {
                Text(
                    text = stringResource(R.string.listen_together_background_disconnect_note),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isInRoom) {
            // Room status card
            roomState?.let { room ->
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = ContinuousRoundedRectangle(16.dp),
                        color = AppTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.room_code),
                                style = AppTheme.typography.labelMedium,
                                color = AppTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = room.roomCode,
                                    style = AppTheme.typography.headlineLarge,
                                    color = AppTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp
                                )
                            }
                            if (isHost) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val inviteLink = remember(room.roomCode) {
                                    "https://vivimusic-listen-together.onrender.com/listen?code=${room.roomCode}"
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Listen Together Link", inviteLink)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        SymbolIcon(symbol = sf("link"), contentDescription = stringResource(R.string.copy_link), size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.copy_link))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Room Code", room.roomCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        SymbolIcon(symbol = sf("doc.on.doc"), contentDescription = stringResource(R.string.copy_code), size = 18.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.copy_code))
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // Connected users - horizontal layout
                val connectedUsers = room.users.filter { it.isConnected }
                
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.connected_users, connectedUsers.size),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            connectedUsers.forEach { user ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clickable(
                                            enabled = isHost && user.userId != userId,
                                            onClick = {
                                                selectedUserForMenu = user.userId
                                                selectedUsername = user.username
                                            }
                                        )
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(52.dp),
                                            shape = ContinuousCapsule(),
                                            color = if (user.isHost) {
                                                AppTheme.colorScheme.primary
                                            } else if (user.userId == userId) {
                                                AppTheme.colorScheme.secondary
                                            } else {
                                                AppTheme.colorScheme.surfaceVariant
                                            }
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = user.username.take(1).uppercase(),
                                                    style = AppTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.isHost) {
                                                        AppTheme.colorScheme.onPrimary
                                                    } else if (user.userId == userId) {
                                                        AppTheme.colorScheme.onSecondary
                                                    } else {
                                                        AppTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                        
                                        // Host/You badge
                                        if (user.isHost || user.userId == userId) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .offset(x = 4.dp, y = 4.dp)
                                                    .size(18.dp),
                                                shape = ContinuousCapsule(),
                                                color = if (user.isHost) AppTheme.colorScheme.primary else AppTheme.colorScheme.secondary
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        painter = painterResource(
                                                            if (user.isHost) R.drawable.crown else R.drawable.person
                                                        ),
                                                        contentDescription = null,
                                                        tint = AppTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Text(
                                        text = user.username,
                                        style = AppTheme.typography.labelMedium,
                                        fontWeight = if (user.userId == userId) FontWeight.Bold else FontWeight.Medium,
                                        color = if (user.isHost) {
                                            AppTheme.colorScheme.primary
                                        } else {
                                            AppTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    if (user.isHost) {
                                        Text(
                                            text = stringResource(R.string.host_label),
                                            style = AppTheme.typography.labelSmall,
                                            color = AppTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    } else if (user.userId == userId) {
                                        Text(
                                            text = stringResource(R.string.you_label),
                                            style = AppTheme.typography.labelSmall,
                                            color = AppTheme.colorScheme.secondary.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Pending join requests (host only)
                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.pending_requests),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(pendingJoinRequests, key = { it.userId }) { request ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = AppTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = ContinuousCapsule(),
                                        color = AppTheme.colorScheme.secondary
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = request.username.take(1).uppercase(),
                                                style = AppTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppTheme.colorScheme.onSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = request.username,
                                        style = AppTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = AppTheme.colorScheme.onSurface
                                    )
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { listenTogetherManager.approveJoin(request.userId) }
                                    ) {
                                        SymbolIcon(symbol = sf("checkmark"), contentDescription = stringResource(R.string.approve), size = 24.dp, tint = AppTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { listenTogetherManager.rejectJoin(request.userId, "Rejected by host") }
                                    ) {
                                        SymbolIcon(symbol = sf("xmark"), contentDescription = stringResource(R.string.reject), size = 24.dp, tint = AppTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending suggestions (host only)
                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.pending_suggestions),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingSuggestions, key = { it.suggestionId }) { suggestion ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = ContinuousRoundedRectangle(12.dp),
                            color = AppTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    SymbolIcon(symbol = sf("music.note.list"), contentDescription = null, size = 24.dp, tint = AppTheme.colorScheme.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.trackInfo.title,
                                            style = AppTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = AppTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = suggestion.fromUsername,
                                            style = AppTheme.typography.bodySmall,
                                            color = AppTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { listenTogetherManager.approveSuggestion(suggestion.suggestionId) }
                                    ) {
                                        SymbolIcon(symbol = sf("checkmark"), contentDescription = stringResource(R.string.approve), size = 24.dp, tint = AppTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { listenTogetherManager.rejectSuggestion(suggestion.suggestionId, "Rejected by host") }
                                    ) {
                                        SymbolIcon(symbol = sf("xmark"), contentDescription = stringResource(R.string.reject), size = 24.dp, tint = AppTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(R.string.cancel),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = {
                                listenTogetherManager.leaveRoom()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colorScheme.error
                            )
                        ) {
                            SymbolIcon(symbol = sf("rectangle.portrait.and.arrow.right"), contentDescription = null, size = 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.leave_room), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Join/Create room section
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = ContinuousRoundedRectangle(16.dp),
                    color = AppTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.listen_together_description),
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(stringResource(R.string.username)) },
                            placeholder = { Text(stringResource(R.string.enter_username)) },
                            leadingIcon = {
                                SymbolIcon(symbol = sf("person.fill"), contentDescription = null, tint = AppTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (usernameInput.isNotBlank()) {
                                    IconButton(onClick = { usernameInput = "" }) {
                                        SymbolIcon(symbol = sf("xmark"), contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = ContinuousRoundedRectangle(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colorScheme.primary,
                                unfocusedBorderColor = AppTheme.colorScheme.outline,
                                focusedLabelColor = AppTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        HorizontalDivider()
                        
                        Text(
                            text = stringResource(R.string.join_existing_room),
                            style = AppTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colorScheme.primary
                        )
                        
                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(MAX_ROOM_CODE_LENGTH) },
                            label = { Text(stringResource(R.string.room_code)) },
                            placeholder = { Text("ABCD1234") },
                            supportingText = {
                                Text(
                                    text = "${roomCodeInput.length}/8",
                                    style = AppTheme.typography.labelSmall,
                                    color = AppTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.token),
                                    null,
                                    tint = AppTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = ContinuousRoundedRectangle(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppTheme.colorScheme.primary,
                                unfocusedBorderColor = AppTheme.colorScheme.outline,
                                focusedLabelColor = AppTheme.colorScheme.primary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Status messages
                        if (isJoiningRoom) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AppTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = waitingForApprovalText,
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        joinErrorMessage?.let { msg ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ContinuousRoundedRectangle(8.dp),
                                color = AppTheme.colorScheme.error.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    SymbolIcon(symbol = sf("exclamationmark.circle"), contentDescription = null, size = 18.dp, tint = AppTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = AppTheme.typography.bodyMedium,
                                        color = AppTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                val finalUsername = username.trim()
                                if (finalUsername.isNotBlank()) {
                                    savedUsername = finalUsername
                                    Toast.makeText(context, R.string.creating_room, Toast.LENGTH_SHORT).show()
                                    isCreatingRoom = true
                                    isJoiningRoom = false
                                    joinErrorMessage = null
                                    // createRoom allocates the code and opens the
                                    // socket against that room itself. A bare
                                    // connect() first just dials the server root,
                                    // which 404s and burns a reconnect attempt.
                                    listenTogetherManager.createRoom(finalUsername)
                                } else {
                                    Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colorScheme.primary
                            )
                        ) {
                            SymbolIcon(symbol = sf("plus"), contentDescription = null, size = 18.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.create_room), fontWeight = FontWeight.SemiBold)
                        }
                        
                        if (roomCodeInput.trim().length >= MIN_ROOM_CODE_LENGTH) {
                            Button(
                                onClick = {
                                    val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                    val finalUsername = username.trim()
                                    if (finalUsername.isNotBlank()) {
                                        savedUsername = finalUsername
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.joining_room, roomCodeInput),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isJoiningRoom = true
                                        isCreatingRoom = false
                                        joinErrorMessage = null
                                        listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                                    } else {
                                        Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppTheme.colorScheme.secondary
                                )
                            ) {
                                SymbolIcon(symbol = sf("arrow.right.to.line"), contentDescription = null, size = 18.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.join_room), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.cancel),
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
