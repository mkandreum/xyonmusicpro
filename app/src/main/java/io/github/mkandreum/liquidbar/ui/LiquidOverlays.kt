package io.github.mkandreum.liquidbar.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.mkandreum.liquidbar.LiquidGlassStyle
import io.github.mkandreum.liquidbar.elasticStretch
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.glass.GlassGlow
import io.github.mkandreum.liquidbar.glass.GlassLook
import io.github.mkandreum.liquidbar.glass.GlassSource
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.glassSurface
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import io.github.mkandreum.liquidbar.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

/* ═════════════ 7 · Isla líquida ═════════════ */

@Immutable
data class LiquidIslandMessage(
    val title: String,
    val text: String? = null,
    val icon: ImageVector? = null,
    val accent: Color = Color(0xFF30D158),
    /** 0 = se queda hasta [LiquidIslandState.hide]. */
    val durationMillis: Long = 3200,
    val onTap: (() -> Unit)? = null,
    val id: Long = System.nanoTime(),
)

@Stable
class LiquidIslandState {
    var current by mutableStateOf<LiquidIslandMessage?>(null)
        private set
    fun show(message: LiquidIslandMessage) { current = message }
    fun show(title: String, text: String? = null, icon: ImageVector? = null, accent: Color = Color(0xFF30D158)) =
        show(LiquidIslandMessage(title, text, icon, accent))
    fun hide() { current = null }
}

/** Isla de la app, para mostrar avisos desde cualquier pantalla (la da la raíz). */
val LocalLiquidIsland = androidx.compose.runtime.staticCompositionLocalOf<LiquidIslandState?> { null }

/* ═════════════ 8 · Abanico de burbujas ═════════════ */

@Immutable
data class LiquidFanItem(val label: String, val icon: ImageVector, val color: Color = Color.Unspecified, val onClick: () -> Unit)

/* ═════════════ 9 · Lupa para gráficas ═════════════ */

@Immutable
data class LiquidChartPoint(val value: String, val label: String? = null, val y: Float? = null, val key: Any? = null)

/* ═════════════ 10 · Buscar que se transforma ═════════════ */

/* ═════════════ 11 · Menú de pulsación larga ═════════════ */

@Immutable
data class LiquidMenuItem(val label: String, val icon: ImageVector? = null, val destructive: Boolean = false, val onClick: () -> Unit)

internal class MenuRequest(val bounds: Rect, val items: List<LiquidMenuItem>, val content: @Composable () -> Unit, val key: Any)

@Stable
class LiquidMenuState {
    internal var request by mutableStateOf<MenuRequest?>(null)
    val isOpen: Boolean get() = request != null
    fun dismiss() { request = null }
}

/** Menú de pulsación larga de la app (lo da la raíz junto a su [LiquidMenuHost]). */
val LocalLiquidMenuState = androidx.compose.runtime.staticCompositionLocalOf<LiquidMenuState?> { null }

/**
 * Capa del menú: va en el Box raíz, encima de todo y hermana del contenido con `glassSource(backdrop)`.
 * [cardShape]: forma de las tarjetas (para la sombra al elevarlas).
 */
