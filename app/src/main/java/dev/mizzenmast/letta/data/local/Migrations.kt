package dev.mizzenmast.letta.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add indices for better query performance
        
        // conversation_members indices
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_members_conversationId` ON `conversation_members` (`conversationId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_members_userId` ON `conversation_members` (`userId`)")
        
        // messages indices
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_conversationId_createdAt` ON `messages` (`conversationId`, `createdAt`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_senderId` ON `messages` (`senderId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_isPending` ON `messages` (`isPending`)")
        
        // pinned_messages index
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_pinned_messages_conversationId` ON `pinned_messages` (`conversationId`)")
        
        // statuses indices
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_userId_createdAt` ON `statuses` (`userId`, `createdAt`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_statuses_isMine` ON `statuses` (`isMine`)")
        
        // calls indices
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_calls_conversationId` ON `calls` (`conversationId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_calls_createdAt` ON `calls` (`createdAt`)")
    }
}
