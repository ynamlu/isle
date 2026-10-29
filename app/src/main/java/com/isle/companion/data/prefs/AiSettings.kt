package com.isle.companion.data.prefs

import com.isle.companion.BuildConfig
import com.isle.companion.data.remote.OpenAiUrls

/**
 * 模型服务配置（OpenAI 兼容协议）。
 *
 * 兼容 OpenAI / DeepSeek / 通义千问 / Moonshot / 本地 Ollama、vLLM、LM Studio 等
 * 实现了 `/chat/completions` 的服务。优先级：
 *
 *   应用内设置  >  local.properties 构建注入（BuildConfig）  >  通用默认值
 *
 * 地址支持三种写法：`https://host/v1`、`https://host`、`https://host/v1/chat/completions`。
 */
data class AiSettings(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val temperature: Double = DEFAULT_TEMPERATURE,
    val maxTokens: Int = DEFAULT_MAX_TOKENS
) {
    /** 用户填写的 Key 优先；未填写时回退到 local.properties 注入的 Key */
    val effectiveApiKey: String
        get() = apiKey.ifBlank { BuildConfig.DEMO_AI_API_KEY }

    /** 用户填写的地址优先；未填写时回退到内置地址（再回退到 OpenAI 官方地址） */
    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { DEFAULT_BASE_URL }

    /** 用户填写的模型优先；未填写时回退到内置模型（再回退到通用默认模型） */
    val effectiveModel: String
        get() = model.ifBlank { DEFAULT_MODEL }

    /** 本地 / 局域网服务通常不校验 Key，可以留空 */
    val isLocalEndpoint: Boolean
        get() = OpenAiUrls.isLocalEndpoint(effectiveBaseUrl)

    val requiresApiKey: Boolean get() = !isLocalEndpoint

    val isConfigured: Boolean
        get() = effectiveBaseUrl.isNotBlank() &&
            (effectiveApiKey.isNotBlank() || !requiresApiKey)

    /** 正在使用 local.properties 注入的内置密钥（用户未自行填写） */
    val usingBuiltInKey: Boolean
        get() = apiKey.isBlank() && BuildConfig.DEMO_AI_API_KEY.isNotBlank()

    companion object {
        /** 构建时从 local.properties 读取；未配置则使用 OpenAI 官方地址作为示例 */
        val DEFAULT_BASE_URL: String =
            BuildConfig.DEMO_AI_BASE_URL.ifBlank { "https://api.openai.com/v1" }

        /** 构建时从 local.properties 读取；未配置则使用通用默认模型 */
        val DEFAULT_MODEL: String =
            BuildConfig.DEMO_AI_MODEL.ifBlank { "gpt-4o-mini" }

        const val DEFAULT_TEMPERATURE: Double = 0.8
        const val DEFAULT_MAX_TOKENS: Int = 2048
    }
}
