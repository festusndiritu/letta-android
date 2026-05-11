package dev.mizzenmast.letta.core.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.dp

/**
 * Centralized animation specifications for consistent motion design across the app.
 * Based on premium chat apps (Telegram, WhatsApp) motion language.
 */

object LettaAnimations {
    
    // ══════════════════════════════════════════════════════════════════════
    // DURATION CONSTANTS
    // ══════════════════════════════════════════════════════════════════════
    
    const val DURATION_INSTANT = 100
    const val DURATION_FAST = 200
    const val DURATION_NORMAL = 300
    const val DURATION_SLOW = 400
    const val DURATION_VERY_SLOW = 500
    
    // ══════════════════════════════════════════════════════════════════════
    // SPRING SPECIFICATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Bouncy spring for playful interactions (reactions, emoji) */
    val springBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /** Standard spring for most UI transitions */
    val springDefault = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    /** Smooth spring for message bubbles */
    val springSmooth = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 300f
    )
    
    /** Gentle spring for subtle movements */
    val springGentle = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 250f
    )
    
    /** Stiff spring for snappy interactions */
    val springSnappy = spring<Float>(
        dampingRatio = 0.7f,
        stiffness = 400f
    )
    
    // ══════════════════════════════════════════════════════════════════════
    // TWEEN SPECIFICATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    fun <T> tweenFast(): AnimationSpec<T> = tween(
        durationMillis = DURATION_FAST,
        easing = FastOutSlowInEasing
    )
    
    fun <T> tweenNormal(): AnimationSpec<T> = tween(
        durationMillis = DURATION_NORMAL,
        easing = EaseInOut
    )
    
    fun <T> tweenSlow(): AnimationSpec<T> = tween(
        durationMillis = DURATION_SLOW,
        easing = EaseOut
    )
    
    // ══════════════════════════════════════════════════════════════════════
    // MESSAGE-SPECIFIC ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Message bubble enter animation (scale + fade) */
    val messageBubbleEnter = springSmooth
    
    /** Message bubble exit animation (scale + fade) */
    val messageBubbleExit = tweenFast<Float>()
    
    /** Message bubble press scale */
    const val MESSAGE_PRESS_SCALE = 0.96f
    val messagePressSpring = springSnappy
    
    /** Swipe-to-reply threshold */
    val SWIPE_REPLY_THRESHOLD = 80.dp
    const val SWIPE_REPLY_RESISTANCE = 0.3f
    val swipeReplySpring = springDefault
    
    // ══════════════════════════════════════════════════════════════════════
    // LIST ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Stagger delay between list items (ms) */
    const val LIST_ITEM_STAGGER_DELAY = 30
    
    /** Conversation list item appear */
    val listItemEnter = springGentle
    
    /** Conversation list item swipe */
    val listItemSwipe = springDefault
    
    // ══════════════════════════════════════════════════════════════════════
    // INPUT BAR ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Input bar expansion on focus */
    val inputBarExpand = springDefault
    
    /** Send button appear/morph */
    val sendButtonMorph = springBouncy
    
    /** Attach menu radial expansion */
    val attachMenuExpand = springBouncy
    
    // ══════════════════════════════════════════════════════════════════════
    // REACTION PICKER
    // ══════════════════════════════════════════════════════════════════════
    
    /** Reaction picker appear */
    val reactionPickerEnter = springBouncy
    
    /** Individual reaction icon stagger delay */
    const val REACTION_STAGGER_DELAY = 20
    
    /** Reaction selection bounce */
    const val REACTION_SELECT_SCALE = 1.3f
    val reactionSelectSpring = springBouncy
    
    // ══════════════════════════════════════════════════════════════════════
    // MEDIA VIEWER
    // ══════════════════════════════════════════════════════════════════════
    
    /** Media fullscreen transition */
    val mediaFullscreenTransition = springSmooth
    
    /** Pinch zoom spring */
    val pinchZoomSpring = springGentle
    
    /** Swipe to dismiss threshold */
    const val SWIPE_DISMISS_THRESHOLD = 0.2f
    val swipeDismissSpring = springDefault
    
    // ══════════════════════════════════════════════════════════════════════
    // BOTTOM SHEETS & DIALOGS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Bottom sheet slide up/down */
    val bottomSheetSlide = springDefault
    
    /** Dialog scale + fade */
    val dialogAppear = springGentle
    
    /** Sheet drag resistance after threshold */
    const val SHEET_DRAG_RESISTANCE = 0.5f
    
    // ══════════════════════════════════════════════════════════════════════
    // MISC UI ELEMENTS
    // ══════════════════════════════════════════════════════════════════════
    
    /** FAB expand/collapse */
    val fabMorph = springBouncy
    
    /** Avatar scale on tap */
    const val AVATAR_TAP_SCALE = 0.92f
    val avatarTapSpring = springSnappy
    
    /** Typing indicator pulse */
    val typingIndicatorPulse = tween<Float>(
        durationMillis = 1000,
        easing = EaseInOut
    )
    
    /** Shimmer loading animation */
    val shimmerAnimation = tween<Float>(
        durationMillis = 1500,
        easing = EaseInOut
    )
}
