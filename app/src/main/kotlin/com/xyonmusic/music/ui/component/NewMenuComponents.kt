/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.component

import com.xyonmusic.music.ui.utils.MenuItemChrome
import com.xyonmusic.music.ui.utils.MenuIconChrome
import com.xyonmusic.music.ui.utils.additiveChrome
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import com.xyonmusic.music.ui.component.shapes.ContinuousCapsule
import com.xyonmusic.music.ui.component.shapes.ContinuousRoundedRectangle
import com.xyonmusic.music.ui.ios.component.IosText
import com.xyonmusic.music.ui.ios.foundation.IosMetrics
import com.xyonmusic.music.ui.ios.foundation.IosTheme
import com.xyonmusic.music.ui.ios.foundation.LocalIosContentColor

/**
 * The action strip at the top of a menu sheet, drawn as SwiftUI's ControlGroup does it:
 * ONE capsule split into equal segments by hairlines, not a row of separate cards.
 *
 * A phone sheet is only about 320dp inside its inset. Cells narrower than
 * [MenuActionMinCellWidth] crush the labels ("Add S…"). Wider cells wrap onto another
 * row instead of packing every icon together.
 */
@Composable
fun NewActionGrid(
    actions: List<NewAction>,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    if (actions.isEmpty()) return
    val colors = IosTheme.colors
    // Inside the menu panel the quick actions sit bare on the panel (iOS 27 clip).
    val inPanel = LocalInMenuPanel.current
    // The panel drops the hairline, so the gap itself has to separate the icons.
    val gap = if (inPanel) 12.dp else 0.dp

    if (inPanel) {
        PanelActions(actions, modifier)
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = menuActionColumnCount(maxWidth, MenuActionMinCellWidth, gap, columns)
        Column(verticalArrangement = Arrangement.spacedBy(if (inPanel) 12.dp else 8.dp)) {
            actions.chunked(columnCount).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().then(
                        if (inPanel) Modifier else Modifier.clip(ContinuousCapsule())
                            .background(colors.secondarySystemGroupedBackground),
                    ),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEachIndexed { index, action ->
                        if (index > 0 && !inPanel) {
                            Box(Modifier.width(IosMetrics.SeparatorThickness).height(36.dp).background(colors.separator))
                        }
                        ActionSegment(action, Modifier.weight(1f))
                    }
                    repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** How many quick actions the floating panel shows across its top, as an iOS context menu does. */
private const val PanelQuickActions = 4

/**
 * Quick actions inside the floating menu panel: at most [PanelQuickActions] in ONE row of small
 * icons with a one-line caption; anything beyond that becomes an ordinary one-line row under it.
 * Three rows of big buttons with clipped two-line captions made the menu taller than the screen.
 */
@Composable
private fun PanelActions(actions: List<NewAction>, modifier: Modifier) {
    val quick = actions.take(PanelQuickActions)
    val rest = actions.drop(PanelQuickActions)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            quick.forEach { action -> ActionSegment(action, Modifier.weight(1f), compact = true) }
            repeat(PanelQuickActions - quick.size) { Spacer(Modifier.weight(1f)) }
        }
        rest.forEach { action -> PanelActionRow(action) }
    }
}

/** An overflow quick action drawn as a one-line menu row (icon, label). */
@Composable
private fun PanelActionRow(action: NewAction) {
    val contentColor = if (action.contentColor.isSpecified) action.contentColor
        else com.xyonmusic.music.ui.ios.foundation.IosMenuMaterial.colors.label
    var performAction by remember { mutableStateOf(false) }
    if (performAction) {
        action.onClick()
        LaunchedEffect(Unit) { performAction = false }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = action.enabled, interactionSource = null, indication = null) { performAction = true }
            .alpha(if (action.enabled) 1f else 0.4f)
            .defaultMinSize(minHeight = 44.dp)
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalIosContentColor provides contentColor,
            com.xyonmusic.music.ui.ios.compat.LocalContentColor provides contentColor,
            LocalSymbolChrome provides MenuItemChrome,
            LocalSymbolIconSize provides 22.dp,
        ) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) { action.icon() }
        }
        Spacer(Modifier.width(8.dp))
        IosText(
            text = action.text,
            style = IosTheme.typography.body.copy(fontSize = 16.sp, letterSpacing = (-0.43).sp),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Narrowest action cell that still fits a two-line caption such as "Add to playlist". */
internal val MenuActionMinCellWidth = 120.dp

/**
 * How many actions fit across [width] without going under [minCell], and never above
 * [requestedColumns]. An unbounded width (a menu still being measured) keeps the request.
 */
internal fun menuActionColumnCount(width: Dp, minCell: Dp, gap: Dp, requestedColumns: Int): Int {
    val requested = requestedColumns.coerceAtLeast(1)
    if (!width.isFinite || width <= 0.dp) return requested
    val step = minCell + gap
    if (step <= 0.dp) return requested
    val fitted = ((width + gap) / step).toInt().coerceAtLeast(1)
    return minOf(requested, fitted)
}

@Composable
private fun ActionSegment(action: NewAction, modifier: Modifier, compact: Boolean = false) {
    val colors = IosTheme.colors
    val contentColor = when {
        action.contentColor.isSpecified -> action.contentColor
        LocalInMenuPanel.current -> com.xyonmusic.music.ui.ios.foundation.IosMenuMaterial.colors.label
        else -> IosTheme.tint
    }

    // NewAction.onClick is a @Composable lambda (several call sites open dialogs from
    // it), so it has to be invoked from composition rather than the click callback.
    var performAction by remember { mutableStateOf(false) }
    if (performAction) {
        action.onClick()
        LaunchedEffect(Unit) { performAction = false }
    }

    Column(
        modifier = modifier
            .clickable(enabled = action.enabled, interactionSource = null, indication = null) { performAction = true }
            .alpha(if (action.enabled) 1f else 0.4f)
            .padding(vertical = if (compact) 8.dp else 10.dp, horizontal = if (compact) 2.dp else 8.dp)
            .semantics { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 5.dp),
    ) {
        CompositionLocalProvider(
            LocalIosContentColor provides contentColor,
            com.xyonmusic.music.ui.ios.compat.LocalContentColor provides contentColor,
        ) {
            CompositionLocalProvider(LocalSymbolChrome provides MenuItemChrome, LocalSymbolIconSize provides if (compact) 22.dp else 24.dp) {
                Box(Modifier.size(if (compact) 24.dp else 28.dp), contentAlignment = Alignment.Center) { action.icon() }
            }
        }
        IosText(
            text = action.text,
            style = if (compact) IosTheme.typography.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                else IosTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = if (compact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One segment of a [NewActionGrid]. */
data class NewAction(
    val icon: @Composable () -> Unit,
    val text: String,
    val onClick: @Composable () -> Unit,
    val enabled: Boolean = true,
    val backgroundColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
)

