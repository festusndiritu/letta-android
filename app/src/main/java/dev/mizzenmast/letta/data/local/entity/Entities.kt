package dev.mizzenmast.letta.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val bio: String?,
    val avatarUrl: String?,
    val phoneNumber: String,
    val presenceVisible: Boolean,
    val receiptsVisible: Boolean,
    val showTimestamps: Boolean,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val type: String, // direct | group
    val name: String?,
    val avatarUrl: String?,
    val createdAt: String,
    val lastMessageContent: String?,
    val lastMessageAt: String?,
    val lastMessageSenderId: String?,
    val unreadCount: Int = 0,
)

@Entity(
    tableName = "conversation_members",
    indices = [Index(value = ["conversationId"]), Index(value = ["userId"])]
)
data class ConversationMemberEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String, // conversationId_userId
    val conversationId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "createdAt"]),
        Index(value = ["senderId"]),
        Index(value = ["isPending"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val type: String, // text | image | video | audio | document
    val content: String?,
    val mediaUrl: String?,
    val mediaMime: String?,
    val replyToId: String?,
    val replyToContent: String?,
    val replyToSenderName: String?,
    val pollData: String?,
    val reactionsJson: String?,
    val myReaction: String?,
    val deletedAt: String?,
    val createdAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val isMine: Boolean,
    val isPending: Boolean = false, // true while sending
)

@Entity(
    tableName = "pinned_messages",
    indices = [Index(value = ["conversationId"])]
)
data class PinnedMessageEntity(
    @PrimaryKey val id: String, // conversationId_messageId
    val conversationId: String,
    val messageId: String,
    val pinnedAt: String? = null,
)

@Entity(
    tableName = "statuses",
    indices = [Index(value = ["userId", "createdAt"]), Index(value = ["isMine"])]
)
data class StatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val content: String?,
    val mediaUrl: String?,
    val bgColor: String?,
    val createdAt: String?,
    val isMine: Boolean,
)

@Entity(
    tableName = "calls",
    indices = [Index(value = ["conversationId"]), Index(value = ["createdAt"])]
)
data class CallEntity(
    @PrimaryKey val id: String,
    val conversationId: String?,
    val callerId: String?,
    val calleeId: String?,
    val type: String?,
    val createdAt: String?,
    val endedAt: String?,
    val status: String?,
)
