package io.github.mkandreum.liquidbar.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.glass.GlassSource
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.glassSurface
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/* ═════════════ 14 · Barra curva ═════════════ */

@Immutable
data class LiquidArcSliderColors(val fill: Color, val track: Color, val label: Color, val bubble: Color, val onBubble: Color)

object LiquidArcSliderDefaults {
    fun colors(
        fill: Color = Color.White.copy(alpha = 0.62f),
        track: Color = Color.White.copy(alpha = 0.08f),
        label: Color = Color.White.copy(alpha = 0.7f),
        bubble: Color = Color.White,
        onBubble: Color = Color.Black,
    ) = LiquidArcSliderColors(fill, track, label, bubble, onBubble)
}

/**
 * Barra curva de cristal: un cuarto de círculo que se rellena desde el extremo de abajo (junto a lo que abraza)
 * hasta el de arriba. [mirrored] = false es la barra izquierda «╮» (arriba a la izquierda → abajo a la derecha);
 * true, la derecha «╭» (abajo a la izquierda → arriba a la derecha). Juntas abrazan un botón: «╮ ▶ ╭».
 *
 * Gestos: al tocarla el tubo engorda y la bolita se vuelve una lente que amplía ×1,6; mantenida 350 ms vibra.
 * Arrastrando a lo largo del arco el valor sigue al dedo y vibra en cada tramo ([segments]), más fuerte en los
 * extremos. Un toque suelto no cambia el valor. [topLabel]/[bottomLabel] van junto a cada extremo.
 */
