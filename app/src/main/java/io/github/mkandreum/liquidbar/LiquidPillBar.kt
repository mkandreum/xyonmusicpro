package io.github.mkandreum.liquidbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import io.github.mkandreum.liquidbar.glass.GlassBackdrop
import io.github.mkandreum.liquidbar.glass.GlassGlow
import io.github.mkandreum.liquidbar.glass.GlassLook
import io.github.mkandreum.liquidbar.glass.GlassSource
import io.github.mkandreum.liquidbar.glass.glassSource
import io.github.mkandreum.liquidbar.glass.glassSurface
import io.github.mkandreum.liquidbar.glass.rememberGlassBackdrop
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class LiquidPillColors(
    /** Relleno de la cápsula activa en reposo. */
    val accent: Color,
    /** Icono y etiqueta sobre la cápsula activa. */
    val onAccent: Color,
    /** Iconos en reposo. */
    val content: Color,
)

object LiquidPillDefaults {
    val TabHeight: Dp = 48.dp
    val LabelStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    fun colors(
        accent: Color = Color(0xFF0A84FF),
        onAccent: Color = Color.White,
        content: Color = Color.White.copy(alpha = 0.72f),
    ) = LiquidPillColors(accent, onAccent, content)
}

/** Pestaña de la píldora: icono que hace «pop» al activarse y etiqueta que se despliega con muelle. */
@Composable
private fun PillTab(
    item: LiquidBarItem,
    selected: Boolean,
    colors: LiquidPillColors,
    labelStyle: TextStyle,
    interactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(if (selected) colors.onAccent else colors.content, label = "pillTint")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
        }
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val squeeze by animateFloatAsState(if (pressed) 0.95f else 1f, spring(0.6f, 800f), label = "pillPress")
    Row(
        modifier
            .height(LiquidPillDefaults.TabHeight)
            .graphicsLayer { scaleX = squeeze; scaleY = squeeze }
            .then(
                if (interactive) Modifier.clickable(source, null, role = Role.Tab, onClickLabel = item.label, onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val glyph = Modifier.size(24.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        if (item.iconContent != null) Box(glyph) { item.iconContent.invoke(tint) }
        else if (item.icon != null) Icon(item.icon, item.label, tint = tint, modifier = glyph)
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(spring(0.75f, 500f)) + fadeIn(),
            exit = shrinkHorizontally(spring(0.9f, 500f)) + fadeOut()
        ) {
            Text(item.label, style = labelStyle, color = tint, maxLines = 1, modifier = Modifier.padding(start = 7.dp))
        }
    }
}
