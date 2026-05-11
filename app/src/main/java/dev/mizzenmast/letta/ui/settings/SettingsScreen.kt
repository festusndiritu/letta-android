package dev.mizzenmast.letta.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.core.motion.pressAndScale
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class SettingsState(
    val presenceVisible: Boolean = false,
    val receiptsVisible: Boolean = false,
    val showTimestamps: Boolean = false,
    val autoDownloadMedia: Boolean = true,
    val compressImages: Boolean = true,
    val hapticFeedback: Boolean = true,
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

    fun updateAutoDownloadMedia(v: Boolean) {
        _state.update { it.copy(autoDownloadMedia = v) }
        // TODO: Save to SharedPreferences
    }

    fun updateCompressImages(v: Boolean) {
        _state.update { it.copy(compressImages = v) }
        // TODO: Save to SharedPreferences
    }

    fun updateHapticFeedback(v: Boolean) {
        _state.update { it.copy(hapticFeedback = v) }
        // TODO: Save to SharedPreferences
    }

    fun clearCache() {
        viewModelScope.launch {
            // TODO: Implement cache clearing
        }
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
    val haptics = rememberHapticFeedback()
    val lc = LocalLettaColors.current
    var showClearCacheDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Profile section
            item {
                SectionHeader("PROFILE")
                EnhancedSettingsItem(
                    title = "Profile",
                    subtitle = "Photo, display name, and bio",
                    icon = Icons.Rounded.Person,
                    onClick = {
                        haptics.click()
                        onProfileClick()
                    }
                )
                SettingsDivider()
            }

            // Privacy section
            item { SectionHeader("PRIVACY") }
            item {
                SettingsSwitch(
                    title = "Show online status",
                    subtitle = "Let others see when you're active",
                    checked = state.presenceVisible,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updatePresence(it)
                    }
                )
            }
            item {
                SettingsSwitch(
                    title = "Read receipts",
                    subtitle = "Both parties must enable for either to see",
                    checked = state.receiptsVisible,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updateReceipts(it)
                    }
                )
            }
            item {
                SettingsSwitch(
                    title = "Show timestamps",
                    subtitle = "Display time on all messages",
                    checked = state.showTimestamps,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updateTimestamps(it)
                    }
                )
            }
            item { SettingsDivider() }

            // Notifications section
            item { SectionHeader("NOTIFICATIONS") }
            item {
                EnhancedSettingsItem(
                    title = "Notification channels",
                    subtitle = "Customize alerts for different types",
                    icon = Icons.Rounded.Notifications,
                    onClick = {
                        haptics.click()
                        // TODO: Open system notification settings
                    }
                )
            }
            item {
                EnhancedSettingsItem(
                    title = "Focus profile",
                    subtitle = "Normal, Quiet, or Off",
                    icon = Icons.Rounded.DoNotDisturb,
                    onClick = {
                        haptics.click()
                        onFocusProfileClick()
                    }
                )
            }
            item { SettingsDivider() }

            // Data & Storage section
            item { SectionHeader("DATA & STORAGE") }
            item {
                SettingsSwitch(
                    title = "Auto-download media",
                    subtitle = "Automatically save media on WiFi",
                    checked = state.autoDownloadMedia,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updateAutoDownloadMedia(it)
                    }
                )
            }
            item {
                SettingsSwitch(
                    title = "Compress images",
                    subtitle = "Reduce file size before sending",
                    checked = state.compressImages,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updateCompressImages(it)
                    }
                )
            }
            item {
                EnhancedSettingsItem(
                    title = "Clear cache",
                    subtitle = "Free up storage space",
                    icon = Icons.Rounded.CleaningServices,
                    onClick = {
                        haptics.heavyClick()
                        showClearCacheDialog = true
                    }
                )
            }
            item { SettingsDivider() }

            // Appearance section
            item { SectionHeader("APPEARANCE") }
            item {
                EnhancedSettingsItem(
                    title = "Theme",
                    subtitle = "Light, Dark, or System",
                    icon = Icons.Rounded.Palette,
                    onClick = {
                        haptics.click()
                        onThemeClick()
                    }
                )
            }
            item {
                SettingsSwitch(
                    title = "Haptic feedback",
                    subtitle = "Vibrations for interactions",
                    checked = state.hapticFeedback,
                    onCheckedChange = {
                        haptics.tick()
                        viewModel.updateHapticFeedback(it)
                    }
                )
            }
            item { SettingsDivider() }

            // Account section
            item { SectionHeader("ACCOUNT") }
            item {
                EnhancedSettingsItem(
                    title = "Active sessions",
                    subtitle = "Manage logged-in devices",
                    icon = Icons.Rounded.Devices,
                    onClick = {
                        haptics.click()
                        onSessionsClick()
                    }
                )
            }
            item { SettingsDivider() }

            // About section
            item { SectionHeader("ABOUT") }
            item {
                EnhancedSettingsItem(
                    title = "Version",
                    subtitle = "1.0.0 (Build 1)",
                    icon = Icons.Rounded.Info,
                    onClick = {
                        haptics.click()
                    }
                )
            }
            item { SettingsDivider() }

            // Logout
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .pressAndScale()
                        .clickable {
                            haptics.heavyClick()
                            viewModel.logout()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ExitToApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "Log out",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Clear cache confirmation dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear cache?") },
            text = { Text("This will delete temporary files and media thumbnails to free up space.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCache()
                    showClearCacheDialog = false
                }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(
                horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.large,
                vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.medium
            )
            .padding(top = dev.mizzenmast.letta.ui.components.LettaSpacing.small),
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        },
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalLettaColors.current.text3
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun EnhancedSettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.large,
                vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall
            )
            .pressAndScale()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dev.mizzenmast.letta.ui.components.LettaSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalLettaColors.current.text3
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LocalLettaColors.current.text3
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp
    )
}