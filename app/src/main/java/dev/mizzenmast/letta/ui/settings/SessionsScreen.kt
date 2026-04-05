package dev.mizzenmast.letta.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.remote.dto.SessionDto
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SessionDto>>(emptyList())
    val sessions = _sessions.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSessions().onSuccess { _sessions.value = it }
        }
    }

    fun revoke(sessionId: String) {
        viewModelScope.launch {
            repository.revokeSession(sessionId).onSuccess {
                _sessions.update { list -> list.filter { it.id != sessionId } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onBack: () -> Unit,
    viewModel: SessionsViewModel = hiltViewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text("Active sessions", fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    "Up to 5 active sessions. Revoke any you don't recognise.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }

            items(sessions, key = { it.id }) { session ->
                ListItem(
                    headlineContent = {
                        Text(
                            session.deviceName ?: "Unknown device",
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    supportingContent = {
                        Text(
                            if (session.isCurrent) "This device" else "Last active ${session.lastActiveAt.take(10)}",
                            color = if (session.isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.PhoneAndroid, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = {
                        if (!session.isCurrent) {
                            TextButton(onClick = { viewModel.revoke(session.id) }) {
                                Text("Revoke", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}