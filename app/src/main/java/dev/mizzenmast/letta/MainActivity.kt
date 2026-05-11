package dev.mizzenmast.letta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.mizzenmast.letta.core.navigation.LettaNavGraph
import dev.mizzenmast.letta.core.theme.LettaPreset
import dev.mizzenmast.letta.core.theme.LettaTheme
import dev.mizzenmast.letta.data.local.SettingsStore
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.repository.AuthRepository
import dev.mizzenmast.letta.service.CallStateManager
import dev.mizzenmast.letta.service.CallState
import dev.mizzenmast.letta.ui.calls.IncomingCallOverlay
import dev.mizzenmast.letta.ui.calls.ActiveCallScreen
import dev.mizzenmast.letta.ui.components.ActiveCallBanner
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var callStateManager: CallStateManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: UI already reflects OS-level notification state. */ }

    // Stays false until the minimum async init that should block the splash finishes.
    private var splashReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !splashReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (tokenStore.isLoggedIn()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    lifecycleScope.launch { authRepository.registerPushToken(token) }
                }
            }
        }

        val launchConversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val openCall = intent?.getBooleanExtra(EXTRA_INCOMING_CALL_ID, false) ?: false

        setContent {
            // Signal splash is done on first composition
            LaunchedEffect(Unit) { splashReady = true }
            val preset by settingsStore.themePresetFlow.collectAsStateWithLifecycle(
                initialValue = LettaPreset.LAPIS,
            )
            val activeCall by callStateManager.activeCall.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            LettaTheme(preset = preset) {
                var callMuted by remember { mutableStateOf(false) }
                var callSpeakerOn by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Persistent call banner — lives above all screens so users
                        // can keep chatting while on a call and return with one tap.
                        ActiveCallBanner(
                            activeCall = activeCall,
                            onTap = {
                                val convId = activeCall?.conversationId
                                if (convId != null) navController.navigate("chat/$convId")
                            },
                            onHangup = { callStateManager.endCall() },
                        )
                        LettaNavGraph(
                            navController = navController,
                            tokenStore = tokenStore,
                            startChatConversationId = launchConversationId,
                            callStateManager = callStateManager,
                        )
                    }

                    // Incoming call overlay
                    val incoming = activeCall
                    if (incoming != null && incoming.state == CallState.RINGING && incoming.isIncoming) {
                        IncomingCallOverlay(
                            call = incoming,
                            onAnswer = {
                                callStateManager.answerCall()
                                callMuted = false
                                callSpeakerOn = false
                            },
                            onReject = { callStateManager.rejectCall() },
                        )
                    }

                    // Active call / outgoing call overlay
                    val outgoingOrActive = activeCall
                    if (outgoingOrActive != null &&
                        (outgoingOrActive.state == CallState.ACTIVE ||
                         (outgoingOrActive.state == CallState.RINGING && !outgoingOrActive.isIncoming))
                    ) {
                        ActiveCallScreen(
                            call = outgoingOrActive,
                            isMuted = callMuted,
                            isSpeakerOn = callSpeakerOn,
                            onEnd = { callStateManager.endCall() },
                            onToggleMute = { callMuted = !callMuted },
                            onToggleSpeaker = { callSpeakerOn = !callSpeakerOn },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID  = "conversation_id"
        const val EXTRA_INCOMING_CALL_ID = "open_call"
    }
}