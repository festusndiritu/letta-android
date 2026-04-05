package dev.mizzenmast.letta.ui.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.repository.ConversationRepository
import dev.mizzenmast.letta.data.repository.MediaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupSettingsUiState(
    val nameInput: String = "",
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GroupSettingsViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val mediaRepository: MediaRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _conversationId = MutableStateFlow("")
    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()

    val conversation = _conversationId
        .flatMapLatest { id -> repository.observeConversation(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members = _conversationId
        .flatMapLatest { id -> repository.observeMembers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId: String? = tokenStore.userId

    fun init(conversationId: String) {
        if (_conversationId.value == conversationId) return
        _conversationId.value = conversationId
        viewModelScope.launch {
            repository.getConversation(conversationId)
        }
    }

    fun setNameInput(value: String) {
        _uiState.update { it.copy(nameInput = value) }
    }

    fun syncNameFromConversation() {
        val name = conversation.value?.name ?: ""
        if (_uiState.value.nameInput.isBlank()) {
            _uiState.update { it.copy(nameInput = name) }
        }
    }

    fun saveName() {
        val conversationId = _conversationId.value
        val newName = _uiState.value.nameInput.trim()
        if (conversationId.isBlank() || newName.isBlank()) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            repository.updateConversation(conversationId, name = newName)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to update group") }
                }
        }
    }

    fun uploadAvatar(uri: Uri) {
        val conversationId = _conversationId.value
        if (conversationId.isBlank()) return
        _uiState.update { it.copy(isUploadingAvatar = true, error = null) }
        viewModelScope.launch {
            mediaRepository.upload(uri)
                .onSuccess { media ->
                    repository.updateConversation(conversationId, avatarUrl = media.url)
                    _uiState.update { it.copy(isUploadingAvatar = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isUploadingAvatar = false, error = e.message ?: "Avatar upload failed") }
                }
        }
    }

    fun mute(duration: String) {
        val conversationId = _conversationId.value
        if (conversationId.isBlank()) return
        viewModelScope.launch { repository.muteConversation(conversationId, duration) }
    }

    fun unmute() {
        val conversationId = _conversationId.value
        if (conversationId.isBlank()) return
        viewModelScope.launch { repository.unmuteConversation(conversationId) }
    }

    fun removeMember(userId: String) {
        val conversationId = _conversationId.value
        if (conversationId.isBlank()) return
        viewModelScope.launch { repository.removeMember(conversationId, userId) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    conversationId: String,
    onBack: () -> Unit,
    onAddMembers: (String) -> Unit,
    onMemberClick: (String) -> Unit,
    viewModel: GroupSettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(conversationId) { viewModel.init(conversationId) }

    val conversation by viewModel.conversation.collectAsState()
    val members by viewModel.members.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(conversation?.name) { viewModel.syncNameFromConversation() }

    val currentUserId = viewModel.currentUserId
    val isAdmin = remember(members, currentUserId) {
        members.firstOrNull { it.userId == currentUserId }?.role == "admin"
    }
    val canEdit = isAdmin

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.uploadAvatar(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = { Text("Group settings", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveName() },
                        enabled = canEdit && uiState.nameInput.isNotBlank() && !uiState.isSaving,
                    ) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .then(
                                if (canEdit) Modifier.clickable { imagePicker.launch("image/*") }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (conversation?.avatarUrl != null) {
                            AsyncImage(
                                model = conversation?.avatarUrl,
                                contentDescription = "Group avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "Pick avatar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (uiState.isUploadingAvatar) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(Modifier.size(12.dp))

                    OutlinedTextField(
                        value = uiState.nameInput,
                        onValueChange = { viewModel.setNameInput(it) },
                        label = { Text("Group name") },
                        singleLine = true,
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (uiState.error != null) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            item {
                SectionHeader("Notifications")
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Mute 1 hour") },
                            modifier = Modifier.clickable { viewModel.mute("1h") },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Mute 8 hours") },
                            modifier = Modifier.clickable { viewModel.mute("8h") },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Mute 1 week") },
                            modifier = Modifier.clickable { viewModel.mute("1w") },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Mute always") },
                            modifier = Modifier.clickable { viewModel.mute("always") },
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Unmute") },
                            modifier = Modifier.clickable { viewModel.unmute() },
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Members (${members.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (canEdit) {
                        Button(onClick = { onAddMembers(conversationId) }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Add")
                        }
                    }
                }
            }

            items(members, key = { it.id }) { member ->
                MemberRow(
                    member = member,
                    isAdmin = isAdmin,
                    isSelf = member.userId == currentUserId,
                    onClick = { onMemberClick(member.userId) },
                    onRemove = { viewModel.removeMember(member.userId) },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Danger zone",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Leave group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            if (currentUserId != null) viewModel.removeMember(currentUserId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun MemberRow(
    member: ConversationMemberEntity,
    isAdmin: Boolean,
    isSelf: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(member.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = if (member.role == "admin") {
            { Text("Admin", color = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = if (isAdmin && !isSelf) {
            { TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) } }
        } else null,
    )
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}
