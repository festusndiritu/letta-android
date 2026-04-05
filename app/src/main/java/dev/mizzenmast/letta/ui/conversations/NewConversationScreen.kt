package dev.mizzenmast.letta.ui.conversations

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _contacts = MutableStateFlow<List<PublicUserDto>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts = _isLoadingContacts.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults = _query
        .debounce(300)
        .filter { it.length >= 2 }
        .flatMapLatest { q ->
            flow { emit(repository.searchUsers(q).getOrDefault(emptyList())) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) { _query.value = q }

    fun syncContacts(context: Context) {
        viewModelScope.launch {
            _isLoadingContacts.value = true
            val hashes = getContactPhoneHashes(context)
            if (hashes.isNotEmpty()) {
                repository.syncContacts(hashes)
                    .onSuccess { _contacts.value = it.map {
                        PublicUserDto(id = it.userId, displayName = it.displayName, avatarUrl = it.avatarUrl)
                    }}
            }
            _isLoadingContacts.value = false
        }
    }

    fun startChat(userId: String, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            repository.createDirect(userId).onSuccess { onCreated(it.id) }
        }
    }

    private fun getContactPhoneHashes(context: Context): List<String> {
        val hashes = mutableListOf<String>()
        val resolver: ContentResolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null,
        ) ?: return hashes

        cursor.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val raw = it.getString(numberIndex) ?: continue
                val normalized = normalizeKenyanNumber(raw) ?: continue
                hashes.add(sha256(normalized))
            }
        }
        return hashes.distinct()
    }

    private fun normalizeKenyanNumber(raw: String): String? {
        var n = raw.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (n.startsWith("0") && n.length == 10) n = "+254${n.drop(1)}"
        if (n.startsWith("254") && !n.startsWith("+")) n = "+$n"
        if (!n.startsWith("+")) return null
        return n
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NewConversationScreen(
    onConversationCreated: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onBack: () -> Unit,
    viewModel: NewConversationViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val query by viewModel.query.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsState()

    val contactsPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    LaunchedEffect(contactsPermission.status.isGranted) {
        if (contactsPermission.status.isGranted) {
            viewModel.syncContacts(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!contactsPermission.status.isGranted) {
            contactsPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                title = { Text("New conversation", fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )

            LazyColumn {
                // New group option
                item {
                    ListItem(
                        headlineContent = { Text("New group", fontWeight = FontWeight.Medium) },
                        leadingContent = {
                            Icon(
                                Icons.Rounded.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.clickable(onClick = onCreateGroup),
                    )
                    HorizontalDivider()
                }

                // Search results
                if (query.length >= 2) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No users found for \"$query\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(searchResults, key = { it.id }) { user ->
                            UserResultItem(user = user, onClick = {
                                viewModel.startChat(user.id, onConversationCreated)
                            })
                        }
                    }
                } else {
                    // Contacts on Letta
                    item {
                        if (isLoadingContacts) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else if (contacts.isNotEmpty()) {
                            Text(
                                "From your contacts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        } else if (contactsPermission.status.isGranted) {
                            Text(
                                "None of your contacts are on Letta yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    items(contacts, key = { it.id }) { user ->
                        UserResultItem(user = user, onClick = {
                            viewModel.startChat(user.id, onConversationCreated)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun UserResultItem(user: PublicUserDto, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(user.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = user.bio?.let { { Text(it, maxLines = 1) } },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}