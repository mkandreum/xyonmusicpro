/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.xyonmusic.music.constants.AppLanguageKey
import com.xyonmusic.music.constants.DisableScreenshotKey
import com.xyonmusic.music.constants.SYSTEM_DEFAULT
import com.xyonmusic.music.constants.StopMusicOnTaskClearKey
import com.xyonmusic.music.db.MusicDatabase
import com.xyonmusic.music.history.HistoryLocalDataSource
import com.xyonmusic.music.playback.DownloadUtil
import com.xyonmusic.music.playback.MusicService
import com.xyonmusic.music.playback.PlaybackServiceConnector
import com.xyonmusic.music.playback.PlayerConnection
import com.xyonmusic.music.utils.SyncUtils
import com.xyonmusic.music.utils.dataStore
import com.xyonmusic.music.utils.get
import com.xyonmusic.music.utils.setAppLocale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PLAY_INTRO = "com.xyonmusic.music.extra.PLAY_INTRO"
        internal const val ACTION_SEARCH = "com.xyonmusic.music.action.SEARCH"
        internal const val ACTION_LIBRARY = "com.xyonmusic.music.action.LIBRARY"
    }

    @Inject
    lateinit var database: dagger.Lazy<MusicDatabase>

    @Inject
    lateinit var downloadUtil: dagger.Lazy<DownloadUtil>

    @Inject
    lateinit var syncUtils: dagger.Lazy<SyncUtils>

    @Inject
    lateinit var historyDataSource: dagger.Lazy<HistoryLocalDataSource>

    @Inject
    lateinit var listenTogetherManager: dagger.Lazy<com.xyonmusic.music.listentogether.ListenTogetherManager>

    @Inject
    lateinit var partyManager: dagger.Lazy<com.xyonmusic.music.jam.JamManager>

    @Inject
    lateinit var playbackServiceConnector: PlaybackServiceConnector

    private var pendingIntent: Intent? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var activityStarted = false
    private var pendingPlaybackBind: Runnable? = null

    override fun onStart() {
        super.onStart()
        activityStarted = true
        if (MusicService.isRunning) {
            bindPlaybackService()
        } else {
            val bind = Runnable {
                pendingPlaybackBind = null
                if (activityStarted) bindPlaybackService()
            }
            pendingPlaybackBind = bind
            window.decorView.postDelayed(bind, 32L)
        }
    }

    override fun onStop() {
        activityStarted = false
        pendingPlaybackBind?.let(window.decorView::removeCallbacks)
        pendingPlaybackBind = null
        playbackServiceConnector.unbind(this)
        super.onStop()
    }

    private fun bindPlaybackService() {
        // The service promotes itself when bound, so bind only while this activity is visible.
        playbackServiceConnector.bind(this, lifecycleScope) { connection ->
            playerConnection = connection
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Handle back navigation here; swiping the task away is handled by the service.
        if (dataStore.get(StopMusicOnTaskClearKey, false) &&
            playerConnection?.isPlaying?.value == true &&
            isFinishing
        ) {
            // onStop already unbound the connection; only stop the service here.
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }

    internal fun takePendingIntent(): Intent? {
        val current = pendingIntent
        pendingIntent = null
        return current
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Retain the intent until Compose's listener is ready to handle it.
        pendingIntent = intent
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    private var tapDownX = 0f
    private var tapDownY = 0f
    private var tapDownAt = 0L

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        when (ev.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                com.xyonmusic.music.ui.component.MenuAnchor.lastDown =
                    androidx.compose.ui.geometry.Offset(ev.x, ev.y)
                tapDownX = ev.x; tapDownY = ev.y; tapDownAt = ev.eventTime
            }
            // A light tick on every tap, anywhere in the app: buttons, rows, tabs, menus. Scrolls and drags
            // (finger moved) and long presses don't tick here; they have their own haptics.
            android.view.MotionEvent.ACTION_UP -> {
                val slop = android.view.ViewConfiguration.get(this).scaledTouchSlop
                val moved = kotlin.math.hypot(ev.x - tapDownX, ev.y - tapDownY) > slop
                if (!moved && ev.eventTime - tapDownAt < 350) {
                    // Wait a frame and a half: if the tapped control vibrates on its own (tabs, play,
                    // skip...), skip this generic tick so the user never feels a double tap.
                    window.decorView.postDelayed({
                        if (!com.xyonmusic.music.utils.Haptics.playedRecently()) com.xyonmusic.music.utils.Haptics.tick(this)
                    }, 28L)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Liquid pieces report their own haptics, so the generic tap tick does not double them.
        io.github.mkandreum.liquidbar.LiquidHaptics.listener = com.xyonmusic.music.utils.Haptics::mark
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val locale = dataStore[AppLanguageKey]
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, locale)
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        // Cold launch from the launcher only (WelcomeActivity hands off with this extra);
        // notification/deep-link opens go straight to the app.
        setContent {
            // Every Compose haptic is recorded, so the generic tap tick never doubles it.
            val baseHaptics = androidx.compose.ui.platform.LocalHapticFeedback.current
            val markingHaptics = androidx.compose.runtime.remember(baseHaptics) {
                object : androidx.compose.ui.hapticfeedback.HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                        com.xyonmusic.music.utils.Haptics.mark()
                        baseHaptics.performHapticFeedback(hapticFeedbackType)
                    }
                }
            }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalHapticFeedback provides markingHaptics) {
            Box(Modifier.fillMaxSize()) {
                AppRootContent(
                    activity = this@MainActivity,
                    intentRouter = deepLinkRouter,
                    playerConnection = playerConnection,
                    database = database,
                    downloadUtil = downloadUtil,
                    syncUtils = syncUtils,
                    historyDataSource = historyDataSource,
                )
            }
            }
        }
    }

    internal val deepLinkRouter by lazy {
        AppIntentRouter(this, { playerConnection }, listenTogetherManager, partyManager)
    }
}
