package dev.mizzenmast.letta.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.mizzenmast.letta.data.local.LettaDatabase
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.dao.UserDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LettaDatabase =
        Room.databaseBuilder(context, LettaDatabase::class.java, "letta.db")
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides fun provideUserDao(db: LettaDatabase): UserDao = db.userDao()
    @Provides fun provideConversationDao(db: LettaDatabase): ConversationDao = db.conversationDao()
    @Provides fun provideMessageDao(db: LettaDatabase): MessageDao = db.messageDao()
}