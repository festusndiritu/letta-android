package dev.mizzenmast.letta.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver for handling notification actions:
 * - Mark as read
 * - Inline reply
 * - Dismiss
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var conversationRepository: ConversationRepository

    @Inject
    lateinit var wsManager: WebSocketManager

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        when (intent.action) {
            ACTION_MARK_READ -> {
                // Mark as read via API/repository if method exists
                // For now, just cancel notification
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            }

            ACTION_REPLY -> {
                val replyText = RemoteInput.getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_TEXT_REPLY)
                    ?.toString()

                if (!replyText.isNullOrBlank()) {
                    // Send message via WebSocket
                    wsManager.sendMessage(
                        conversationId = conversationId,
                        type = "text",
                        content = replyText.trim(),
                        replyToId = null
                    )

                    // Clear notification after reply
                    if (notificationId != -1) {
                        NotificationManagerCompat.from(context).cancel(notificationId)
                    }
                }
            }

            ACTION_DISMISS -> {
                if (notificationId != -1) {
                    NotificationManagerCompat.from(context).cancel(notificationId)
                }
            }
        }
    }

    companion object {
        const val ACTION_MARK_READ = "dev.mizzenmast.letta.ACTION_MARK_READ"
        const val ACTION_REPLY = "dev.mizzenmast.letta.ACTION_REPLY"
        const val ACTION_DISMISS = "dev.mizzenmast.letta.ACTION_DISMISS"

        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
