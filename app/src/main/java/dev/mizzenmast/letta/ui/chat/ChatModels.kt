package dev.mizzenmast.letta.ui.chat

import androidx.compose.runtime.Immutable
import dev.mizzenmast.letta.data.local.entity.MessageEntity

/**
 * UI state models for chat composables.
 * Marked as @Immutable to enable smart recomposition.
 */

@Immutable
data class MessageItemState(
    val message: MessageEntity,
    val showSenderName: Boolean,
    val replyMessage: MessageEntity?,
    val reactionCounts: Map<String, Int>,
    val showReactionPicker: Boolean,
)

@Immutable
data class AttachmentOption(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val mimeType: String,
)

@Immutable
data class MediaPreviewState(
    val uri: android.net.Uri,
    val mimeType: String?,
    val caption: String,
)