@Composable
fun LiquidArcSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    segments: Int = 10,
    onValueChangeFinished: (() -> Unit)? = null,
    topLabel: String? = null,
    bottomLabel: String? = null,
    format: (Float) -> String = { "${(it * 100).roundToInt()}" },
    colors: LiquidArcSliderColors = LiquidArcSliderDefaults.colors(),
    thickness: Dp = 18.dp,
    sweepDegrees: Float = 90f,
    label: String? = null,
    /** Contenido dentro de la curva (en el lado cóncavo), p. ej. el botón de canción anterior/siguiente. */
    inner: (@Composable () -> Unit)? = null,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val latest by rememberUpdatedState(value)
    var dragging by remember { mutableStateOf(false) }
    var dragFrac by remember { mutableFloatStateOf(0f) }
    val frac = if (dragging) dragFrac else ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val press = remember { Animatable(0f, 0.001f) }
    val trackBackdrop = rememberGlassBackdrop()
    val trackOnly = remember(trackBackdrop) { listOf(GlassSource(trackBackdrop)) }

    fun tick(strong: Boolean) {
        val c = when {
            strong && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
            strong -> HapticFeedbackConstants.LONG_PRESS
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(c)
    }

    BoxWithConstraints(
        modifier.semantics {
            label?.let { contentDescription = it }
            stateDescription = format(value)
            setProgress { t -> change(t.coerceIn(valueRange)); true }
        }
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // Margen lateral justo para la bolita; margen vertical con sitio para las etiquetas.
        val padH = with(density) { (thickness * 0.85f).toPx() }
        val padV = with(density) { (thickness * 0.85f + 22.dp).toPx() }
        val pad = padH
        // Ángulos en grados de pantalla (0 = derecha, -90 = arriba). Valor 0 en el extremo de abajo.
        val startDeg = if (!mirrored) 0f else 180f
        val endDeg = if (!mirrored) -sweepDegrees else 180f + sweepDegrees
        val r = minOf(w - 2 * padH, h - 2 * padV).coerceAtLeast(1f)
        // Centro del círculo: abajo a la izquierda («╮») o abajo a la derecha («╭»).
        val cx = if (!mirrored) pad else w - pad
        val cy = (h + r) / 2f
        val toRad = PI.toFloat() / 180f
        fun angleAt(t: Float) = (startDeg + (endDeg - startDeg) * t) * toRad
        fun pointAt(t: Float): Offset { val a = angleAt(t); return Offset(cx + r * cos(a), cy + r * sin(a)) }
        fun fracAt(p: Offset): Float {
            var a = atan2(p.y - cy, p.x - cx) / toRad
            return if (!mirrored) {
                if (a >= 180f) a -= 360f
                ((startDeg - a) / sweepDegrees).coerceIn(0f, 1f)
            } else {
                if (a < 0f) a += 360f
                ((a - startDeg) / sweepDegrees).coerceIn(0f, 1f)
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(mirrored, segments, w, h) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        // Only the band around the curve belongs to the slider: a tap on what sits
                        // inside the curve (the skip button) must not wake the lens.
                        val band = thickness.toPx() * 1.6f
                        if (kotlin.math.abs((down.position - Offset(cx, cy)).getDistance() - r) > band) return@awaitEachGesture
                        scope.launch { press.animateTo(1f, LiquidSprings.press) }
                        var held = false
                        var moved = false
                        var lastSeg = floor(frac * segments).toInt()
                        val startAt = down.uptimeMillis
                        try {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if (!held && ch.uptimeMillis - startAt >= 350) { held = true; tick(strong = true) }
                                if (!moved && (ch.position - down.position).getDistance() > viewConfiguration.touchSlop) { moved = true; dragging = true }
                                if (moved) {
                                    ch.consume()
                                    val f = fracAt(ch.position)
                                    dragFrac = f
                                    val seg = floor(f * segments).toInt().coerceIn(0, segments)
                                    if (seg != lastSeg) { tick(strong = f <= 0f || f >= 1f); lastSeg = seg }
                                    val v = valueRange.start + f * span
                                    if (v != latest) change(v)
                                }
                            }
                        } finally {
                            if (moved) finished?.invoke()
                            dragging = false
                            scope.launch { press.animateTo(0f, LiquidSprings.release) }
                        }
                    }
                }
        ) {
            // Tubo de cristal y relleno: capa propia que la lente amplía.
            Canvas(Modifier.fillMaxSize().glassSource(trackBackdrop)) {
                val stroke = thickness.toPx() * (1f + 0.3f * press.value)
                val tl = Offset(cx - r, cy - r)
                val sz = Size(2 * r, 2 * r)
                val sweep = endDeg - startDeg
                drawArc(colors.track, startDeg, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
                val edge = stroke / 2f - 0.75.dp.toPx()
                drawArc(Color.White.copy(alpha = 0.38f), startDeg, sweep, false, Offset(tl.x - edge, tl.y - edge), Size(sz.width + 2 * edge, sz.height + 2 * edge), style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
                drawArc(Color.White.copy(alpha = 0.16f), startDeg, sweep, false, Offset(tl.x + edge, tl.y + edge), Size(sz.width - 2 * edge, sz.height - 2 * edge), style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
                if (frac > 0.001f) drawArc(colors.fill, startDeg, sweep * frac, false, tl, sz, style = Stroke(stroke * 0.6f, cap = StrokeCap.Round))
            }
            // Bolita de cristal sobre el arco; al pulsar, lente.
            Box(
                Modifier
                    .layout { m, c ->
                        val p = press.value.coerceIn(0f, 1f)
                        val s = (thickness.toPx() * lerp(1.35f, 3.2f, p)).roundToInt().coerceAtLeast(1)
                        val pl = m.measure(androidx.compose.ui.unit.Constraints.fixed(s, s))
                        val pt = pointAt(frac)
                        layout(c.maxWidth, c.maxHeight) { pl.place((pt.x - s / 2f).roundToInt(), (pt.y - s / 2f).roundToInt()) }
                    }
                    .glassSurface({ trackOnly }, CircleShape) {
                        val p = press.value.coerceIn(0f, 1f)
                        lensLook(p, Color.White.copy(alpha = 0.34f - 0.24f * p), zoom = 1.6f, band = 9.dp, amount = 11.dp, saturation = 1.4f, restShadow = 0.5f, fallback = Color.White.copy(alpha = 0.5f))
                    }
            )
            // Lo que va dentro de la curva, en el lado cóncavo.
            if (inner != null) {
                val mid = angleAt(0.5f)
                val at = Offset(cx + cos(mid) * r * 0.36f, cy + sin(mid) * r * 0.36f)
                Box(Modifier.layout { m, c ->
                    val pl = m.measure(androidx.compose.ui.unit.Constraints())
                    layout(c.maxWidth, c.maxHeight) { pl.place((at.x - pl.width / 2f).roundToInt(), (at.y - pl.height / 2f).roundToInt()) }
                }) { inner() }
            }
            // Etiquetas separadas de cada extremo (encima del de arriba, debajo del de abajo).
            val top = pointAt(1f)
            val bottom = pointAt(0f)
            val clear = with(density) { (thickness * 0.75f + 6.dp).toPx() }
            topLabel?.let { ArcLabel(it, colors.label, Offset(top.x, top.y - clear), alignEnd = true) }
            bottomLabel?.let { ArcLabel(it, colors.label, Offset(bottom.x, bottom.y + clear), alignEnd = false) }
            // Burbuja con el valor encima de la bolita mientras se arrastra.
            Text(
                format(valueRange.start + frac * span), color = colors.onBubble, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier
                    .layout { m, c ->
                        val pl = m.measure(androidx.compose.ui.unit.Constraints())
                        val pt = pointAt(frac)
                        val x = (pt.x - pl.width / 2f).coerceIn(0f, (c.maxWidth - pl.width).toFloat().coerceAtLeast(0f))
                        layout(c.maxWidth, c.maxHeight) {
                            pl.placeWithLayer(x.roundToInt(), (pt.y - pl.height - 34.dp.toPx()).roundToInt()) {
                                val p = press.value.coerceIn(0f, 1f); alpha = p; val s = lerp(0.6f, 1f, p); scaleX = s; scaleY = s
                            }
                        }
                    }
                    .background(colors.bubble, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun ArcLabel(text: String, color: Color, at: Offset, alignEnd: Boolean) {
    Text(
        text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
        modifier = Modifier.layout { m, c ->
            val pl = m.measure(androidx.compose.ui.unit.Constraints())
            val x = (at.x - pl.width / 2f).coerceIn(0f, (c.maxWidth - pl.width).toFloat().coerceAtLeast(0f))
            val y = if (alignEnd) at.y - pl.height else at.y
            layout(c.maxWidth, c.maxHeight) { pl.place(x.roundToInt(), y.roundToInt().coerceIn(0, (c.maxHeight - pl.height).coerceAtLeast(0))) }
        },
    )
}

/* ═════════════ 15 · Play/pausa de cristal ═════════════ */

/** Silueta del play (triángulo) que pasa a la pausa (dos barras) con [morph] 0 → 1. */
private class PlayPauseShape(private val morph: () -> Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline =
        androidx.compose.ui.graphics.Outline.Generic(playPausePath(size.width, size.height, morph().coerceIn(0f, 1f)))
}

private fun playPausePath(w: Float, h: Float, t: Float): Path {
    val bar = w * 0.32f
    return Path().apply {
        moveTo(lerp(w * 0.12f, w * 0.10f, t), 0f); lineTo(lerp(w * 0.56f, w * 0.10f + bar, t), lerp(h * 0.25f, 0f, t))
        lineTo(lerp(w * 0.56f, w * 0.10f + bar, t), lerp(h * 0.75f, h, t)); lineTo(lerp(w * 0.12f, w * 0.10f, t), h); close()
        moveTo(lerp(w * 0.56f, w * 0.90f - bar, t), lerp(h * 0.25f, 0f, t)); lineTo(lerp(w * 0.98f, w * 0.90f, t), lerp(h * 0.5f, 0f, t))
        lineTo(lerp(w * 0.98f, w * 0.90f, t), lerp(h * 0.5f, h, t)); lineTo(lerp(w * 0.56f, w * 0.90f - bar, t), lerp(h * 0.75f, h, t)); close()
    }
}

/**
 * Play/pausa hecho de cristal, sin círculo: el propio triángulo (y las dos barras de la pausa) es una pieza de
 * cristal que amplía y desenfoca lo que hay debajo, con filo de luz. Se transforma con spring(0.6, 500), al pulsar
 * se hunde y crece la lupa, y vibra. [backdrop] es lo que refracta (la capa `glassSource` del reproductor).
 */
@Composable
fun LiquidPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    tint: Color = Color.White.copy(alpha = 0.38f),
) {
    val view = LocalView.current
    val morph = remember { Animatable(if (isPlaying) 1f else 0f) }
    LaunchedEffect(isPlaying) { morph.animateTo(if (isPlaying) 1f else 0f, LiquidSprings.toggle) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, LiquidSprings.press, label = "playPress")
    val shape = remember { PlayPauseShape { morph.value } }
    val sources = remember(backdrop) { listOf(GlassSource(backdrop)) }
    Box(
        modifier
            .size(size)
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClickLabel = if (isPlaying) "Pausa" else "Reproducir") {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size * 0.62f)
                .graphicsLayer { val s = 1f - 0.08f * press; scaleX = s; scaleY = s }
                .glassSurface({ sources }, shape) {
                    io.github.mkandreum.liquidbar.glass.GlassLook(
                        blur = 2.dp.toPx(),
                        saturation = 1.6f,
                        refractionHeight = 0f,
                        refractionAmount = 0f,
                        zoom = 1.25f + 0.35f * press,
                        surface = tint,
                        rim = Color.White.copy(alpha = 0.9f),
                        rimWidth = 2.dp.toPx(),
                        darkRimAlpha = 0.25f,
                        darkRimWidth = 1.dp.toPx(),
                        shadowAlpha = 0.55f,
                        fallback = Color.White.copy(alpha = 0.85f),
                    )
                }
        )
    }
}

/** Silueta de «siguiente» (dos triángulos); [next] = false la refleja para «anterior». */
private class SkipShape(private val next: Boolean) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val w = size.width; val h = size.height
        fun x(v: Float) = if (next) v else w - v
        val p = Path().apply {
            moveTo(x(0f), h * 0.1f); lineTo(x(w * 0.52f), h * 0.5f); lineTo(x(0f), h * 0.9f); close()
            moveTo(x(w * 0.48f), h * 0.1f); lineTo(x(w), h * 0.5f); lineTo(x(w * 0.48f), h * 0.9f); close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(p)
    }
}

/** Cristal con forma de glifo (sin círculo): amplía y desenfoca lo de debajo, con filo de luz; se hunde al pulsar. */
@Composable
private fun GlassGlyph(shape: androidx.compose.ui.graphics.Shape, backdrop: GlassBackdrop, size: Dp, glyphFraction: Float, tint: Color, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, LiquidSprings.press, label = "glyphPress")
    val sources = remember(backdrop) { listOf(GlassSource(backdrop)) }
    Box(
        modifier.size(size).clickable(interactionSource = src, indication = null, role = Role.Button, onClickLabel = label) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onClick()
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size * glyphFraction)
                .graphicsLayer { val s = 1f - 0.1f * press; scaleX = s; scaleY = s }
                .glassSurface({ sources }, shape) {
                    io.github.mkandreum.liquidbar.glass.GlassLook(
                        blur = 2.dp.toPx(), saturation = 1.6f, zoom = 1.25f + 0.35f * press,
                        surface = tint, rim = Color.White.copy(alpha = 0.9f), rimWidth = 2.dp.toPx(),
                        darkRimAlpha = 0.25f, darkRimWidth = 1.dp.toPx(), shadowAlpha = 0.55f,
                        fallback = Color.White.copy(alpha = 0.85f),
                    )
                }
        )
    }
}

