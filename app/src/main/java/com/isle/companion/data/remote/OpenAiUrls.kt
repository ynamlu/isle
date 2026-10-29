package com.isle.companion.data.remote

import java.net.URI
import java.util.Locale

/**
 * OpenAI 兼容协议的接口地址工具。
 *
 * 运营商常见的 Base URL 写法都能正确处理：
 *   1. `https://api.openai.com/v1`                         → /v1/chat/completions
 *   2. `https://api.openai.com`（不带版本）                → /v1/chat/completions
 *   3. `https://api.openai.com/v1/chat/completions`（完整）→ 原样使用
 *   4. `https://host/compatible-mode/v1`（自定义前缀）     → 保留前缀后拼接
 *
 * 所有方法都是纯函数，方便单元测试。
 */
object OpenAiUrls {

    private const val CHAT_PATH = "/chat/completions"
    private const val MODELS_PATH = "/models"

    /** 对话补全接口地址 */
    fun chatCompletionsUrl(baseUrl: String): String = endpointUrl(baseUrl, CHAT_PATH)

    /** 模型列表接口地址（用于低成本探活） */
    fun modelsUrl(baseUrl: String): String = endpointUrl(baseUrl, MODELS_PATH)

    /**
     * 归一化 Base URL：去掉首尾空白、末尾斜杠与 query/fragment，并校验 http(s) 与主机名。
     *
     * @throws IllegalArgumentException 地址为空或格式非法（调用方应转为用户可读提示）
     */
    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        require(trimmed.isNotEmpty()) { "API 地址不能为空" }
        val uri = runCatching { URI(trimmed) }
            .getOrElse { throw IllegalArgumentException("API 地址格式不正确") }
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        require(scheme == "http" || scheme == "https") {
            "API 地址需以 http:// 或 https:// 开头"
        }
        require(!uri.host.isNullOrBlank()) { "API 地址缺少主机名" }
        val path = uri.rawPath.orEmpty().trimEnd('/')
        return buildString {
            append(scheme).append("://").append(uri.rawAuthority)
            if (path.isNotEmpty() && path != "/") append(path)
        }
    }

    /** 是否为本地 / 局域网地址（此类 OpenAI 兼容服务通常无需 API Key） */
    fun isLocalEndpoint(baseUrl: String): Boolean {
        val host = runCatching { URI(normalizeBaseUrl(baseUrl)).host }
            .getOrNull()
            ?.lowercase(Locale.ROOT)
            ?: return false
        return host == "localhost" ||
            host == "127.0.0.1" ||
            host == "::1" ||
            host == "[::1]" ||
            host == "10.0.2.2" || // Android 模拟器访问宿主机
            host.startsWith("10.") ||
            host.startsWith("192.168.") ||
            Regex("^172\\.(1[6-9]|2\\d|3[01])\\.").containsMatchIn(host)
    }

    private fun endpointUrl(baseUrl: String, suffix: String): String {
        var normalized = normalizeBaseUrl(baseUrl)
        // 用户可能直接粘贴了完整接口地址，先剥掉已知后缀再统一拼接
        if (normalized.endsWith(CHAT_PATH)) {
            normalized = normalized.removeSuffix(CHAT_PATH)
        } else if (normalized.endsWith(MODELS_PATH)) {
            normalized = normalized.removeSuffix(MODELS_PATH)
        }
        // 只有主机名时补上 /v1，符合 OpenAI 协议的默认约定
        val path = URI(normalized).rawPath.orEmpty()
        if (path.isEmpty() || path == "/") {
            normalized = "$normalized/v1"
        }
        return normalized + suffix
    }
}
