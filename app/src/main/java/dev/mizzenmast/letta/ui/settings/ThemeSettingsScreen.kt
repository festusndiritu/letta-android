package dev.mizzenmast.letta.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.core.theme.DuskBase
import dev.mizzenmast.letta.core.theme.EmberBase
import dev.mizzenmast.letta.core.theme.LapisBase
import dev.mizzenmast.letta.core.theme.LettaPreset
import dev.mizzenmast.letta.core.theme.MonoBase
import dev.mizzenmast.letta.core.theme.SageBase
import dev.mizzenmast.letta.core.theme.SlateBase
import dev.mizzenmast.letta.data.local.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Accent color swatch shown in each preset card. */
private val presetAccent: Map<LettaPreset, Color> = mapOf(
    LettaPreset.LAPIS   to LapisBase,
    LettaPreset.DUSK    to DuskBase,
    LettaPreset.SAGE    to SageBase,
    LettaPreset.SLATE   to SlateBase,
    LettaPreset.EMBER   to EmberBase,
    LettaPreset.MONO    to MonoBase,
    LettaPreset.DYNAMIC to Color(0xFF6750A4), // Material You fallback
)

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ThemeSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.themePresetFlow.collect { preset ->
                _state.update { it.copy(selectedPreset = preset) }
            }
        }
    }

    fun selectPreset(preset: LettaPreset) {
        viewModelScope.launch { settingsStore.setThemePreset(preset) }
    }
}

data class ThemeSettingsState(
    val selectedPreset: LettaPreset = LettaPreset.LAPIS,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: ThemeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                title = { Text("Appearance", fontWeight = FontWeight.SemiBold) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick a color preset. Font and bubble colors update automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
            }

            items(LettaPreset.entries) { preset ->
                ThemePresetCard(
                    preset = preset,
                    isSelected = state.selectedPreset == preset,
                    onClick = { viewModel.selectPreset(preset) },
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Changes apply immediately across the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: LettaPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = presetAccent[preset] ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preset.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}