/** Anterior / siguiente de cristal, con la misma forma de glifo que el play (sin círculo). */
@Composable
fun LiquidSkipButton(
    next: Boolean,
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = Color.White.copy(alpha = 0.38f),
) {
    val shape = remember(next) { SkipShape(next) }
    GlassGlyph(shape, backdrop, size, 0.62f, tint, if (next) "Siguiente" else "Anterior", onClick, modifier)
}

/* ═════════════ 16 · Mini reproductor ═════════════ */

/**
 * Mini reproductor de cristal para [io.github.mkandreum.liquidbar.LiquidMediaBar] (o suelto): cápsula con
 * carátula, título/artista, play/pausa y siguiente cuando está abierto; tira fina con play/pausa cuando la barra
 * se pliega. Las dos versiones se funden con [inline]/[expanded] (las opacidades que da la barra) y solo la visible
 * recibe toques. Deslizar en horizontal llama a [onSkip] (true = siguiente); al pulsar crece un 4 %.
 */
@Composable
fun LiquidMiniPlayer(
    backdrop: GlassBackdrop,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: @Composable (Modifier) -> Unit = {},
    onNext: (() -> Unit)? = null,
    onSkip: ((next: Boolean) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    inline: () -> Float = { 0f },
    expanded: () -> Float = { 1f },
    glass: LiquidGlassStyle = LiquidGlassStyle.Dark,
    contentColor: Color = Color.White,
    /** Glifo propio de play/pausa (tamaño en dp); por defecto [PlayPauseGlyph]. */
    playGlyph: @Composable (isPlaying: Boolean, size: Dp) -> Unit = { p, s -> PlayPauseGlyph(p, contentColor, s) },
    /** Glifo propio de «siguiente»; por defecto [NextGlyph]. */
    nextGlyph: @Composable (size: Dp) -> Unit = { s -> NextGlyph(contentColor, s) },
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val settle = remember { spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow) }
    val press = remember { MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 1.04f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "miniPress")
    val isInline by remember { derivedStateOf { inline() > 0.5f } }
    val haptic: (() -> Unit) -> () -> Unit = { f -> { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); f() } }

    BoxWithConstraints(
        modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clip(CircleShape)
            .liquidGlass(backdrop, CircleShape, glass)
            .clipToBounds()
            .then(
                if (onSkip == null) Modifier else Modifier.pointerInput(Unit) {
                    val threshold = size.width * 0.28f
                    detectHorizontalDragGestures(
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, settle) } },
                        onDragEnd = {
                            val o = offsetX.value
                            if (abs(o) > threshold) { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onSkip(o < 0) }
                            scope.launch { offsetX.animateTo(0f, settle) }
                        },
                    ) { ch, d -> ch.consume(); scope.launch { offsetX.snapTo(offsetX.value + d) } }
                }
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val off = { offsetX.value.roundToInt() }
        val open = haptic(onOpen)
        val play = haptic(onPlayPause)
        val next = onNext?.let { haptic(it) }
        @Composable fun rowMod(interactive: Boolean, alpha: () -> Float) = Modifier
            .fillMaxSize()
            .offset { IntOffset(off(), 0) }
            .graphicsLayer { this.alpha = alpha() }
            .then(
                if (interactive) Modifier.combinedClickable(interactionSource = press, indication = null, onClick = open, onLongClick = onLongPress)
                else Modifier.clearAndSetSemantics {}
            )

        @Composable fun InlineRow(interactive: Boolean) = Row(
            rowMod(interactive, inline).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artwork(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = contentColor, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = contentColor.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MiniGlyphButton(interactive, play) { playGlyph(isPlaying, 20.dp) }
        }

        @Composable fun ExpandedRow(interactive: Boolean) = Row(
            rowMod(interactive, expanded).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            artwork(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = contentColor, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = contentColor.copy(alpha = 0.72f), fontSize = 13.5.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            MiniGlyphButton(interactive, play) { playGlyph(isPlaying, 22.dp) }
            if (next != null) MiniGlyphButton(interactive, next) { nextGlyph(22.dp) }
        }

        // Solo la versión visible recibe toques y se compone la última, encima de la otra.
        if (isInline) { ExpandedRow(false); InlineRow(true) } else { InlineRow(false); ExpandedRow(true) }
    }
}

