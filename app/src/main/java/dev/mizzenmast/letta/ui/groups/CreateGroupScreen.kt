@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.mizzenmast.letta.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.remote.dto.PublicUserDto
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selected = MutableStateFlow<List<PublicUserDto>>(emptyList())
    val selected = _selected.asStateFlow()

    @OptIn(FlowPreview::class)
    val results = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { q ->
            flow { emit(repository.searchUsers(q).getOrDefault(emptyList())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun toggleUser(user: PublicUserDto) {
        _selected.update { list ->
            if (list.any { it.id == user.id }) list.filter { it.id != user.id }
            else list + user
        }
    }

    fun createGroup(name: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repository.createGroup(name, _selected.value.map { it.id })
                .onSuccess { onCreated(it.id) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onGroupCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsStateWithLifecycle(emptyList())
    val selected by viewModel.selected.collectAsState()
    var groupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = { Text("New group", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        onClick = { viewModel.createGroup(groupName, onGroupCreated) },
                        enabled = groupName.isNotBlank() && selected.isNotEmpty(),
                    ) {
                        Text("Create")
                    }
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
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Group name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Add people...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            if (selected.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selected.forEach { user ->
                        AssistChip(
                            onClick = { viewModel.toggleUser(user) },
                            label = { Text(user.displayName) },
                        )
                    }
                }
            }

            LazyColumn {
                items(results, key = { it.id }) { user ->
                    val isSelected = selected.any { it.id == user.id }
                    ListItem(
                        headlineContent = { Text(user.displayName) },
                        trailingContent = {
                            Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleUser(user) })
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(initial: T): State<T> = collectAsState(initial)