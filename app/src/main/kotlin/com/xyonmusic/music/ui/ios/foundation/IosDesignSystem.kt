/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.ios.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import com.xyonmusic.music.constants.AppBackgroundColorKey
import com.xyonmusic.music.constants.AppTextColorKey
import com.xyonmusic.music.utils.rememberPreference

/**
 * Semantic iOS-style colors for content and Liquid Glass surfaces.
 *
 * Use these roles instead of hard-coded colors so light/dark themes stay
 * consistent. User-selected background and text colors override the defaults
 * when set; clearing them restores the system palette.
 */
object IosDesignSystem {
    /** Screen canvas. */
    val IosColors.canvas: Color get() = systemGroupedBackground

    /** Card/group on the canvas. */
    val IosColors.card: Color get() = secondarySystemGroupedBackground

    /** Nested group inside a card. */
    val IosColors.nested: Color get() = tertiarySystemGroupedBackground

    /** Plain-screen background (non-grouped screens). */
    val IosColors.plainBackground: Color get() = systemBackground

    /** Card on a plain screen. */
    val IosColors.plainCard: Color get() = secondarySystemBackground

    /** Primary text — safety/critical copy always uses this. */
    val IosColors.primaryText: Color get() = label

    /** Secondary text — metadata, subtitles. */
    val IosColors.secondaryText: Color get() = secondaryLabel

    /** Tertiary text — captions, footnotes. */
    val IosColors.tertiaryText: Color get() = tertiaryLabel

    /** Hairline dividers. */
    val IosColors.hairline: Color get() = separator
}

/**
 * Standard material levels, thin→thick. Thicker = more opaque = fine text;
 * thinner = more context. Choose by semantic need, not apparent color.
 */
enum class IosMaterial { UltraThin, Thin, Regular, Thick }

/** Fill alpha each material level paints over the backdrop. */
fun IosMaterial.fillAlpha(): Float = when (this) {
    IosMaterial.UltraThin -> 0.28f
    IosMaterial.Thin -> 0.45f
    IosMaterial.Regular -> 0.65f
    IosMaterial.Thick -> 0.85f
}

/** Label color on a prominent (accent-filled) chrome control — always white. */
val chromeOnProminent: Color get() = Color.White
