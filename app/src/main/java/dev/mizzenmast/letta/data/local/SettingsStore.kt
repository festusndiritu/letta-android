package dev.mizzenmast.letta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
        val raw = prefs[KEY_THEME_PRESET] ?: LettaPreset.LAPIS.name
        LettaPreset.entries.firstOrNull { it.name == raw } ?: LettaPreset.LAPIS
    }

    suspend fun setThemePreset(preset: LettaPreset) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_PRESET] = preset.name
        }
    }

    // ── Dark mode ────────────────────────────────────────────────────────────
    // Values: "SYSTEM" | "LIGHT" | "DARK"
    val themeModeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    // ── Font size scalar ─────────────────────────────────────────────────────
    val fontScaleFlow: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_FONT_SCALE] ?: 1.0f
    }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SCALE] = scale
        }
    }

    // ── Bubble style ─────────────────────────────────────────────────────────
    // Values: "ROUNDED" | "SHARP" | "PILL"
    val bubbleStyleFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_BUBBLE_STYLE] ?: "ROUNDED"
    }

    suspend fun setBubbleStyle(style: String) {
        dataStore.edit { prefs ->
            prefs[KEY_BUBBLE_STYLE] = style
        }
    }

    // ── Chat wallpaper ────────────────────────────────────────────────────────
    // type: "NONE" | "COLOR" | "IMAGE"
    val wallpaperTypeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_TYPE] ?: "NONE"
    }

    suspend fun setWallpaperType(type: String) {
        dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_TYPE] = type
        }
    }

    // value: hex color string (e.g. "#FF1A2B3C") or URI string for IMAGE type
    val wallpaperValueFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_VALUE] ?: ""
    }

    suspend fun setWallpaperValue(value: String) {
        dataStore.edit { prefs ->
            prefs[KEY_WALLPAPER_VALUE] = value
        }
    }

    // ── Message timestamps always visible ────────────────────────────────────
    val alwaysShowTimestampsFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ALWAYS_TIMESTAMPS] ?: false
    }

    suspend fun setAlwaysShowTimestamps(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ALWAYS_TIMESTAMPS] = show
        }
    }

    companion object {
        private val KEY_THEME_PRESET       = stringPreferencesKey("theme_preset")
        private val KEY_THEME_MODE         = stringPreferencesKey("theme_mode")
        private val KEY_FONT_SCALE         = floatPreferencesKey("font_scale")
        private val KEY_BUBBLE_STYLE       = stringPreferencesKey("bubble_style")
        private val KEY_WALLPAPER_TYPE     = stringPreferencesKey("wallpaper_type")
        private val KEY_WALLPAPER_VALUE    = stringPreferencesKey("wallpaper_value")
        private val KEY_ALWAYS_TIMESTAMPS  = booleanPreferencesKey("always_timestamps")
    }
}
