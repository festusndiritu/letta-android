package dev.mizzenmast.letta.core.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Custom gesture detectors with animation and haptic feedback integration.
 * Premium interactions for Letta messaging app.
 */

/**
 * Swipe-to-reply gesture modifier.
 * Provides elastic resistance, visual feedback, and haptic confirmation.
 *
 * @param enabled Whether the gesture is enabled
 * @param threshold Distance in pixels to trigger reply action
 * @param onThresholdReached Called when swipe exceeds threshold
 * @param onSwipeProgress Called continuously with swipe progress (0f to 1f+)
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun Modifier.swipeToReply(
    enabled: Boolean = true,
    threshold: Dp = LettaAnimations.SWIPE_REPLY_THRESHOLD,
    onThresholdReached: () -> Unit,
    onSwipeProgress: (Float) -> Unit = {},
    hapticFeedback: HapticFeedback? = null
): Modifier {
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { threshold.toPx() }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    val swipeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this.pointerInput(enabled, thresholdPx) {
        if (!enabled) return@pointerInput
        
        detectDragGestures(
            onDragStart = {
                hasTriggeredHaptic = false
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val newOffset = (swipeOffset.value + dragAmount.x).coerceAtMost(0f)
                
                // Apply elastic resistance after threshold
                val effectiveOffset = if (abs(newOffset) > thresholdPx) {
                    val excess = abs(newOffset) - thresholdPx
                    -thresholdPx - (excess * LettaAnimations.SWIPE_REPLY_RESISTANCE)
                } else {
                    newOffset
                }
                
                coroutineScope.launch {
                    swipeOffset.snapTo(effectiveOffset)
                }
                
                // Update progress (0f at rest, 1f at threshold, >1f beyond)
                val progress = abs(effectiveOffset) / thresholdPx
                onSwipeProgress(progress.coerceAtLeast(0f))
                
                // Trigger haptic at threshold
                if (progress >= 1f && !hasTriggeredHaptic) {
                    hapticFeedback?.swipeThreshold()
                    hasTriggeredHaptic = true
                }
            },
            onDragEnd = {
                val didExceedThreshold = abs(swipeOffset.value) >= thresholdPx
                
                coroutineScope.launch {
                    swipeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.swipeReplySpring
                    )
                    onSwipeProgress(0f)
                }
                
                if (didExceedThreshold) {
                    onThresholdReached()
                }
                
                hasTriggeredHaptic = false
            },
            onDragCancel = {
                coroutineScope.launch {
                    swipeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.swipeReplySpring
                    )
                    onSwipeProgress(0f)
                }
                hasTriggeredHaptic = false
            }
        )
    }
}

/**
 * Press-and-scale gesture with haptic feedback.
 * Creates a premium "button press" feel.
 *
 * @param onPress Called when pressed
 * @param onLongPress Called on long press
 * @param onRelease Called when released
 * @param scaleTarget Scale factor when pressed (0.96f = 96% of original size)
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun Modifier.pressAndScale(
    onPress: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
    onRelease: () -> Unit = {},
    scaleTarget: Float = LettaAnimations.MESSAGE_PRESS_SCALE,
    hapticFeedback: HapticFeedback? = null
): Modifier {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this.pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                onPress()
                coroutineScope.launch {
                    scale.animateTo(
                        targetValue = scaleTarget,
                        animationSpec = LettaAnimations.messagePressSpring
                    )
                }
                
                val released = tryAwaitRelease()
                
                coroutineScope.launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = LettaAnimations.messagePressSpring
                    )
                }
                
                if (released) {
                    onRelease()
                    hapticFeedback?.click()
                }
            },
            onLongPress = {
                onLongPress?.invoke()
                hapticFeedback?.longPress()
            }
        )
    }
}

/**
 * Double-tap gesture for quick reactions.
 *
 * @param onDoubleTap Called when double-tap is detected
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun Modifier.doubleTapToReact(
    onDoubleTap: (Offset) -> Unit,
    hapticFeedback: HapticFeedback? = null
): Modifier {
    return this.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = { offset ->
                onDoubleTap(offset)
                hapticFeedback?.tick()
            }
        )
    }
}

/**
 * Horizontal swipe gesture for conversation list items (archive, delete, mute).
 *
 * @param onSwipeLeft Called when swiped left with distance
 * @param onSwipeRight Called when swiped right with distance
 * @param threshold Minimum swipe distance to trigger action
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun Modifier.horizontalSwipe(
    onSwipeLeft: ((Float) -> Unit)? = null,
    onSwipeRight: ((Float) -> Unit)? = null,
    threshold: Dp = 120.dp,
    hapticFeedback: HapticFeedback? = null
): Modifier {
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { threshold.toPx() }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    val swipeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this.pointerInput(thresholdPx) {
        detectDragGestures(
            onDragStart = {
                hasTriggeredHaptic = false
            },
            onDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    swipeOffset.snapTo(swipeOffset.value + dragAmount.x)
                }
                
                // Trigger haptic at threshold
                if (abs(swipeOffset.value) >= thresholdPx && !hasTriggeredHaptic) {
                    hapticFeedback?.swipeThreshold()
                    hasTriggeredHaptic = true
                }
            },
            onDragEnd = {
                val finalOffset = swipeOffset.value
                
                coroutineScope.launch {
                    swipeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.listItemSwipe
                    )
                }
                
                when {
                    finalOffset < -thresholdPx -> onSwipeLeft?.invoke(abs(finalOffset))
                    finalOffset > thresholdPx -> onSwipeRight?.invoke(finalOffset)
                }
                
                hasTriggeredHaptic = false
            },
            onDragCancel = {
                coroutineScope.launch {
                    swipeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.listItemSwipe
                    )
                }
                hasTriggeredHaptic = false
            }
        )
    }
}

/**
 * Pull-to-refresh gesture with elastic resistance.
 *
 * @param onRefresh Called when refresh is triggered
 * @param onPullProgress Called continuously with pull progress (0f to 1f+)
 * @param threshold Pull distance to trigger refresh
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun Modifier.pullToRefresh(
    onRefresh: () -> Unit,
    onPullProgress: (Float) -> Unit = {},
    threshold: Dp = 80.dp,
    hapticFeedback: HapticFeedback? = null
): Modifier {
    val thresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { threshold.toPx() }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    val pullOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    
    return this.pointerInput(thresholdPx) {
        detectDragGestures(
            onDragStart = {
                hasTriggeredHaptic = false
            },
            onDrag = { change, dragAmount ->
                if (dragAmount.y < 0) return@detectDragGestures // Only pull down
                
                change.consume()
                val newOffset = (pullOffset.value + dragAmount.y).coerceAtLeast(0f)
                
                // Apply elastic resistance after threshold
                val effectiveOffset = if (newOffset > thresholdPx) {
                    val excess = newOffset - thresholdPx
                    thresholdPx + (excess * 0.4f) // 40% resistance
                } else {
                    newOffset
                }
                
                coroutineScope.launch {
                    pullOffset.snapTo(effectiveOffset)
                }
                
                val progress = effectiveOffset / thresholdPx
                onPullProgress(progress.coerceAtLeast(0f))
                
                // Trigger haptic at threshold
                if (progress >= 1f && !hasTriggeredHaptic) {
                    hapticFeedback?.refreshTriggered()
                    hasTriggeredHaptic = true
                }
            },
            onDragEnd = {
                val didExceedThreshold = pullOffset.value >= thresholdPx
                
                coroutineScope.launch {
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.springDefault
                    )
                    onPullProgress(0f)
                }
                
                if (didExceedThreshold) {
                    onRefresh()
                }
                
                hasTriggeredHaptic = false
            },
            onDragCancel = {
                coroutineScope.launch {
                    pullOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = LettaAnimations.springDefault
                    )
                    onPullProgress(0f)
                }
                hasTriggeredHaptic = false
            }
        )
    }
}
