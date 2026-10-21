package io.github.mschache.karoopro.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "karoopro")

val SettingsJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

/** Settings stored as one JSON string, so new fields fall back to their defaults. */
class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    private val key = stringPreferencesKey("settings")

    val settings: StateFlow<Settings> = context.dataStore.data
        .map { prefs -> decode(prefs[key]) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            prefs[key] = SettingsJson.encodeToString(Settings.serializer(), transform(decode(prefs[key])).normalized())
        }
    }

    private fun decode(json: String?): Settings = try {
        json?.let { SettingsJson.decodeFromString(Settings.serializer(), it).normalized() } ?: Settings()
    } catch (e: Exception) {
        Log.w("KarooPRO", "Unreadable settings, using defaults", e)
        Settings()
    }
}
