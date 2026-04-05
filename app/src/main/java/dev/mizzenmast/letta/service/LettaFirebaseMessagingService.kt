package dev.mizzenmast.letta.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import dev.mizzenmast.letta.LettaApp
import dev.mizzenmast.letta.MainActivity
import dev.mizzenmast.letta.R
import dev.mizzenmast.letta.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LettaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var wsManager: WebSocketManager
    @Inject lateinit var conversationRepository: dev.mizzenmast.letta.data.repository.ConversationRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { authRepository.registerPushToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]
        if (type == "knock") {
            // Data-only push — sync missed messages and reconnect WebSocket
            scope.launch { conversationRepository.syncMissedMessages() }
            wsManager.connect()
        }

        val title = message.data["title"]
            ?: message.notification?.title
        val body = message.data["body"]
            ?: message.notification?.body
        val conversationId = message.data["conversation_id"]

        val hasNotificationContent = !title.isNullOrBlank() || !body.isNullOrBlank()
        if (type == "knock" && !hasNotificationContent) {
            return
        }

        showMessageNotification(
            title ?: "Letta",
            body ?: "New message",
            conversationId,
        )
    }

    private fun showMessageNotification(
        title: String,
        body: String,
        conversationId: String?,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (conversationId != null) {
                putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversationId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conversationId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, LettaApp.NOTIFICATION_CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}