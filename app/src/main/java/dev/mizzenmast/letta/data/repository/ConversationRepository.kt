package dev.mizzenmast.letta.data.repository

import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.entity.ConversationEntity
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.remote.api.ConversationApiService
import dev.mizzenmast.letta.data.remote.dto.ContactDto
import dev.mizzenmast.letta.data.remote.dto.ContactSyncRequest
import dev.mizzenmast.letta.data.remote.dto.ConversationDto
import dev.mizzenmast.letta.data.remote.dto.CreateDirectRequest
import dev.mizzenmast.letta.data.remote.dto.CreateGroupRequest
import dev.mizzenmast.letta.data.remote.dto.FocusProfileRequest
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import dev.mizzenmast.letta.data.remote.dto.MuteRequest
import dev.mizzenmast.letta.data.remote.dto.PublicUserDto
import dev.mizzenmast.letta.data.remote.dto.ReactionRequest
import dev.mizzenmast.letta.data.remote.dto.SessionDto
import dev.mizzenmast.letta.data.remote.dto.AddMembersRequest
import dev.mizzenmast.letta.data.remote.dto.RemoveMemberRequest
import dev.mizzenmast.letta.data.remote.dto.UpdateConversationRequest
import dev.mizzenmast.letta.service.toEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationRepository @Inject constructor(
    private val api: ConversationApiService,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val tokenStore: dev.mizzenmast.letta.data.local.TokenStore,
) {
    // ── Conversations ───────────────────────────────────────────────────────

    fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeAll()

    fun observeConversation(id: String): Flow<ConversationEntity?> =
        conversationDao.observeById(id)

    suspend fun refreshConversations() {
        try {
            val conversations = api.getConversations()
            val currentUserId = tokenStore.userId
            conversationDao.upsertAll(conversations.map { conv ->
                val display = resolveDirectDisplay(conv, currentUserId)
                conv.toEntity(
                    nameOverride = display?.displayName,
                    avatarOverride = display?.avatarUrl,
                )
            })
            conversations.forEach { conv ->
                conversationDao.upsertMembers(conv.members.map { member ->
                    ConversationMemberEntity(
                        id = "${conv.id}_${member.userId}",
                        conversationId = conv.id,
                        userId = member.userId,
                        displayName = member.displayName,
                        avatarUrl = member.avatarUrl,
                        role = member.role,
                    )
                })
            }
        } catch (_: Exception) { }
    }

    suspend fun createDirect(otherUserId: String): Result<ConversationDto> {
        return try {
            val conv = api.createDirect(CreateDirectRequest(otherUserId))
            val display = resolveDirectDisplay(conv, tokenStore.userId)
            conversationDao.upsert(
                conv.toEntity(
                    nameOverride = display?.displayName,
                    avatarOverride = display?.avatarUrl,
                )
            )
            Result.success(conv)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, memberIds: List<String>): Result<ConversationDto> {
        return try {
            val conv = api.createGroup(CreateGroupRequest(name, memberIds))
            conversationDao.upsert(conv.toEntity())
            Result.success(conv)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeMembers(conversationId: String) =
        conversationDao.observeMembers(conversationId)

    // ── Messages ────────────────────────────────────────────────────────────

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.observeByConversation(conversationId)

    suspend fun loadMoreMessages(conversationId: String, beforeId: String): Result<List<MessageDto>> {
        return try {
            val messages = api.getMessages(conversationId, beforeId = beforeId)
            val members = conversationDao.getMembers(conversationId)
            val memberMap = members.associateBy { it.userId }
            val currentUserId = tokenStore.userId
            messageDao.upsertAll(messages.map { msg ->
                val member = memberMap[msg.senderId]
                msg.toEntity(
                    isMine = msg.senderId == currentUserId,
                    senderName = member?.displayName ?: "",
                    senderAvatar = member?.avatarUrl,
                )
            })
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMissedMessages() {
        try {
            val since = messageDao.getLatestTimestamp() ?: return
            val missed = api.getMissedMessages(since)
            val currentUserId = tokenStore.userId
            val grouped = missed.groupBy { it.conversationId }
            val resolved = grouped.flatMap { (conversationId, msgs) ->
                val members = conversationDao.getMembers(conversationId)
                val memberMap = members.associateBy { it.userId }
                msgs.map { msg ->
                    val member = memberMap[msg.senderId]
                    msg.toEntity(
                        isMine = msg.senderId == currentUserId,
                        senderName = member?.displayName ?: "",
                        senderAvatar = member?.avatarUrl,
                    )
                }
            }
            messageDao.upsertAll(resolved)
        } catch (_: Exception) { }
    }

    suspend fun clearUnread(conversationId: String) {
        conversationDao.clearUnread(conversationId)
    }

    // ── Contacts ────────────────────────────────────────────────────────────

    suspend fun syncContacts(phoneHashes: List<String>): Result<List<ContactDto>> {
        return try {
            val response = api.syncContacts(ContactSyncRequest(phoneHashes))
            Result.success(response.contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<PublicUserDto>> {
        return try {
            Result.success(api.searchUsers(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<PublicUserDto> {
        return try {
            Result.success(api.getUser(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Settings ────────────────────────────────────────────────────────────

    suspend fun setFocusProfile(profile: String) {
        try { api.setFocusProfile(FocusProfileRequest(profile)) } catch (_: Exception) { }
    }

    suspend fun muteConversation(conversationId: String, duration: String) {
        try { api.muteConversation(conversationId, MuteRequest(duration)) } catch (_: Exception) { }
    }

    suspend fun unmuteConversation(conversationId: String) {
        try { api.unmuteConversation(conversationId) } catch (_: Exception) { }
    }

    suspend fun getSessions(): Result<List<SessionDto>> {
        return try {
            Result.success(api.getSessions())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revokeSession(sessionId: String): Result<Unit> {
        return try {
            api.revokeSession(sessionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(messageId: String, emoji: String): Result<Unit> {
        return try {
            api.reactToMessage(messageId, ReactionRequest(emoji))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversation(conversationId: String): Result<ConversationDto> {
        return try {
            val conv = api.getConversation(conversationId)
            val display = resolveDirectDisplay(conv, tokenStore.userId)
            conversationDao.upsert(
                conv.toEntity(
                    nameOverride = display?.displayName,
                    avatarOverride = display?.avatarUrl,
                )
            )
            conversationDao.upsertMembers(conv.members.map { member ->
                ConversationMemberEntity(
                    id = "${conv.id}_${member.userId}",
                    conversationId = conv.id,
                    userId = member.userId,
                    displayName = member.displayName,
                    avatarUrl = member.avatarUrl,
                    role = member.role,
                )
            })
            Result.success(conv)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConversation(
        conversationId: String,
        name: String? = null,
        avatarUrl: String? = null,
    ): Result<ConversationDto> {
        return try {
            val updated = api.updateConversation(
                conversationId,
                UpdateConversationRequest(name = name, avatarUrl = avatarUrl),
            )
            val display = resolveDirectDisplay(updated, tokenStore.userId)
            conversationDao.upsert(
                updated.toEntity(
                    nameOverride = display?.displayName,
                    avatarOverride = display?.avatarUrl,
                )
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMembers(conversationId: String, memberIds: List<String>): Result<Unit> {
        return try {
            api.addMembers(conversationId, AddMembersRequest(memberIds))
            getConversation(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(conversationId: String, userId: String): Result<Unit> {
        return try {
            api.removeMember(conversationId, RemoveMemberRequest(userId))
            conversationDao.deleteMember(conversationId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private data class DirectDisplay(val displayName: String?, val avatarUrl: String?)

private fun resolveDirectDisplay(
    conv: ConversationDto,
    currentUserId: String?,
): DirectDisplay? {
    if (conv.type != "direct") return null
    val other = conv.members.firstOrNull { it.userId != currentUserId } ?: conv.members.firstOrNull()
    return DirectDisplay(other?.displayName, other?.avatarUrl)
}

fun ConversationDto.toEntity(
    nameOverride: String? = null,
    avatarOverride: String? = null,
) = ConversationEntity(
    id = id,
    type = type,
    name = nameOverride ?: name,
    avatarUrl = avatarOverride ?: avatarUrl,
    createdAt = createdAt,
    lastMessageContent = null,
    lastMessageAt = null,
    lastMessageSenderId = null,
)