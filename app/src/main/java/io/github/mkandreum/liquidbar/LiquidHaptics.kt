package io.github.mkandreum.liquidbar

/**
 * Lets the host app know a liquid piece just vibrated, e.g. so a generic "every tap ticks"
 * handler can stay silent instead of doubling the piece's own haptic.
 */
object LiquidHaptics {
    @Volatile var listener: (() -> Unit)? = null

    fun performed() { listener?.invoke() }
}
