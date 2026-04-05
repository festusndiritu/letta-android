package dev.mizzenmast.letta.data.local.entity

import androidx.room.Entity
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

@Entity(tableName = "conversation_members")
data class ConversationMemberEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String, // conversationId_userId
    val conversationId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val role: String,
)

@Entity(tableName = "messages")
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
    val createdAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val isMine: Boolean,
    val isPending: Boolean = false, // true while sending
)