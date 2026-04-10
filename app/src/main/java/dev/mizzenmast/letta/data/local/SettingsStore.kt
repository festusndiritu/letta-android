package dev.mizzenmast.letta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mizzenmast.letta.core.theme.LettaPreset
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "letta_settings",
)

@Singleton
class SettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    val themePresetFlow: Flow<LettaPreset> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_PRESET] ?: LettaPreset.DEFAULT.name
        LettaPreset.entries.firstOrNull { it.name == raw } ?: LettaPreset.DEFAULT
    }

    suspend fun setThemePreset(preset: LettaPreset) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_PRESET] = preset.name
        }
    }

    companion object {
        private val KEY_THEME_PRESET = stringPreferencesKey("theme_preset")
    }
}
