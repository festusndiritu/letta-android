package dev.mizzenmast.letta.ui.calls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mizzenmast.letta.service.ActiveCall
import dev.mizzenmast.letta.service.CallState
import dev.mizzenmast.letta.ui.components.LettaAvatar

/**
 * Full-screen incoming call overlay — shown when [CallState.RINGING] and [ActiveCall.isIncoming].
 */
@Composable
fun IncomingCallOverlay(
    call: ActiveCall,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.96f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(64.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .scale(pulse)
                        .size(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LettaAvatar(
                        name = call.callerName,
                        imageUrl = null,
                        size = 100.dp,
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = call.callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (call.callType == "video") "Incoming video call" else "Incoming voice call",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(72.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 80.dp),
            ) {
                CallActionButton(
                    icon = { Icon(Icons.Rounded.PhoneDisabled, contentDescription = "Decline", modifier = Modifier.size(32.dp)) },
                    color = Color(0xFFE53935),
                    label = "Decline",
                    onClick = onReject,
                )
                CallActionButton(
                    icon = { Icon(Icons.Rounded.Phone, contentDescription = "Answer", modifier = Modifier.size(32.dp)) },
                    color = Color(0xFF43A047),
                    label = "Answer",
                    onClick = onAnswer,
                )
            }
        }
    }
}

/**
 * Full-screen in-call UI — shown when [CallState.RINGING] + outgoing, or [CallState.ACTIVE].
 */
@Composable
fun ActiveCallScreen(
    call: ActiveCall,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onEnd: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(64.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LettaAvatar(
                    name = call.callerName,
                    imageUrl = null,
                    size = 100.dp,
                )
                Spacer(Modifier.height(dev.mizzenmast.letta.ui.components.LettaSpacing.large + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall))
                Text(
                    text = call.callerName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(dev.mizzenmast.letta.ui.components.LettaSpacing.small))
                Text(
                    text = when (call.state) {
                        CallState.RINGING -> if (call.isIncoming) "Ringing…" else "Calling…"
                        CallState.ACTIVE  -> "Connected"
                        CallState.ON_HOLD -> "On hold"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 80.dp),
            ) {
                CallActionButton(
                    icon = {
                        Icon(
                            if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    color = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    label = if (isMuted) "Unmute" else "Mute",
                    onClick = onToggleMute,
                    iconTint = if (isMuted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CallActionButton(
                    icon = { Icon(Icons.Rounded.CallEnd, contentDescription = "End call", modifier = Modifier.size(32.dp)) },
                    color = Color(0xFFE53935),
                    label = "End",
                    onClick = onEnd,
                )
                CallActionButton(
                    icon = {
                        Icon(
                            if (isSpeakerOn) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
                            contentDescription = if (isSpeakerOn) "Speaker on" else "Speaker off",
                            modifier = Modifier.size(28.dp),
                        )
                    },
                    color = if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "Speaker",
                    onClick = onToggleSpeaker,
                    iconTint = if (isSpeakerOn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: @Composable () -> Unit,
    color: Color,
    label: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(68.dp)) {
                icon()
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
