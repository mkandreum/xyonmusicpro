/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * One motion for every menu in the app (the glass menu, the "⋯" dropdowns and the tab menu),
 * so they all open and close alike.
 *
 * Open: the panel starts at [OpenFromScale] of its size, not from a dot, fades in over a short
 * curve and settles on two springs with a touch of overshoot: width a little ahead of height,
 * which is what reads as the panel unfolding out of the finger rather than a flat zoom.
 *
 * Close: a quick shrink to [CloseToScale] while it fades out. It never flattens into a line or
 * a dot, because it is gone before the shrink could get that far.
 */
object MenuMotion {
    const val OpenFromScale = 0.4f
    const val CloseToScale = 0.7f

    val OpenFade = tween<Float>(durationMillis = 140, easing = LinearOutSlowInEasing)
    val OpenWidth = spring<Float>(dampingRatio = 0.8f, stiffness = 560f, visibilityThreshold = 0.001f)
    val OpenHeight = spring<Float>(dampingRatio = 0.86f, stiffness = 420f, visibilityThreshold = 0.001f)

    val CloseFade = tween<Float>(durationMillis = 110, easing = FastOutSlowInEasing)
    val CloseScale = spring<Float>(dampingRatio = 1f, stiffness = 1100f, visibilityThreshold = 0.001f)
}
