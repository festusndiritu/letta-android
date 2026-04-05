package dev.mizzenmast.letta.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
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
import dev.mizzenmast.letta.data.remote.dto.UserDto
import dev.mizzenmast.letta.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.getMe().onSuccess { user ->
                _state.update { it.copyFrom(user) }
            }
        }
    }

    fun updateDisplayName(value: String) {
        _state.update { it.copy(displayName = value, isDirty = true) }
    }

    fun updateBio(value: String) {
        _state.update { it.copy(bio = value, isDirty = true) }
    }

    fun uploadAvatar(uri: Uri) {
        _state.update { it.copy(isUploadingAvatar = true) }
        viewModelScope.launch {
            authRepository.uploadAvatar(uri)
                .onSuccess { url ->
                    _state.update { it.copy(avatarUrl = url, isUploadingAvatar = false, isDirty = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isUploadingAvatar = false, error = e.message ?: "Avatar upload failed") }
                }
        }
    }

    fun save() {
        val current = _state.value
        if (!current.isDirty || current.isSaving) return
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            authRepository.updateProfile(
                displayName = current.displayName.trim().ifBlank { null },
                bio = current.bio.trim().ifBlank { null },
            )
                .onSuccess { user ->
                    _state.update { it.copyFrom(user).copy(isSaving = false, isDirty = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save profile") }
                }
        }
    }
}

data class ProfileSettingsState(
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val error: String? = null,
) {
    fun copyFrom(user: UserDto) = copy(
        displayName = user.displayName,
        bio = user.bio ?: "",
        avatarUrl = user.avatarUrl,
        isDirty = false,
        error = null,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

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
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = state.isDirty && !state.isSaving,
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (state.avatarUrl != null) {
                    AsyncImage(
                        model = state.avatarUrl,
                        contentDescription = "Avatar",
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

                if (state.isUploadingAvatar) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                }
            }

            OutlinedTextField(
                value = state.displayName,
                onValueChange = { viewModel.updateDisplayName(it) },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.bio,
                onValueChange = { viewModel.updateBio(it) },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            if (state.error != null) {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
