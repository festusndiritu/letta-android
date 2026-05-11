package dev.mizzenmast.letta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mizzenmast.letta.service.ActiveCall
import dev.mizzenmast.letta.service.CallState
import kotlinx.coroutines.delay

/**
 * Persistent green banner shown at the very top of the app when there is an
 * active or ringing call. Tapping the banner navigates back to the call screen.
 *
 * @param activeCall  The current call, or null to hide the banner
 * @param onTap       Navigate to the call screen
 * @param onHangup    End the call from the banner
 */
@Composable
fun ActiveCallBanner(
    activeCall: ActiveCall?,
    onTap: () -> Unit,
    onHangup: () -> Unit,
) {
    AnimatedVisibility(
        visible = activeCall != null,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        if (activeCall == null) return@AnimatedVisibility

        val bannerColor = when (activeCall.state) {
            CallState.RINGING  -> Color(0xFF2563EB) // blue — ringing
            CallState.ACTIVE   -> Color(0xFF16A34A) // green — connected
            CallState.ON_HOLD  -> Color(0xFF92400E) // amber — on hold
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerColor)
                .statusBarsPadding()
                .clickable(onClick = onTap)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Pulsing phone icon for RINGING, static for ACTIVE
            Icon(
                imageVector = Icons.Rounded.Phone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )

            // Label
            val label = when (activeCall.state) {
                CallState.RINGING -> if (activeCall.conversationId.isNotEmpty()) "Incoming call · ${activeCall.callerName}" else "Calling ${activeCall.callerName}…"
                CallState.ACTIVE  -> activeCall.callerName
                CallState.ON_HOLD -> "${activeCall.callerName} · On hold"
            }
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )

            // Duration timer (only shows when call is ACTIVE)
            if (activeCall.state == CallState.ACTIVE) {
                CallTimer()
            }

            // Hang-up button
            IconButton(
                onClick = onHangup,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Icon(
                    Icons.Rounded.CallEnd,
                    contentDescription = "End call",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CallTimer() {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
    )
}
