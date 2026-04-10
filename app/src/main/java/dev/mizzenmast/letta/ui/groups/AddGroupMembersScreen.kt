@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.mizzenmast.letta.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.remote.dto.PublicUserDto
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGroupMembersViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {
    private val _conversationId = MutableStateFlow("")
    private val _query = MutableStateFlow("")
    private val _selected = MutableStateFlow<List<PublicUserDto>>(emptyList())

    val selected = _selected.asStateFlow()

    val members = _conversationId
        .flatMapLatest { id -> repository.observeMembers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val results = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { q ->
            flow { emit(repository.searchUsers(q).getOrDefault(emptyList())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(conversationId: String) {
        _conversationId.value = conversationId
    }

    fun setQuery(value: String) { _query.value = value }

    fun toggleUser(user: PublicUserDto) {
        _selected.update { list ->
            if (list.any { it.id == user.id }) list.filter { it.id != user.id }
            else list + user
        }
    }

    fun addMembers(onComplete: () -> Unit) {
        val conversationId = _conversationId.value
        val ids = _selected.value.map { it.id }
        if (conversationId.isBlank() || ids.isEmpty()) return
        viewModelScope.launch {
            repository.addMembers(conversationId, ids).onSuccess { onComplete() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMembersScreen(
    conversationId: String,
    onBack: () -> Unit,
    onAdded: () -> Unit,
    viewModel: AddGroupMembersViewModel = hiltViewModel(),
) {
    val members by viewModel.members.collectAsState()
    val results by viewModel.results.collectAsState()
    val selected by viewModel.selected.collectAsState()
    var query by remember { mutableStateOf("") }

    val existingIds = remember(members) { members.map { it.userId }.toSet() }

    LaunchedEffect(conversationId) { viewModel.init(conversationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = { Text("Add members", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        onClick = { viewModel.addMembers(onAdded) },
                        enabled = selected.isNotEmpty(),
                    ) { Text("Add") }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.setQuery(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search people...") },
                singleLine = true,
            )

            if (query.length < 2) {
                Text(
                    text = "Type at least 2 characters to search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (selected.isNotEmpty()) {
                Text(
                    text = "Selected: ${selected.joinToString { it.displayName }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            LazyColumn {
                items(results.filter { it.id !in existingIds }, key = { it.id }) { user ->
                    val isSelected = selected.any { it.id == user.id }
                    ListItem(
                        headlineContent = { Text(user.displayName) },
                        trailingContent = {
                            Text(
                                if (isSelected) "Selected" else "Add",
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.clickable { viewModel.toggleUser(user) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