@Composable
fun LiquidMenuHost(
    state: LiquidMenuState,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    glass: LiquidGlassStyle = LiquidGlassStyle.Dark,
    contentColor: Color = Color.White,
    destructiveColor: Color = Color(0xFFFF6B60),
    cardShape: Shape = RoundedCornerShape(20.dp),
) {
    var shown by remember { mutableStateOf<MenuRequest?>(null) }
    val lift = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    val pills = remember { List(8) { Animatable(0f) } }
    val req = state.request
    LaunchedEffect(req) {
        if (req != null) {
            shown = req
            pills.forEach { it.snapTo(0f) }
            launch { lift.animateTo(1f, spring(0.55f, 420f)) }
            launch { fade.animateTo(1f, spring(1f, 400f)) }
            req.items.indices.take(8).forEach { i -> launch { delay(40L + i * 35L); pills[i].animateTo(1f, spring(0.6f, 420f)) } }
        } else if (shown != null) {
            pills.forEach { launch { it.animateTo(0f, spring(0.9f, 600f)) } }
            launch { fade.animateTo(0f, spring(1f, 500f)) }
            lift.animateTo(0f, spring(0.8f, 420f))
            shown = null
        }
    }
    val s = shown ?: return
    val density = LocalDensity.current
    val screen = remember(backdrop) { listOf(GlassSource(backdrop)) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Fondo: la pantalla desenfocada y oscurecida.
        Box(
            Modifier.fillMaxSize().graphicsLayer { alpha = fade.value.coerceIn(0f, 1f) }
                .glassSurface({ screen }, RectangleShape) {
                    GlassLook(blur = 10.dp.toPx(), saturation = 1.2f, surface = Color.Black.copy(alpha = 0.28f), fallback = Color.Black.copy(alpha = 0.6f))
                }
                .pointerInput(Unit) { detectTapGestures { state.dismiss() } }
        )
        val b = s.bounds
        Box(
            Modifier.offset { IntOffset(b.left.roundToInt(), b.top.roundToInt()) }
                .size(with(density) { b.width.toDp() }, with(density) { b.height.toDp() })
                .graphicsLayer {
                    val l = lift.value
                    val sc = lerp(1f, 1.03f, l); scaleX = sc; scaleY = sc
                    shadowElevation = 18.dp.toPx() * l.coerceIn(0f, 1f)
                    shape = cardShape
                    clip = false
                }
                .pointerInput(Unit) { detectTapGestures { state.dismiss() } }
        ) { s.content() }
        val count = s.items.size.coerceAtMost(8)
        val colH = with(density) { (44.dp * count + 8.dp * (count - 1).coerceAtLeast(0)).toPx() }
        val maxH = constraints.maxHeight
        val below = b.bottom + with(density) { 12.dp.toPx() } + colH < maxH
        val maxWpx = constraints.maxWidth
        Column(
            Modifier.offset {
                val x = b.left.coerceIn(12.dp.toPx(), maxWpx - 232.dp.toPx())
                val yy = if (below) b.bottom + 12.dp.toPx() else b.top - 12.dp.toPx() - colH
                IntOffset(x.roundToInt(), yy.roundToInt())
            }.width(220.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            s.items.take(8).forEachIndexed { i, item ->
                val color = if (item.destructive) destructiveColor else contentColor
                Row(
                    Modifier.fillMaxWidth().height(44.dp)
                        .graphicsLayer {
                            val tt = pills[i].value
                            alpha = tt.coerceIn(0f, 1f)
                            translationY = (if (below) -10.dp else 10.dp).toPx() * (1f - tt)
                            val sc = lerp(0.8f, 1f, tt); scaleX = sc; scaleY = sc
                            transformOrigin = TransformOrigin(0.3f, 0.5f)
                        }
                        .liquidGlass(backdrop, CircleShape, glass)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { state.dismiss(); item.onClick() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (item.icon != null) Icon(item.icon, null, tint = color, modifier = Modifier.size(20.dp))
                    Text(item.label, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/* ═════════════ 12 · Tirar para recargar con gota ═════════════ */

/* ═════════════ 13 · Hoja inferior de cristal ═════════════ */

@Stable
class LiquidSheetState(initial: Int = -1) {
    /** Altura actual (índice en `detents`) o −1 cerrada. */
    var detent by mutableIntStateOf(initial)
        internal set
    val isOpen: Boolean get() = detent >= 0
    fun open(detent: Int = 0) { this.detent = detent }
    fun close() { detent = -1 }
}

@Composable
fun rememberLiquidSheetState() = remember { LiquidSheetState() }

/**
 * Hoja inferior propia (no `ModalBottomSheet`, que vive en otra ventana y no ve la capa): cristal con canto
 * que refracta, asa que se estira con la velocidad y alturas fijas [detents] (fracción de pantalla) con
 * spring(0.75, 300). Bajarla del todo la cierra. Va en el Box raíz, hermana del contenido.
 * El contenido gestiona su propio scroll (p. ej. `verticalScroll`); al tirar hacia abajo arriba del todo, baja la hoja.
 */
@Composable
fun LiquidSheet(
    state: LiquidSheetState,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    detents: List<Float> = listOf(0.45f, 0.9f),
    title: String? = null,
    glass: LiquidGlassStyle = LiquidGlassStyle.Dark.copy(blur = 12.dp),
    contentColor: Color = Color.White,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val y = remember { Animatable(1f, 0.0005f) }
    var visible by remember { mutableStateOf(false) }
    val dismiss by rememberUpdatedState(onDismiss)
    val sorted = remember(detents) { detents.sorted() }
    LaunchedEffect(state.detent) {
        if (state.detent >= 0) {
            visible = true
            y.animateTo(1f - sorted[state.detent.coerceIn(sorted.indices)], LiquidSprings.sheet)
        } else if (visible) {
            y.animateTo(1f, spring(0.9f, 320f))
            visible = false
            dismiss()
        }
    }
    if (!visible && state.detent < 0) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        val H = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val maxD = sorted.last()
        val top = 1f - maxD
        val sheetH = with(density) { (H * maxD).toDp() + 60.dp }
        fun settle(v: Float) {
            val cur = 1f - y.value
            var best = 0
            var bd = Float.MAX_VALUE
            sorted.forEachIndexed { i, d -> val dd = abs(d - cur - v / H * 0.25f); if (dd < bd) { bd = dd; best = i } }
            if (cur < sorted.first() * 0.6f || (v > 1800f && best == 0 && cur < sorted.first())) state.close()
            else if (state.detent == best) scope.launch { y.animateTo(1f - sorted[best], LiquidSprings.sheet) }
            else state.detent = best
        }
        fun move(dy: Float) {
            var ny = y.value + dy / H
            if (ny < top) ny = top - (top - ny) * 0.3f
            scope.launch { y.snapTo(ny) }
        }
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = 0.35f * ((1f - y.value) / maxD).coerceIn(0f, 1f) }
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { state.close() } }
        )
        val bodyConnection = remember(H) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // Subir con la hoja a media altura: primero sube la hoja.
                    if (available.y < 0f && y.value > top + 0.001f && source == NestedScrollSource.UserInput) { move(available.y); return Offset(0f, available.y) }
                    return Offset.Zero
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f && source == NestedScrollSource.UserInput) { move(available.y); return Offset(0f, available.y) }
                    return Offset.Zero
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    val atDetent = sorted.any { abs((1f - it) - y.value) < 0.002f }
                    if (atDetent) return Velocity.Zero
                    settle(available.y)
                    return available
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 8.dp).fillMaxWidth().height(sheetH)
                .offset { IntOffset(0, (y.value * H).roundToInt()) }
                .liquidGlass(backdrop, RoundedCornerShape(28.dp), glass)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .draggable(rememberDraggableState { dy -> move(dy) }, Orientation.Vertical, onDragStopped = { v -> settle(v) })
                    .padding(top = 8.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.layout { me, _ ->
                        val v = (abs(y.velocity) * 0.6f).coerceIn(0f, 1f)
                        val w = (36.dp.toPx() + 18.dp.toPx() * v).roundToInt()
                        val child = me.measure(Constraints.fixed(w, 5.dp.roundToPx()))
                        layout(w, child.height) { child.place(0, 0) }
                    }.background(Color.White.copy(alpha = 0.4f), CircleShape)
                )
                if (title != null) Text(title, color = contentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            }
            Column(
                Modifier.fillMaxWidth()
                    .layout { me, c ->
                        val vis = ((1f - y.value) * H - 56.dp.toPx()).roundToInt().coerceIn(0, c.maxHeight)
                        val child = me.measure(Constraints(minWidth = c.maxWidth, maxWidth = c.maxWidth, minHeight = vis, maxHeight = vis))
                        layout(c.maxWidth, vis) { child.place(0, 0) }
                    }
                    .nestedScroll(bodyConnection)
                    .padding(horizontal = 20.dp),
                content = content,
            )
        }
    }
}

/* ── Portal de hojas: declara la hoja donde quieras (dentro de una pantalla) y se pinta en la raíz ── */

internal class PortalSheet(
    val title: String?,
    val detents: List<Float>,
    val onDismiss: () -> Unit,
    val content: @Composable ColumnScope.() -> Unit,
    val key: Any,
)

/** Dónde se pintan las [LiquidPortalSheet] de toda la app. Créalo en la raíz y dáselo con [LocalLiquidSheetPortal]. */
@Stable
class LiquidSheetPortal {
    internal var entry by mutableStateOf<PortalSheet?>(null)
    val isOpen: Boolean get() = entry != null
}

val LocalLiquidSheetPortal = androidx.compose.runtime.staticCompositionLocalOf<LiquidSheetPortal?> { null }

/**
 * Hoja de cristal declarativa, sustituta directa de `ModalBottomSheet`: mientras esté en composición se muestra en
 * el [LiquidSheetPortalHost] de la raíz (donde sí puede refractar la pantalla); al salir de la composición se cierra.
 * Bajarla del todo llama a [onDismiss].
 */
@Composable
fun LiquidPortalSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    detents: List<Float> = listOf(0.9f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val portal = LocalLiquidSheetPortal.current ?: error("LiquidPortalSheet necesita LiquidSheetPortalHost en la raíz (LocalLiquidSheetPortal)")
    val key = remember { Any() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val latestContent by rememberUpdatedState(content)
    androidx.compose.runtime.DisposableEffect(portal) {
        portal.entry = PortalSheet(title, detents, { latestDismiss() }, { latestContent() }, key)
        onDispose { if (portal.entry?.key === key) portal.entry = null }
    }
}

/** Pinta las hojas del portal. Va en el Box raíz, encima del contenido y hermano del `glassSource(backdrop)`. */
@Composable
fun LiquidSheetPortalHost(
    portal: LiquidSheetPortal,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    glass: LiquidGlassStyle = LiquidGlassStyle.Dark.copy(blur = 12.dp),
    contentColor: Color = Color.White,
) {
    val state = rememberLiquidSheetState()
    var shown by remember { mutableStateOf<PortalSheet?>(null) }
    val e = portal.entry
    LaunchedEffect(e) {
        if (e != null) { shown = e; state.open(0) } else state.close()
    }
    val s = shown ?: return
    LiquidSheet(
        state, backdrop, modifier, detents = s.detents, title = s.title, glass = glass, contentColor = contentColor,
        onDismiss = {
            // Cerrada con el dedo: avisa a la app (que la quitará de la composición). Cerrada por la app: ya no está.
            if (portal.entry?.key === s.key) s.onDismiss()
            if (portal.entry == null) shown = null
        },
    ) { s.content(this) }
}

