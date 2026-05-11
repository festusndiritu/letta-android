package dev.mizzenmast.letta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.dao.UserDao
import dev.mizzenmast.letta.data.local.entity.ConversationEntity
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.local.entity.PinnedMessageEntity
import dev.mizzenmast.letta.data.local.entity.StatusEntity
import dev.mizzenmast.letta.data.local.entity.CallEntity
import dev.mizzenmast.letta.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        ConversationMemberEntity::class,
        MessageEntity::class,
        PinnedMessageEntity::class,
        StatusEntity::class,
        CallEntity::class,
    ],
    version = 3,
    exportSchema = false,  // Can enable this later with schema location
)
abstract class LettaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun pinnedMessageDao(): dev.mizzenmast.letta.data.local.dao.PinnedMessageDao
    abstract fun statusDao(): dev.mizzenmast.letta.data.local.dao.StatusDao
    abstract fun callDao(): dev.mizzenmast.letta.data.local.dao.CallDao
}