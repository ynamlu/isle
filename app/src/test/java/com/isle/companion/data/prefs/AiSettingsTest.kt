package com.isle.companion.data.prefs

import com.isle.companion.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 模型服务配置回退逻辑的单元测试。
 * 优先级：应用内填写 > local.properties 注入（BuildConfig）> 通用默认值。
 */
class AiSettingsTest {

    @Test
    fun userConfiguredValuesWin() {
        val settings = AiSettings(
            apiKey = "sk-user",
            baseUrl = "https://example.com/v1",
            model = "my-model"
        )
        assertEquals("sk-user", settings.effectiveApiKey)
        assertEquals("https://example.com/v1", settings.effectiveBaseUrl)
        assertEquals("my-model", settings.effectiveModel)
        assertFalse(settings.usingBuiltInKey)
    }

    @Test
    fun blankValuesFallBackToBuildConfigAndDefaults() {
        val settings = AiSettings()
        assertEquals(BuildConfig.DEMO_AI_API_KEY, settings.effectiveApiKey)
        assertEquals(AiSettings.DEFAULT_BASE_URL, settings.effectiveBaseUrl)
        assertEquals(AiSettings.DEFAULT_MODEL, settings.effectiveModel)
    }

    @Test
    fun builtInKeyIsUsedWhenUserKeyBlank() {
        val settings = AiSettings(apiKey = "")
        assertEquals(BuildConfig.DEMO_AI_API_KEY.isNotBlank(), settings.usingBuiltInKey)
    }

    @Test
    fun localEndpointDoesNotRequireApiKey() {
        val settings = AiSettings(
            apiKey = "",
            baseUrl = "http://127.0.0.1:11434/v1",
            model = "qwen2.5:7b"
        )
        assertTrue(settings.isLocalEndpoint)
        assertFalse(settings.requiresApiKey)
        assertTrue(settings.isConfigured)
    }

    @Test
    fun remoteEndpointRequiresApiKeyOrBuiltInKey() {
        val settings = AiSettings(apiKey = "", baseUrl = "https://api.openai.com/v1")
        assertFalse(settings.isLocalEndpoint)
        assertTrue(settings.requiresApiKey)
        // 本机构建是否注入 Key 决定最终是否已配置
        assertEquals(BuildConfig.DEMO_AI_API_KEY.isNotBlank(), settings.isConfigured)
    }

    @Test
    fun maxTokensDefaultsAreWithinAllowedRange() {
        assertTrue(AiSettings.DEFAULT_MAX_TOKENS in 128..8192)
        assertEquals(0.8, AiSettings.DEFAULT_TEMPERATURE, 0.0001)
    }
}
