package com.isle.companion.data.remote

import com.isle.companion.data.remote.dto.ChatCompletionChunk
import kotlinx.serialization.json.Json

/**
 * OpenAI 兼容协议的 SSE 分片解析。
 *
 * 纯函数实现，便于单元测试；只把模型真正输出的正文（delta.content）交给 UI，
 * 不把推理模型（如 deepseek-reasoner / o1）的 reasoning_content 混进聊天内容。
 */
object OpenAiStreamParser {

    sealed interface Event {
        /** `data: [DONE]`，流正常结束 */
        data object Done : Event

        /** 本次新增的正文文本 */
        data class Delta(val text: String) : Event

        /** 服务端在流中返回错误对象 */
        data class Failure(val message: String) : Event

        /** 空行、心跳或无可展示内容的分片 */
        data object Ignored : Event
    }

    fun parse(json: Json, data: String): Event {
        val payload = data.trim()
        if (payload.isEmpty()) return Event.Ignored
        if (payload == "[DONE]") return Event.Done

        val chunk = runCatching {
            json.decodeFromString(ChatCompletionChunk.serializer(), payload)
        }.getOrNull()

        if (chunk != null && chunk.choices.isNotEmpty()) {
            val content = chunk.choices.firstOrNull()?.delta?.content.orEmpty()
            return if (content.isEmpty()) Event.Ignored else Event.Delta(content)
        }

        // 空分片或部分服务在流中返回 {"error": {...}}
        val message = OpenAiErrorParser.extractMessage(payload)
        return if (message != null) Event.Failure(message) else Event.Ignored
    }
}
