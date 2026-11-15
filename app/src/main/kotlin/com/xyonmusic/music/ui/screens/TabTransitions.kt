package com.xyonmusic.music.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry

/**
 * Bar tab switches, iOS 26 style: the page that leaves blurs out and dims, the new one arrives
 * coming into focus and rising 16 dp on a soft spring. No scale, so no cropped edges in the margins,
 * and the old page never fully disappears before the new one is visible, so no flash of background.
 */
internal object TabTransitions {
    private val riseSpring = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.82f, stiffness = 380f)
    private const val FadeMillis = 260
    const val MaxBlurDp = 24f

    fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
        initialState.destination.route in Screens.MainRoutes && targetState.destination.route in Screens.MainRoutes

    /** The page coming in: fades in and rises into place; [tabDepthBlur] brings it into focus. */
    fun enter(): EnterTransition =
        fadeIn(tween(FadeMillis, easing = FastOutSlowInEasing)) +
            slideInVertically(riseSpring) { (it * RiseFraction).toInt() }

    /** The page going out under the new one: dims but stays drawn, so there is never an empty frame. */
    fun exitUnder(): ExitTransition = fadeOut(tween(FadeMillis, easing = FastOutSlowInEasing), targetAlpha = 0.4f)

    /** The page going out on top (a pop back to a tab): blurs and fades away over the page beneath. */
    fun exitOver(): ExitTransition = fadeOut(tween(FadeMillis, easing = FastOutSlowInEasing))

    /** The page coming back underneath a pop: already partly there, it brightens and rises. */
    fun enterUnder(): EnterTransition =
        fadeIn(tween(FadeMillis, easing = FastOutSlowInEasing), initialAlpha = 0.4f) +
            slideInVertically(riseSpring) { (it * RiseFraction).toInt() }

    // About 16 dp on a phone-height page.
    private const val RiseFraction = 0.02f
}

/**
 * Depth blur for a tab page while it enters or leaves: sharp when settled, [TabTransitions.MaxBlurDp]
 * blurred before it arrives and after it leaves. API 31+ only (RenderEffect); older devices just fade.
 */
@Composable
internal fun Modifier.tabDepthBlur(scope: AnimatedVisibilityScope): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    val density = LocalDensity.current
    val blurDp by scope.transition.animateFloat(
        transitionSpec = { spring(dampingRatio = 1f, stiffness = 520f) },
        label = "tabDepthBlur",
    ) { state -> if (state == EnterExitState.Visible) 0f else TabTransitions.MaxBlurDp }
    return graphicsLayer {
        val px = with(density) { blurDp.dp.toPx() }
        renderEffect = if (px > 0.5f) BlurEffect(px, px, TileMode.Decal) else null
    }
}
