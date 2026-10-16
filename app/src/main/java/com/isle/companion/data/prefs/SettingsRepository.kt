package com.isle.companion.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.isle.companion.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "isle_settings")

/**
 * DeepSeek 接口配置。
 */
data class DeepSeekSettings(
    val apiKey: String = "",
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val temperature: Double = 0.8,
    val maxTokens: Int = 2048
) {
    /** 用户填写的 Key 优先；未填写时回退到构建时注入的演示 Key */
    val effectiveApiKey: String
        get() = apiKey.ifBlank { BuildConfig.DEMO_DEEPSEEK_API_KEY }

    val isConfigured: Boolean get() = effectiveApiKey.isNotBlank()

    /** 正在使用内置演示密钥（用户未自行填写） */
    val usingDemoKey: Boolean
        get() = apiKey.isBlank() && BuildConfig.DEMO_DEEPSEEK_API_KEY.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        /** DeepSeek-V4.1-Flash（原生多模态视觉理解） */
        const val DEFAULT_MODEL = "deepseek-flash"
    }
}

/**
 * 应用设置读写（DataStore）。
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = doublePreferencesKey("temperature")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
    }

    val settings: Flow<DeepSeekSettings> = context.settingsDataStore.data.map { prefs ->
        DeepSeekSettings(
            apiKey = prefs[Keys.API_KEY] ?: "",
            baseUrl = prefs[Keys.BASE_URL] ?: DeepSeekSettings.DEFAULT_BASE_URL,
            model = prefs[Keys.MODEL] ?: DeepSeekSettings.DEFAULT_MODEL,
            temperature = prefs[Keys.TEMPERATURE] ?: 0.8,
            maxTokens = prefs[Keys.MAX_TOKENS] ?: 2048
        )
    }

    suspend fun updateApiKey(value: String) = context.settingsDataStore.edit { it[Keys.API_KEY] = value }

    suspend fun updateBaseUrl(value: String) = context.settingsDataStore.edit { it[Keys.BASE_URL] = value }

    suspend fun updateModel(value: String) = context.settingsDataStore.edit { it[Keys.MODEL] = value }

    suspend fun updateTemperature(value: Double) = context.settingsDataStore.edit { it[Keys.TEMPERATURE] = value }
}
