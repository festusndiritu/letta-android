package dev.mizzenmast.letta.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mizzenmast.letta.core.motion.LettaAnimations
import dev.mizzenmast.letta.core.motion.HapticFeedback
import dev.mizzenmast.letta.core.motion.swipeToReply
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium chat bubble with asymmetric tail, gradient fills, and sophisticated animations.
 * This is the signature visual element of the Letta app.
 *
 * @param isMine Whether this is user's outgoing message
 * @param content Composable content of the bubble (text, media, etc.)
 * @param modifier Standard Compose modifier
 * @param showSenderName Whether to show sender name above bubble
 * @param senderName Name to show above bubble
 * @param onLongPress Callback for long-press gesture
 * @param onSwipeReply Callback when swipe-to-reply threshold reached
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun LettaBubble(
    isMine: Boolean,
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    showSenderName: Boolean = false,
    senderName: String = "",
    onLongPress: () -> Unit = {},
    onSwipeReply: () -> Unit = {},
    hapticFeedback: HapticFeedback? = null,
) {
    // ══════════════════════════════════════════════════════════════════════
    // ANIMATIONS & STATE
    // ══════════════════════════════════════════════════════════════════════
    
    // Press animation (scale)
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) LettaAnimations.MESSAGE_PRESS_SCALE else 1f,
        animationSpec = LettaAnimations.messagePressSpring,
        label = "bubble_press_scale"
    )
    
    // Swipe-to-reply progress
    var swipeProgress by remember { mutableStateOf(0f) }
    
    // Enter animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val enterScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = LettaAnimations.messageBubbleEnter,
        label = "bubble_enter_scale"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = LettaAnimations.tweenFast(),
        label = "bubble_enter_alpha"
    )
    
    // ══════════════════════════════════════════════════════════════════════
    // COLORS
    // ══════════════════════════════════════════════════════════════════════
    
    val bubbleBackground = if (isMine) {
        // Gradient for outgoing messages (premium feel)
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        // Solid color for incoming messages
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ══════════════════════════════════════════════════════════════════════
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .swipeToReply(
                enabled = true,
                onThresholdReached = {
                    onSwipeReply()
                    hapticFeedback?.tick()
                },
                onSwipeProgress = { progress ->
                    swipeProgress = progress
                },
                hapticFeedback = hapticFeedback
            ),
        horizontalAlignment = if (isMine) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start
    ) {
        // Sender name (for group chats)
        if (showSenderName && senderName.isNotBlank()) {
            Text(
                text = senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = LettaSpacing.medium,
                    vertical = LettaSpacing.extraSmall / 2
                )
            )
        }
        
        // Message bubble
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .widthIn(max = LettaSizes.messageBubbleMaxWidth)
                .graphicsLayer {
                    // Combined scale: enter * press
                    val combinedScale = enterScale * scale
                    scaleX = combinedScale
                    scaleY = combinedScale
                    alpha = enterAlpha
                    
                    // Swipe-to-reply visual feedback (subtle rotation)
                    rotationZ = swipeProgress * 2f
                }
                .shadow(
                    elevation = if (isPressed) LettaElevation.higher else LettaElevation.low,
                    shape = BubbleShape(isMine = isMine, density = density),
                    clip = false
                )
                .background(
                    brush = bubbleBackground,
                    shape = BubbleShape(isMine = isMine, density = density)
                )
                .padding(
                    horizontal = LettaSpacing.medium,
                    vertical = LettaSpacing.small + LettaSpacing.extraSmall / 2
                ),
            content = content
        )
    }
}

/**
 * Custom bubble shape with asymmetric tail (Telegram-style).
 * 
 * For outgoing messages (isMine = true):
 * - Top-left: 18dp
 * - Top-right: 18dp
 * - Bottom-left: 18dp
 * - Bottom-right: 4dp (accent corner with tail effect)
 *
 * For incoming messages (isMine = false):
 * - Top-left: 18dp
 * - Top-right: 18dp
 * - Bottom-left: 4dp (accent corner with tail effect)
 * - Bottom-right: 18dp
 */
private fun BubbleShape(isMine: Boolean, density: Density): Shape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    
    with(density) {
        val radiusLarge = LettaCorners.large.toPx()
        val radiusSmall = LettaCorners.small.toPx()
        
        if (isMine) {
            // Outgoing: tail on bottom-right
            moveTo(radiusLarge, 0f)
            
            // Top edge with rounded corners
            lineTo(width - radiusLarge, 0f)
            quadraticBezierTo(width, 0f, width, radiusLarge)
            
            // Right edge
            lineTo(width, height - radiusSmall)
            
            // Bottom-right corner (accent/tail)
            quadraticBezierTo(width, height, width - radiusSmall, height)
            
            // Bottom edge
            lineTo(radiusLarge, height)
            
            // Bottom-left corner
            quadraticBezierTo(0f, height, 0f, height - radiusLarge)
            
            // Left edge
            lineTo(0f, radiusLarge)
            
            // Top-left corner
            quadraticBezierTo(0f, 0f, radiusLarge, 0f)
            
        } else {
            // Incoming: tail on bottom-left
            moveTo(radiusLarge, 0f)
            
            // Top edge with rounded corners
            lineTo(width - radiusLarge, 0f)
            quadraticBezierTo(width, 0f, width, radiusLarge)
            
            // Right edge
            lineTo(width, height - radiusLarge)
            
            // Bottom-right corner
            quadraticBezierTo(width, height, width - radiusLarge, height)
            
            // Bottom edge
            lineTo(radiusSmall, height)
            
            // Bottom-left corner (accent/tail)
            quadraticBezierTo(0f, height, 0f, height - radiusSmall)
            
            // Left edge
            lineTo(0f, radiusLarge)
            
            // Top-left corner
            quadraticBezierTo(0f, 0f, radiusLarge, 0f)
        }
        
        close()
    }
}

/**
 * Text content for LettaBubble with premium typography.
 *
 * @param text Message text content
 * @param isMine Whether this is user's message (affects color)
 * @param modifier Standard Compose modifier
 */
@Composable
fun BoxScope.BubbleText(
    text: String,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,  // Slightly larger than default 14sp
            lineHeight = 21.sp,  // 1.4 line height for readability
            letterSpacing = 0.01.sp  // Subtle letter-spacing
        ),
        color = textColor,
        modifier = modifier
    )
}
