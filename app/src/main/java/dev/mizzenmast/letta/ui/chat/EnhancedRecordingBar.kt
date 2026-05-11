package dev.mizzenmast.letta.ui.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mizzenmast.letta.core.motion.pressAndScale
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Enhanced voice recording UI with waveform visualization.
 * Features:
 * - Animated waveform during recording
 * - Time display
 * - Cancel/Send actions
 * - Recording indicator pulse
 */
@Composable
fun EnhancedRecordingBar(
    elapsedMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHapticFeedback()
    
    // Waveform animation
    var waveformData by remember { mutableStateOf(List(40) { 0.3f }) }
    
    LaunchedEffect(elapsedMs) {
        while (true) {
            delay(100) // Update waveform every 100ms
            waveformData = waveformData.drop(1) + Random.nextFloat().coerceIn(0.2f, 1f)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.small,
                vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.small
            ),
        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.huge),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = dev.mizzenmast.letta.ui.components.LettaElevation.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.medium,
                    vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.medium
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Delete button
            IconButton(
                onClick = {
                    haptics.heavyClick()
                    onCancel()
                },
                modifier = Modifier
                    .size(40.dp)
                    .pressAndScale()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Cancel recording",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Recording indicator + timer
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing red dot
                    PulsingRecordingIndicator()
                    
                    // Timer
                    Text(
                        text = formatRecordingDuration(elapsedMs),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Animated waveform
                AnimatedWaveform(
                    data = waveformData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }

            // Send button
            IconButton(
                onClick = {
                    haptics.heavyClick()
                    onSend()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                    .pressAndScale()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send recording",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun PulsingRecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .background(
                Color(0xFFE05C5C).copy(alpha = alpha),
                CircleShape
            )
    )
}

@Composable
private fun AnimatedWaveform(
    data: List<Float>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / data.size
        val maxBarHeight = height * 0.8f
        
        data.forEachIndexed { index, value ->
            val barHeight = maxBarHeight * value
            val x = index * barWidth + barWidth / 2
            val y = (height - barHeight) / 2

            // Gradient bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x - barWidth * 0.3f, y),
                size = Size(barWidth * 0.6f, barHeight),
                cornerRadius = CornerRadius(barWidth * 0.3f, barWidth * 0.3f)
            )
        }
    }
}

private fun formatRecordingDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
