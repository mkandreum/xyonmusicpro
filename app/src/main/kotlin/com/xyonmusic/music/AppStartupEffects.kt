/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.xyonmusic.music.R
import com.xyonmusic.music.constants.EnableHighRefreshRateKey
import com.xyonmusic.music.core.UpdateNotificationHelper
import com.xyonmusic.music.core.updater.checkForUpdate
import com.xyonmusic.music.core.updater.getAutoUpdateCheckSetting
import com.xyonmusic.music.core.updater.getLastNotifiedUpdateTag
import com.xyonmusic.music.core.updater.saveLastNotifiedUpdateTag
import com.xyonmusic.music.core.updater.getUpdateNotificationsSetting
import com.xyonmusic.music.core.updater.saveUpdateAvailableState
import com.xyonmusic.music.playback.PlayerConnection
import com.xyonmusic.music.utils.rememberPreference
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import timber.log.Timber

/** One-shot app effects: update check, refresh-rate request, and the owner-room toast. */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class,
)
@Composable
fun AppStartupEffects(activity: android.app.Activity, playerConnection: PlayerConnection?) {
    val enableHighRefreshRate by rememberPreference(EnableHighRefreshRateKey, defaultValue = true)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!getAutoUpdateCheckSetting(context)) return@LaunchedEffect

        delay(2000L.milliseconds)
        checkForUpdate(
            context = context,
            onSuccess = { latestVersion, isAvailable, _, _, _, _, _, _ ->
                val currentVersion = BuildConfig.VERSION_NAME
                Timber.tag("UpdateCheck").d("Startup check success. Latest: $latestVersion, Current: $currentVersion, isAvailable: $isAvailable")
                saveUpdateAvailableState(context, isAvailable)

                if (isAvailable && getUpdateNotificationsSetting(context) && getLastNotifiedUpdateTag() != latestVersion) {
                    saveLastNotifiedUpdateTag(latestVersion)
                    Timber.tag("UpdateCheck").d("Posting update notification for $latestVersion")
                    UpdateNotificationHelper.showUpdateNotification(context, latestVersion)
                }
            },
            onError = {
                Timber.tag("UpdateCheck").e("Startup check failed")
                // Preserve the last known update state when the check fails offline.
            },
        )
    }

    LaunchedEffect(enableHighRefreshRate) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val layoutParams = window.attributes
            val modes = window.windowManager.defaultDisplay.supportedModes
            val currentMode = window.windowManager.defaultDisplay.mode
            if (enableHighRefreshRate) {
                // Same resolution only, so changing the rate never reflows the UI. Aim for 120 Hz,
                // not simply the fastest mode: on a 144 Hz panel every frame would have to be
                // ready in 6.9 ms instead of 8.3 ms, and the frames Compose misses at 144 Hz are
                // the stutter you see while scrolling. Below 120 Hz, take the fastest there is.
                val sameSize = modes.filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                val target = sameSize.filter { it.refreshRate <= TargetRefreshRate + 1f }.maxByOrNull { it.refreshRate }
                    ?: sameSize.minByOrNull { it.refreshRate }
                layoutParams.preferredDisplayModeId = target?.modeId ?: 0
            } else {
                val mode60 = modes.firstOrNull { kotlin.math.abs(it.refreshRate - 60f) < 1f }
                    ?: modes.minByOrNull { kotlin.math.abs(it.refreshRate - 60f) }

                if (mode60 != null) {
                    layoutParams.preferredDisplayModeId = mode60.modeId
                }
            }
            window.attributes = layoutParams
        } else {
            val params = window.attributes
            params.preferredRefreshRate = if (enableHighRefreshRate) {
                // Zero means no preference, so request the fastest supported rate.
                window.windowManager.defaultDisplay.supportedRefreshRates
                    .filter { it <= TargetRefreshRate + 1f }
                    .maxOrNull()
                    ?: 0f
            } else {
                60f
            }
            window.attributes = params
        }
    }

    LaunchedEffect(playerConnection) {
        val connection = playerConnection ?: return@LaunchedEffect
        connection.playbackBlockedByRoom.collect { blocked ->
            if (!blocked) return@collect
            Toast.makeText(
                activity,
                activity.getString(R.string.listen_together_host_controls),
                Toast.LENGTH_SHORT,
            ).show()
            connection.playbackBlockedByRoom.value = false
        }
    }
}

/** The refresh rate the app asks for: smooth, and a frame budget Compose reliably meets. */
private const val TargetRefreshRate = 120f
