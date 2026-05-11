package dev.mizzenmast.letta.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized constants for composables to ensure consistency
 * and easy maintenance across the app.
 */
@Immutable
object LettaSpacing {
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 24.dp
    val huge: Dp = 32.dp
}

@Immutable
object LettaCorners {
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val extraLarge: Dp = 20.dp
    val huge: Dp = 28.dp
}

@Immutable
object LettaElevation {
    val none: Dp = 0.dp
    val low: Dp = 2.dp
    val medium: Dp = 4.dp
    val high: Dp = 8.dp
    val higher: Dp = 12.dp
}

@Immutable
object LettaSizes {
    // Avatar sizes
    val avatarSmall: Dp = 32.dp
    val avatarMedium: Dp = 40.dp
    val avatarLarge: Dp = 52.dp
    val avatarExtraLarge: Dp = 80.dp
    
    // Icon sizes
    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 20.dp
    val iconLarge: Dp = 24.dp
    val iconExtraLarge: Dp = 32.dp
    
    // Button sizes
    val buttonHeight: Dp = 48.dp
    val iconButtonSize: Dp = 40.dp
    
    // Input
    val inputMinHeight: Dp = 40.dp
    val inputMaxHeight: Dp = 120.dp
    
    // Message bubble
    val messageBubbleMaxWidth: Dp = 280.dp
}

@Immutable
object LettaAnimationDurations {
    const val quick = 150
    const val normal = 300
    const val slow = 450
    const val verySlow = 600
}

/**
 * Commonly used emoji presets for reactions
 */
object LettaEmojis {
    val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
}

/**
 * Standard alpha values for transparency
 */
object LettaAlpha {
    const val disabled = 0.38f
    const val medium = 0.6f
    const val high = 0.87f
    const val full = 1f
}
