package dev.mizzenmast.letta.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.remote.dto.PublicUserDto
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {
    private val _user = MutableStateFlow<PublicUserDto?>(null)
    val user = _user.asStateFlow()
    private val _directConversationId = MutableStateFlow<String?>(null)

    fun load(userId: String) {
        viewModelScope.launch {
            repository.getUser(userId).onSuccess { _user.value = it }
        }
    }

    fun startChat(userId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repository.createDirect(userId).onSuccess { onCreated(it.id) }
        }
    }

    private suspend fun ensureConversationId(userId: String): String? {
        _directConversationId.value?.let { return it }
        return repository.createDirect(userId)
            .getOrNull()
            ?.id
            ?.also { _directConversationId.value = it }
    }

    fun muteConversation(userId: String, duration: String) {
        viewModelScope.launch {
            val conversationId = ensureConversationId(userId) ?: return@launch
            repository.muteConversation(conversationId, duration)
        }
    }

    fun unmuteConversation(userId: String) {
        viewModelScope.launch {
            val conversationId = ensureConversationId(userId) ?: return@launch
            repository.unmuteConversation(conversationId)
        }
    }

    fun clearChat(userId: String) {
        viewModelScope.launch {
            val conversationId = ensureConversationId(userId) ?: return@launch
            repository.clearLocalConversation(conversationId)
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch { repository.blockUser(userId) }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch { repository.unblockUser(userId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onStartChat: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val user by viewModel.user.collectAsState()
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showUnblockConfirm by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var showMuteSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = { Text(user?.displayName ?: "", fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (user?.avatarUrl != null) {
                    AsyncImage(
                        model = user?.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = user?.displayName?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = user?.displayName ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            user?.bio?.let { bio ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.startChat(userId, onStartChat) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send message")
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(Modifier.height(8.dp))
            Surface(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Mute conversation") },
                        supportingContent = { Text("1 hour, 8 hours, 1 week, or always") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Rounded.VolumeOff, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showMuteSheet = true },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Unmute conversation") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                        },
                        modifier = Modifier.clickable { viewModel.unmuteConversation(userId) },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Block user") },
                        supportingContent = { Text("They will not be able to message you") },
                        leadingContent = {
                            Icon(Icons.Rounded.Block, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showBlockConfirm = true },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Unblock user") },
                        leadingContent = {
                            Icon(Icons.Rounded.PersonOff, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showUnblockConfirm = true },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Clear chat locally") },
                        supportingContent = { Text("Removes messages from this device") },
                        leadingContent = {
                            Icon(Icons.Rounded.Delete, contentDescription = null)
                        },
                        modifier = Modifier.clickable { showClearChatConfirm = true },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Notification settings") },
                        supportingContent = { Text("Coming soon") },
                        leadingContent = {
                            Icon(Icons.Rounded.Notifications, contentDescription = null)
                        },
                    )
                }
            }
        }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            title = { Text("Block this user?") },
            text = { Text("They won't be able to message you, and you won't see their messages.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.blockUser(userId)
                        showBlockConfirm = false
                    },
                ) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showUnblockConfirm) {
        AlertDialog(
            onDismissRequest = { showUnblockConfirm = false },
            title = { Text("Unblock this user?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unblockUser(userId)
                        showUnblockConfirm = false
                    },
                ) { Text("Unblock") }
            },
            dismissButton = {
                TextButton(onClick = { showUnblockConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = { Text("Clear local chat?") },
            text = { Text("This only clears messages on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearChat(userId)
                        showClearChatConfirm = false
                    },
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showMuteSheet) {
        MuteOptionsSheet(
            onDismiss = { showMuteSheet = false },
            onMute = { duration ->
                viewModel.muteConversation(userId, duration)
                showMuteSheet = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuteOptionsSheet(
    onDismiss: () -> Unit,
    onMute: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                headlineContent = { Text("Mute 1 hour") },
                modifier = Modifier.clickable { onMute("1h") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Mute 8 hours") },
                modifier = Modifier.clickable { onMute("8h") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Mute 1 week") },
                modifier = Modifier.clickable { onMute("1w") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Mute always") },
                modifier = Modifier.clickable { onMute("always") },
            )
        }
    }
}