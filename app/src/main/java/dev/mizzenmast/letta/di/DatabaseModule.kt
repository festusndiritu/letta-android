package dev.mizzenmast.letta.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.mizzenmast.letta.data.local.LettaDatabase
import dev.mizzenmast.letta.data.local.MIGRATION_2_3
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.dao.UserDao
import dev.mizzenmast.letta.data.local.dao.PinnedMessageDao
import dev.mizzenmast.letta.data.local.dao.StatusDao
import dev.mizzenmast.letta.data.local.dao.CallDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LettaDatabase =
        Room.databaseBuilder(context, LettaDatabase::class.java, "letta.db")
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides fun provideUserDao(db: LettaDatabase): UserDao = db.userDao()
    @Provides fun provideConversationDao(db: LettaDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: LettaDatabase): MessageDao = db.messageDao()
    @Provides fun providePinnedMessageDao(db: LettaDatabase): PinnedMessageDao = db.pinnedMessageDao()
    @Provides fun provideStatusDao(db: LettaDatabase): StatusDao = db.statusDao()
    @Provides fun provideCallDao(db: LettaDatabase): CallDao = db.callDao()
}