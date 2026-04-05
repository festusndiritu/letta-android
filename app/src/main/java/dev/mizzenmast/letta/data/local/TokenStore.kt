package dev.mizzenmast.letta.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "letta_tokens"
)

/**
 * Stores JWT access + refresh tokens.
 *
 * Uses Jetpack DataStore (preferences). On Android 6+ the DataStore file sits
 * in the app's private data directory which is protected by the OS — no
 * additional encryption layer needed for tokens of this sensitivity level.
 *
 * EncryptedSharedPreferences was deprecated in Security-Crypto 1.1.0-alpha06
 * in favour of this approach.
 *
 * Blocking reads (isLoggedIn, accessToken getter) are intentional — they are
 * only called from MainActivity.onCreate before the UI is inflated, and from
 * the OkHttp interceptor which already runs on a background thread.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.tokenDataStore

    // ── synchronous reads (safe: called off main thread) ────────────────────

    var accessToken: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_ACCESS] }.firstOrNull()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_ACCESS] = value
                else prefs.remove(KEY_ACCESS)
            }
        }

    var refreshToken: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_REFRESH] }.firstOrNull()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_REFRESH] = value
                else prefs.remove(KEY_REFRESH)
            }
        }

    var userId: String?
        get() = runBlocking {
            dataStore.data.map { it[KEY_USER_ID] }.firstOrNull()
        }
        set(value) = runBlocking {
            dataStore.edit { prefs ->
                if (value != null) prefs[KEY_USER_ID] = value
                else prefs.remove(KEY_USER_ID)
            }
        }

    fun isLoggedIn(): Boolean = accessToken != null && refreshToken != null

    suspend fun clearSuspend() {
        dataStore.edit { it.clear() }
    }

    fun clear() = runBlocking { clearSuspend() }

    companion object {
        private val KEY_ACCESS  = stringPreferencesKey("access_token")
        private val KEY_REFRESH = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
    }
}