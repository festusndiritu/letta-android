package dev.mizzenmast.letta.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives accept/decline actions from the call notification buttons.
 * Hilt-injected so it can access the [CallStateManager] singleton.
 */
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject lateinit var callStateManager: CallStateManager

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return

        when (intent.action) {
            ACTION_ACCEPT -> {
                val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
                callStateManager.onCallAccepted(callId)
                // Cancel the ringing notification (the active call banner replaces it)
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CALL)
                // Launch MainActivity to the call screen if app is in background
                if (conversationId != null) {
                    val launchIntent = Intent(context, Class.forName("dev.mizzenmast.letta.MainActivity")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("conversation_id", conversationId)
                        putExtra("open_call", true)
                    }
                    context.startActivity(launchIntent)
                }
            }
            ACTION_DECLINE -> {
                callStateManager.onCallDeclined()
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CALL)
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT  = "dev.mizzenmast.letta.CALL_ACCEPT"
        const val ACTION_DECLINE = "dev.mizzenmast.letta.CALL_DECLINE"
        const val EXTRA_CALL_ID  = "call_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val NOTIFICATION_ID_CALL = 9001
    }
}
