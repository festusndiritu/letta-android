package dev.mizzenmast.letta.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val presenceVisible: Boolean = false,
    val receiptsVisible: Boolean = false,
    val showTimestamps: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getMe().onSuccess { user ->
                _state.update {
                    it.copy(
                        presenceVisible = user.presenceVisible,
                        receiptsVisible = user.receiptsVisible,
                        showTimestamps = user.showTimestamps,
                    )
                }
            }
        }
    }

    fun updatePresence(v: Boolean) {
        _state.update { it.copy(presenceVisible = v) }
        viewModelScope.launch { authRepository.updateProfile(presenceVisible = v) }
    }

    fun updateReceipts(v: Boolean) {
        _state.update { it.copy(receiptsVisible = v) }
        viewModelScope.launch { authRepository.updateProfile(receiptsVisible = v) }
    }

    fun updateTimestamps(v: Boolean) {
        _state.update { it.copy(showTimestamps = v) }
        viewModelScope.launch { authRepository.updateProfile(showTimestamps = v) }
    }

    fun logout() { authRepository.logout() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onFocusProfileClick: () -> Unit,
    onSessionsClick: () -> Unit,
    onThemeClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SectionHeader("Profile")
            SettingsItem("Profile", "Photo, name, and bio", Icons.Rounded.Person, onProfileClick)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Privacy")
            SettingsSwitch("Show online status", "Let others see when you're active", state.presenceVisible) { viewModel.updatePresence(it) }
            SettingsSwitch("Read receipts", "Both parties must enable for either to see", state.receiptsVisible) { viewModel.updateReceipts(it) }
            SettingsSwitch("Show timestamps", "Display time on messages", state.showTimestamps) { viewModel.updateTimestamps(it) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Focus")
            SettingsItem("Focus profile", "Normal, Quiet, or Off", Icons.Rounded.DoNotDisturb, onFocusProfileClick)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader("Account")
            SettingsItem("Active sessions", "Manage logged-in devices", Icons.Rounded.Devices, onSessionsClick)
            SettingsItem("Appearance", "Theme and font", Icons.Rounded.Palette, onThemeClick)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text("Log out", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.AutoMirrored.Rounded.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { viewModel.logout() },
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
fun SettingsSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
    )
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}