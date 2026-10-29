package com.isle.companion.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * OpenAI 兼容服务的错误体解析。
 *
 * 常见形态：`{"error":{"message":"...","type":"...","code":"..."}}`，
 * 也兼容普通文本与 `{"message":"..."}`。
 */
object OpenAiErrorParser {

    /** 从错误响应体 / SSE data 中提取可展示的错误信息 */
    fun extractMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val root = runCatching { Json.Default.parseToJsonElement(raw) as? JsonObject }.getOrNull()
            ?: return null
        val error = root["error"]
        val message = when (error) {
            is JsonObject -> error.stringField("message") ?: error.stringField("code")
            is JsonPrimitive -> error.contentOrNull
            else -> root.stringField("message")
        }
        return message?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** 生成带 HTTP 状态码的用户可读错误；拿不到具体信息时退回 fallback */
    fun describe(code: Int, responseBody: String?, fallback: String): String {
        val detail = extractMessage(responseBody)
        return if (detail.isNullOrBlank()) "$fallback（HTTP $code）" else "$detail（HTTP $code）"
    }

    private fun JsonObject.stringField(name: String): String? =
        runCatching { this[name]?.jsonPrimitive?.contentOrNull }.getOrNull()
}
