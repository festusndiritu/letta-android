package dev.mizzenmast.letta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.dao.UserDao
import dev.mizzenmast.letta.data.local.entity.ConversationEntity
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ConversationEntity::class,
        ConversationMemberEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LettaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}