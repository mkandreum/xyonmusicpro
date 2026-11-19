/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Port of React Bits' ElasticSlider (github.com/DavidHDev/react-bits): drag past
 * either end and the track stretches/squashes like rubber instead of just
 * clamping, with the edge icon nudging outward and pulsing, then springs back
 * on release. The web version drives this off Framer Motion's clientX/overflow
 * motion values (`.jump()` while dragging, `animate()` only on release); here
 * that same shape comes from a plain float while dragging and an [Animatable]
 * only for the release bounce, since Compose's [Animatable.snapTo] is suspend
 * and drag callbacks below aren't.
 */

package com.xyonmusic.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xyonmusic.music.ui.component.shapes.ContinuousCapsule
import kotlinx.coroutines.launch
import kotlin.math.exp

/** How far the track can visually stretch past its own bounds, no matter how
 *  far past the edge the finger actually goes — [decay] softly caps toward
 *  this without ever quite reaching it. */
private val MaxOverflow = 28.dp

private enum class ElasticRegion { LEFT, MIDDLE, RIGHT }

@Composable
private fun ElasticIcon(pulse: Animatable<Float, AnimationVector1D>, pullPx: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = pulse.value
            scaleY = pulse.value
            translationX = pullPx
        }
    ) {
        content()
    }
}

private fun regionFor(rawX: Float, widthPx: Float): ElasticRegion = when {
    rawX < 0f -> ElasticRegion.LEFT
    rawX > widthPx -> ElasticRegion.RIGHT
    else -> ElasticRegion.MIDDLE
}

private fun overflowPast(rawX: Float, widthPx: Float): Float = when {
    rawX < 0f -> -rawX
    rawX > widthPx -> rawX - widthPx
    else -> 0f
}

/** Same soft-cap the reference uses: a sigmoid that approaches [max] but never
 *  reaches it, so the elastic pull always has somewhere left to give. */
private fun decay(value: Float, max: Float): Float {
    if (max == 0f) return 0f
    val entry = value / max
    val sigmoid = 2 * (1 / (1 + exp(-entry)) - 0.5f)
    return sigmoid * max
}
