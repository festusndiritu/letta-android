package dev.mizzenmast.letta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

/**
 * Standard modifier pattern for clickable items with proper semantics.
 * Always use this instead of raw clickable for better accessibility.
 */
fun Modifier.lettaClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
) = composed {
    clickable(
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick
    )
}

/**
 * Apply conditional modifier based on condition
 */
inline fun Modifier.conditional(
    condition: Boolean,
    modifier: Modifier.() -> Modifier
): Modifier {
    return if (condition) {
        then(modifier(Modifier))
    } else {
        this
    }
}
