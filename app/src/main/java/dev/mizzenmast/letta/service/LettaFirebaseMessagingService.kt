package dev.mizzenmast.letta.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
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
    @Inject lateinit var callStateManager: CallStateManager

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { authRepository.registerPushToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"]

        if (type == "knock") {
            // Data-only push: sync missed messages and ensure WS is connected
            scope.launch { conversationRepository.syncMissedMessages() }
            wsManager.connect()
            // Fall through — knock may also carry notification content
        }

        when (type) {
            "call.incoming" -> handleIncomingCall(message.data)
            "call.ended"    -> handleCallEnded(message.data)
            else            -> handleMessageNotification(message)
        }
    }

    // ── Incoming call ─────────────────────────────────────────────────────────

    private fun handleIncomingCall(data: Map<String, String>) {
        val callId         = data["call_id"] ?: return
        val conversationId = data["conversation_id"] ?: return
        val callerName     = data["caller_name"] ?: "Unknown"
        val callerAvatar   = data["caller_avatar"]

        // Update global call state so the in-app banner can appear
        callStateManager.onIncomingCall(callId, conversationId, callerName)

        // Accept action - opens MainActivity with call screen
        val acceptIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ACCEPT
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
            putExtra(CallActionReceiver.EXTRA_CONVERSATION_ID, conversationId)
        }
        val acceptPi = PendingIntent.getBroadcast(
            this,
            callId.hashCode() + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Decline action
        val declineIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE
            putExtra(CallActionReceiver.EXTRA_CALL_ID, callId)
        }
        val declinePi = PendingIntent.getBroadcast(
            this,
            callId.hashCode() + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Content intent - tapping notification opens MainActivity
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_INCOMING_CALL_ID, callId)
            putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversationId)
        }
        val contentPi = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, LettaApp.NOTIFICATION_CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming call")
            .setContentText(callerName)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null) // Custom ringtone handled in CallStateManager
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // Vibrate pattern
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Decline",
                    declinePi
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    R.drawable.ic_launcher_foreground,
                    "Accept",
                    acceptPi
                ).build()
            )
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_CALL, notification)
    }

    private fun handleCallEnded(data: Map<String, String>) {
        callStateManager.onCallEnded()
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_CALL)
    }

    // ── Message notifications (grouped by conversation) ───────────────────────

    private fun handleMessageNotification(message: RemoteMessage) {
        val title          = message.data["title"] ?: message.notification?.title
        val body           = message.data["body"] ?: message.notification?.body
        val conversationId = message.data["conversation_id"]
        val senderName     = message.data["sender_name"] ?: title ?: "Letta"
        val isGroupChat    = message.data["is_group"] == "true"
        val isMention      = message.data["is_mention"] == "true"

        if (title.isNullOrBlank() && body.isNullOrBlank()) return

        val notificationId = conversationId?.hashCode() ?: NOTIFICATION_ID_MESSAGES_DEFAULT
        val groupKey = "conv_${conversationId ?: "default"}"

        // Select appropriate channel
        val channelId = when {
            isMention -> LettaApp.NOTIFICATION_CHANNEL_MENTIONS
            isGroupChat -> LettaApp.NOTIFICATION_CHANNEL_GROUP_MESSAGES
            else -> LettaApp.NOTIFICATION_CHANNEL_DIRECT_MESSAGES
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (conversationId != null) {
                putExtra(MainActivity.EXTRA_CONVERSATION_ID, conversationId)
            }
        }
        val openPi = PendingIntent.getActivity(
            this,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Mark as Read action
        val markReadIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_READ
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markReadPi = PendingIntent.getBroadcast(
            this,
            notificationId + 1,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Inline Reply action
        val replyIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REPLY
            putExtra(NotificationActionReceiver.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val replyPi = PendingIntent.getBroadcast(
            this,
            notificationId + 2,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val remoteInput = RemoteInput.Builder(NotificationActionReceiver.KEY_TEXT_REPLY)
            .setLabel("Reply")
            .build()

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Reply",
            replyPi
        ).addRemoteInput(remoteInput).build()

        // Per-conversation child notification
        val childNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(senderName)
            .setContentText(body ?: "")
            .setContentIntent(openPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(groupKey)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setStyle(
                NotificationCompat.MessagingStyle(
                    Person.Builder().setName("You").build(),
                ).addMessage(
                    body ?: "",
                    System.currentTimeMillis(),
                    Person.Builder().setName(senderName).build(),
                ),
            )
            .addAction(replyAction)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Mark as read",
                markReadPi
            )
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        // Summary notification (required for grouping to work on Android)
        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Letta")
            .setContentText("New messages")
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, childNotification)
            // Summary ID is derived from the group key — stable per conversation
            notify(notificationId + SUMMARY_ID_OFFSET, summaryNotification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID_CALL = 9001
        private const val NOTIFICATION_ID_MESSAGES_DEFAULT = 9000
        private const val SUMMARY_ID_OFFSET = 100_000
    }
}