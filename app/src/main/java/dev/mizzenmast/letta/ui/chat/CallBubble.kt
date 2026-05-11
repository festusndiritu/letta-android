package dev.mizzenmast.letta.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mizzenmast.letta.core.motion.pressAndScale
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.data.local.entity.CallEntity
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

/**
 * Call log bubble for displaying call events in chat timeline.
 * Shows call direction, duration, and status with proper accessibility.
 */
@Composable
fun CallBubble(
    call: CallEntity,
    isMine: Boolean,
    onCallBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lc = LocalLettaColors.current
    val haptics = rememberHapticFeedback()

    val (callIcon, callColor, callLabel) = remember(call.status, isMine) {
        val isMissed = call.status == "missed" || call.status == "rejected"
        when {
            isMissed && isMine -> Triple(
                Icons.Rounded.CallMade,
                Color(0xFFE05C5C),
                "Missed outgoing call"
            )
            isMissed && !isMine -> Triple(
                Icons.Rounded.CallMissed,
                Color(0xFFE05C5C),
                "Missed incoming call"
            )
            isMine -> Triple(
                Icons.Rounded.CallMade,
                lc.text3,
                "Outgoing call"
            )
            else -> Triple(
                Icons.Rounded.CallReceived,
                Color(0xFF4CAF7D),
                "Incoming call"
            )
        }
    }

    val isMissed = call.status == "missed" || call.status == "rejected"

    val durationText = try {
        if (call.createdAt != null && call.endedAt != null) {
            val start = OffsetDateTime.parse(call.createdAt)
            val end = OffsetDateTime.parse(call.endedAt)
            val durationSecs = java.time.Duration.between(start, end).seconds
            if (durationSecs > 0) {
                val minutes = durationSecs / 60
                val seconds = durationSecs % 60
                String.format("%d:%02d", minutes, seconds)
            } else null
        } else null
    } catch (_: Exception) { null }

    val timeText = try {
        call.createdAt?.let { formatCallTime(it) } ?: ""
    } catch (_: Exception) { "" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.medium,
                vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall
            ),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .pressAndScale()
                .clickable(
                    onClick = {
                        haptics.click()
                        onCallBack()
                    },
                    onClickLabel = "Call back"
                ),
            shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.large),
            color = if (isMine) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            callColor.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = callIcon,
                        contentDescription = callLabel,
                        tint = callColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Call info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = callLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isMissed) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isMissed) {
                            Color(0xFFE05C5C)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (durationText != null) {
                            Text(
                                text = durationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = lc.text3,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = lc.text3,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = lc.text3,
                            fontSize = 12.sp
                        )
                    }
                }

                // Call back button
                IconButton(
                    onClick = {
                        haptics.heavyClick()
                        onCallBack()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call back",
                        tint = Color(0xFF4CAF7D),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatCallTime(isoString: String): String {
    return try {
        val parsed = OffsetDateTime.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        parsed.format(formatter)
    } catch (_: Exception) {
        ""
    }
}
