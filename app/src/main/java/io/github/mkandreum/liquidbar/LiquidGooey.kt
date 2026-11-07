package io.github.mkandreum.liquidbar

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as PlatformRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/*
 * Efecto «gooey» nativo: la capa se compone fuera de pantalla, se desenfoca y el alfa se endurece con un
 * umbral. Las formas cercanas funden su alfa antes del umbral y se unen por un cuello que se afina y se
 * corta, en vez de un fundido plano. En reposo (radio 0) no cuesta nada: ni capa ni filtro.
 */
private const val ThresholdSlope = 10f
private const val ThresholdMidpoint = 0.5f * 255f

private val thresholdFilter by lazy(LazyThreadSafetyMode.NONE) {
    ColorMatrixColorFilter(
        ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, ThresholdSlope, -ThresholdSlope * ThresholdMidpoint,
            )
        )
    )
}

// Un efecto por radio entero: una transición reutiliza los mismos en vez de crear uno por fotograma.
private val gooeyCache = HashMap<Int, RenderEffect>()

private fun gooeyEffect(radiusPx: Float): RenderEffect = gooeyCache.getOrPut(radiusPx.toInt().coerceAtLeast(1)) {
    val r = radiusPx.toInt().coerceAtLeast(1).toFloat()
    val blur = PlatformRenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL)
    PlatformRenderEffect.createColorFilterEffect(thresholdFilter, blur).asComposeRenderEffect()
}

internal val GooeyPeakBlur = 12.dp

internal fun Modifier.gooey(radiusPx: Density.() -> Float): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return this
    return graphicsLayer {
        val radius = radiusPx()
        if (radius >= 1f) {
            compositingStrategy = CompositingStrategy.Offscreen
            renderEffect = gooeyEffect(radius)
        } else {
            compositingStrategy = CompositingStrategy.Auto
            renderEffect = null
        }
    }
}
