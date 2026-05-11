package dev.mizzenmast.letta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mizzenmast.letta.core.motion.HapticFeedback
import dev.mizzenmast.letta.core.motion.LettaAnimations

/**
 * Premium floating input bar for Letta messaging.
 * Features: elevated card design, spring animations, haptic feedback, smooth transitions.
 *
 * @param value Current text input value
 * @param onValueChange Callback when text changes
 * @param onSend Callback when send button is clicked
 * @param onAttach Callback when attach button is clicked
 * @param onCamera Callback when camera button is clicked
 * @param onMic Callback when mic button is clicked (hold-to-record)
 * @param onEmojiToggle Callback when emoji/keyboard toggle is clicked
 * @param modifier Standard Compose modifier
 * @param placeholder Placeholder text
 * @param showEmojiPicker Whether emoji picker is currently shown
 * @param showCamera Whether camera button should be visible
 * @param enabled Whether input is enabled
 * @param isUploading Whether media upload is in progress
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun LettaInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onCamera: () -> Unit,
    onMic: () -> Unit,
    onEmojiToggle: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Message",
    showEmojiPicker: Boolean = false,
    showCamera: Boolean = true,
    enabled: Boolean = true,
    isUploading: Boolean = false,
    hapticFeedback: HapticFeedback? = null,
) {
    // ══════════════════════════════════════════════════════════════════════
    // STATE & ANIMATIONS
    // ══════════════════════════════════════════════════════════════════════
    
    val canSend = value.isNotBlank()
    var hasFocus by remember { mutableStateOf(false) }
    
    // Elevation increases on focus
    val elevation by animateFloatAsState(
        targetValue = if (hasFocus) 8f else 4f,
        animationSpec = LettaAnimations.springGentle,
        label = "input_elevation"
    )
    
    // Send button scale animation
    val sendButtonScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.9f,
        animationSpec = LettaAnimations.springBouncy,
        label = "send_button_scale"
    )
    
    // ══════════════════════════════════════════════════════════════════════
    // LAYOUT
    // ══════════════════════════════════════════════════════════════════════
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(LettaCorners.huge),
                clip = false
            ),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(LettaCorners.huge),
        tonalElevation = LettaElevation.low
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LettaSpacing.small,
                    vertical = LettaSpacing.small
                )
                .animateContentSize(animationSpec = LettaAnimations.springDefault as FiniteAnimationSpec<IntSize>),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ══════════════════════════════════════════════════════════════
            // EMOJI / KEYBOARD TOGGLE
            // ══════════════════════════════════════════════════════════════
            
            IconButton(
                onClick = {
                    hapticFeedback?.click()
                    onEmojiToggle()
                },
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (showEmojiPicker) Icons.Rounded.Keyboard else Icons.Rounded.EmojiEmotions,
                    contentDescription = if (showEmojiPicker) "Show keyboard" else "Show emoji picker",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // ══════════════════════════════════════════════════════════════
            // TEXT INPUT FIELD
            // ══════════════════════════════════════════════════════════════
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        letterSpacing = 0.01.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                hapticFeedback?.tick()
                                onSend()
                            }
                        }
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    enabled = enabled && !isUploading,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    lineHeight = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            // ══════════════════════════════════════════════════════════════
            // TRAILING ICONS (ATTACH, CAMERA)
            // ══════════════════════════════════════════════════════════════
            
            AnimatedVisibility(
                visible = value.isBlank(),
                enter = scaleIn(animationSpec = LettaAnimations.springBouncy) + fadeIn(),
                exit = scaleOut(animationSpec = tween(durationMillis = LettaAnimations.DURATION_FAST)) + fadeOut()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(
                        onClick = {
                            hapticFeedback?.click()
                            onAttach()
                        },
                        enabled = enabled && !isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = "Attach file",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (showCamera) {
                        IconButton(
                            onClick = {
                                hapticFeedback?.click()
                                onCamera()
                            },
                            enabled = enabled && !isUploading
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // ══════════════════════════════════════════════════════════════
            // SEND / MIC BUTTON
            // ══════════════════════════════════════════════════════════════
            
            IconButton(
                onClick = {
                    if (canSend) {
                        hapticFeedback?.tick()
                        onSend()
                    } else {
                        hapticFeedback?.recordingStart()
                        onMic()
                    }
                },
                enabled = enabled && !isUploading,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = sendButtonScale
                        scaleY = sendButtonScale
                    }
                    .clip(CircleShape)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector = if (canSend) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                    contentDescription = if (canSend) "Send message" else "Voice message",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Reply preview bar that appears above the input when replying to a message.
 *
 * @param replyToName Name of the person being replied to
 * @param replyToText Preview of the message being replied to
 * @param onCancel Callback when cancel button is clicked
 * @param modifier Standard Compose modifier
 * @param hapticFeedback Haptic feedback instance
 */
@Composable
fun ReplyPreviewBar(
    replyToName: String,
    replyToText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedback: HapticFeedback? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Replying to $replyToName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = replyToText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            IconButton(
                onClick = {
                    hapticFeedback?.click()
                    onCancel()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel reply",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
