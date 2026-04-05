package dev.mizzenmast.letta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("role") val role: String,
)

@Serializable
data class ConversationDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("members") val members: List<MemberDto>,
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("type") val type: String,
    @SerialName("content") val content: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_mime") val mediaMime: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class CreateDirectRequest(
    @SerialName("other_user_id") val otherUserId: String,
)

@Serializable
data class CreateGroupRequest(
    @SerialName("name") val name: String,
    @SerialName("member_ids") val memberIds: List<String>,
)

@Serializable
data class ContactSyncRequest(
    @SerialName("phone_hashes") val phoneHashes: List<String>,
)

@Serializable
data class ContactDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("phone_hash") val phoneHash: String,
)

@Serializable
data class ContactSyncResponse(
    @SerialName("contacts") val contacts: List<ContactDto>,
)

@Serializable
data class PublicUserDto(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

// WebSocket event envelope
@Serializable
data class WsEvent(
    @SerialName("type") val type: String,
    @SerialName("payload") val payload: kotlinx.serialization.json.JsonObject,
)

// WebSocket outbound
@Serializable
data class SendMessagePayload(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("type") val type: String,
    @SerialName("content") val content: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_mime") val mediaMime: String? = null,
    @SerialName("reply_to_id") val replyToId: String? = null,
)

@Serializable
data class FocusProfileRequest(
    @SerialName("profile") val profile: String,
)

@Serializable
data class MuteRequest(
    @SerialName("duration") val duration: String,
)

@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_active_at") val lastActiveAt: String,
    @SerialName("is_current") val isCurrent: Boolean,
)

@Serializable
data class ReactionRequest(
    @SerialName("emoji") val emoji: String,
)

@Serializable
data class UpdateConversationRequest(
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class AddMembersRequest(
    @SerialName("member_ids") val memberIds: List<String>,
)

@Serializable
data class RemoveMemberRequest(
    @SerialName("user_id") val userId: String,
)
