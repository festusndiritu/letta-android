package dev.mizzenmast.letta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var settingsStore: SettingsStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* No-op: UI already reflects OS-level notification state. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContent {
            val preset by settingsStore.themePresetFlow.collectAsStateWithLifecycle(
                initialValue = LettaPreset.DEFAULT,
            )
            val navController = rememberNavController()

            LettaTheme(preset = preset) {
                LettaNavGraph(
                    navController = navController,
                    tokenStore = tokenStore,
                    startChatConversationId = launchConversationId,
                )
            }
        }
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}