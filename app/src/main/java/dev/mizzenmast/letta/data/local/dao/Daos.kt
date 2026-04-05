package dev.mizzenmast.letta.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.mizzenmast.letta.data.local.entity.ConversationEntity
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id")
    fun observeById(id: String): Flow<UserEntity?>
}

@Dao
interface ConversationDao {
    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt IS NULL ASC, lastMessageAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Upsert
    suspend fun upsertMembers(members: List<ConversationMemberEntity>)

    @Query("SELECT * FROM conversation_members WHERE conversationId = :conversationId")
    fun observeMembers(conversationId: String): Flow<List<ConversationMemberEntity>>

    @Query("SELECT * FROM conversation_members WHERE conversationId = :conversationId")
    suspend fun getMembers(conversationId: String): List<ConversationMemberEntity>

    @Query("""
        UPDATE conversations 
        SET lastMessageContent = :content, lastMessageAt = :at, lastMessageSenderId = :senderId
        WHERE id = :conversationId
    """)
    suspend fun updateLastMessage(conversationId: String, content: String?, at: String, senderId: String)

    @Query("UPDATE conversations SET unreadCount = unreadCount + 1 WHERE id = :conversationId")
    suspend fun incrementUnread(conversationId: String)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnread(conversationId: String)

    @Query("UPDATE conversations SET name = :name, avatarUrl = :avatarUrl WHERE id = :conversationId")
    suspend fun updateNameAvatar(conversationId: String, name: String?, avatarUrl: String?)

    @Query("DELETE FROM conversation_members WHERE conversationId = :conversationId AND userId = :userId")
    suspend fun deleteMember(conversationId: String, userId: String)
}

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :conversationId 
        AND createdAt < :before
        ORDER BY createdAt DESC 
        LIMIT :limit
    """)
    suspend fun getPageBefore(conversationId: String, before: String, limit: Int): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatest(conversationId: String, limit: Int = 30): List<MessageEntity>

    @Query("UPDATE messages SET deliveredAt = :at WHERE id = :messageId")
    suspend fun markDelivered(messageId: String, at: String)

    @Query("UPDATE messages SET readAt = :at WHERE id = :messageId")
    suspend fun markRead(messageId: String, at: String)

    @Query("UPDATE messages SET isPending = 0, id = :serverId WHERE id = :tempId")
    suspend fun confirmSent(tempId: String, serverId: String)

    @Query("SELECT * FROM messages WHERE createdAt > :since AND conversationId IN (:conversationIds)")
    suspend fun getMissedMessages(since: String, conversationIds: List<String>): List<MessageEntity>

    @Query("SELECT MAX(createdAt) FROM messages")
    suspend fun getLatestTimestamp(): String?
}