@Composable
private fun MiniGlyphButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.85f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "miniGlyph")
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = s; scaleY = s }
            .clip(CircleShape)
            .clickable(enabled = enabled, interactionSource = src, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Glifo play/pausa que se transforma con muelle; sirve para mini reproductores y listas. */
@Composable
fun PlayPauseGlyph(isPlaying: Boolean, color: Color, size: Dp) {
    val m by animateFloatAsState(if (isPlaying) 1f else 0f, LiquidSprings.toggle, label = "glyphMorph")
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height; val t = m.coerceIn(0f, 1f); val bar = w * 0.32f
        drawPath(Path().apply {
            moveTo(lerp(w * 0.12f, w * 0.10f, t), 0f); lineTo(lerp(w * 0.56f, w * 0.10f + bar, t), lerp(h * 0.25f, 0f, t))
            lineTo(lerp(w * 0.56f, w * 0.10f + bar, t), lerp(h * 0.75f, h, t)); lineTo(lerp(w * 0.12f, w * 0.10f, t), h); close()
        }, color)
        drawPath(Path().apply {
            moveTo(lerp(w * 0.56f, w * 0.90f - bar, t), lerp(h * 0.25f, 0f, t)); lineTo(lerp(w * 0.98f, w * 0.90f, t), lerp(h * 0.5f, 0f, t))
            lineTo(lerp(w * 0.98f, w * 0.90f, t), lerp(h * 0.5f, h, t)); lineTo(lerp(w * 0.56f, w * 0.90f - bar, t), lerp(h * 0.75f, h, t)); close()
        }, color)
    }
}

/** Glifo «siguiente» (dos triángulos). */
@Composable
fun NextGlyph(color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        drawPath(Path().apply { moveTo(0f, h * 0.12f); lineTo(w * 0.5f, h * 0.5f); lineTo(0f, h * 0.88f); close() }, color)
        drawPath(Path().apply { moveTo(w * 0.5f, h * 0.12f); lineTo(w, h * 0.5f); lineTo(w * 0.5f, h * 0.88f); close() }, color)
    }
}
