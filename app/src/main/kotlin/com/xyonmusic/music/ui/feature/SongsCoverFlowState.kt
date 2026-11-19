/**
 * XyonMusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.xyonmusic.music.ui.feature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Whether the Songs tab is showing its full-screen album CoverFlow. The root reads this to hide
 * the top bar, the player and the floating navigation while the flow owns the screen.
 */
object SongsCoverFlowState {
    var open by mutableStateOf(false)
}
