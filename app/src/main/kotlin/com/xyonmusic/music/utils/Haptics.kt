/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * App-wide micro-haptics through the vibrator itself (EFFECT_TICK / EFFECT_CLICK), so they are felt even on
 * phones that silence View haptic constants such as KEYBOARD_TAP.
 */

package com.xyonmusic.music.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    @Volatile private var vibrator: Vibrator? = null

    /** Uptime of the last haptic any component played, so the generic tap tick never doubles it. */
    @Volatile var lastAt: Long = 0L
        private set

    /** Records a haptic played elsewhere (View or Compose haptics), so the tap tick stays silent. */
    fun mark() { lastAt = android.os.SystemClock.uptimeMillis() }

    /** True when a component has just vibrated on its own. */
    fun playedRecently(windowMs: Long = 90): Boolean = android.os.SystemClock.uptimeMillis() - lastAt < windowMs

    private fun vib(context: Context): Vibrator? = vibrator ?: run {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator = v
        v
    }

    /** Light tick: taps, tab changes, steps. */
    fun tick(context: Context) = play(context, VibrationEffect.EFFECT_TICK, 12)

    /** Firmer click: confirmations, opening menus and screens. */
    fun click(context: Context) = play(context, VibrationEffect.EFFECT_CLICK, 20)

    private fun play(context: Context, effect: Int, fallbackMs: Long) {
        mark()
        val v = vib(context) ?: return
        if (!v.hasVibrator()) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) v.vibrate(VibrationEffect.createPredefined(effect))
            else v.vibrate(VibrationEffect.createOneShot(fallbackMs, 80))
        }
    }
}
