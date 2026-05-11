package dev.mizzenmast.letta.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns a [State<Dp>] representing the height of the currently-visible
 * software keyboard. When the keyboard is hidden, returns 0.dp.
 *
 * Backed by WindowInsets.ime so it updates smoothly during IME animations.
 */
@Composable
fun rememberKeyboardHeight(): State<Dp> {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    return remember(density) {
        derivedStateOf {
            val px = imeInsets.getBottom(density)
            if (px > 0) (px / density.density).dp else 0.dp
        }
    }
}

/**
 * Returns the keyboard height clamped to a sane range:
 * - Never below [minHeight] (so the picker is still usable when no keyboard was
 *   ever shown)
 * - Never above [maxHeight]
 *
 * Typical usage:
 * ```
 * val pickerHeight by rememberClampedKeyboardHeight()
 * ModalBottomSheet(...) {
 *     Column(Modifier.height(pickerHeight)) { ... }
 * }
 * ```
 */
@Composable
fun rememberClampedKeyboardHeight(
    minHeight: Dp = 300.dp,
    maxHeight: Dp = 500.dp,
): State<Dp> {
    val raw by rememberKeyboardHeight()
    return remember(minHeight, maxHeight) {
        derivedStateOf {
            when {
                raw <= 0.dp -> minHeight
                raw < minHeight -> minHeight
                raw > maxHeight -> maxHeight
                else -> raw
            }
        }
    }
}
