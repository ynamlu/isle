package com.isle.companion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "isle_settings")

/**
 * 应用设置读写（DataStore）。
 *
 * 存储键沿用旧版命名（api_key / base_url / model），升级应用不会丢失已有配置。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
    }

    val settings: Flow<AiSettings> = context.settingsDataStore.data.map { prefs ->
        AiSettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            baseUrl = prefs[Keys.BASE_URL] ?: AiSettings.DEFAULT_BASE_URL,
            model = prefs[Keys.MODEL] ?: AiSettings.DEFAULT_MODEL,
            temperature = prefs[Keys.TEMPERATURE] ?: AiSettings.DEFAULT_TEMPERATURE,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: AiSettings.DEFAULT_MAX_TOKENS
        )
    }

    suspend fun updateApiKey(value: String) = context.settingsDataStore.edit { it[Keys.API_KEY] = value }

    suspend fun updateBaseUrl(value: String) = context.settingsDataStore.edit { it[Keys.BASE_URL] = value }

    suspend fun updateModel(value: String) = context.settingsDataStore.edit { it[Keys.MODEL] = value }

    suspend fun updateTemperature(value: Double) = context.settingsDataStore.edit { it[Keys.TEMPERATURE] = value }

    suspend fun updateMaxTokens(value: Int) =
        context.settingsDataStore.edit { it[Keys.MAX_TOKENS] = value.coerceIn(128, 8192) }
}
