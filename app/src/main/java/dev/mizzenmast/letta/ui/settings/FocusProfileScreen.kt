package dev.mizzenmast.letta.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusProfileViewModel @Inject constructor(
    private val repository: ConversationRepository,
) : ViewModel() {
    private val _profile = MutableStateFlow("normal")
    val profile = _profile.asStateFlow()

    fun setProfile(value: String) {
        _profile.value = value
        viewModelScope.launch { repository.setFocusProfile(value) }
    }
}

data class FocusOption(val key: String, val label: String, val description: String)

val focusOptions = listOf(
    FocusOption("normal", "Normal", "Everything works as configured. Presence, notifications, all active."),
    FocusOption("quiet", "Quiet", "Presence not broadcast. Notifications delivered silently."),
    FocusOption("off", "Off", "WebSocket disconnected. No notifications. You won't receive anything until you switch back."),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusProfileScreen(
    onBack: () -> Unit,
    viewModel: FocusProfileViewModel = hiltViewModel(),
) {
    val current by viewModel.profile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text("Focus profile", fontWeight = FontWeight.SemiBold) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Control how Letta behaves when you need focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            focusOptions.forEach { option ->
                val isSelected = current == option.key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setProfile(option.key) }
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(12.dp),
                            ) else Modifier
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(option.label, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                }
            }
        }
    }
}