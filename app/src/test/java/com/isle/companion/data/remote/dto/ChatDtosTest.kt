package com.isle.companion.data.remote.dto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OpenAI 兼容协议 DTO 的序列化 / 反序列化测试。
 */
class ChatDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Test
    fun streamRequestSerializesOpenAiFields() {
        val request = ChatCompletionRequest(
            model = "gpt-4o-mini",
            messages = listOf(ApiMessage(role = "user", content = JsonPrimitive("hi"))),
            stream = true,
            temperature = 0.7,
            maxTokens = 256
        )
        val text = json.encodeToString(ChatCompletionRequest.serializer(), request)
        assertTrue(text.contains("\"model\":\"gpt-4o-mini\""))
        assertTrue(text.contains("\"stream\":true"))
        assertTrue(text.contains("\"max_tokens\":256"))
        assertTrue(text.contains("\"temperature\":0.7"))
        assertFalse(text.contains("balance"))
    }

    @Test
    fun completionResponseParsesContentAndUsage() {
        val raw = """
            {
              "id": "chatcmpl-1",
              "model": "deepseek-chat",
              "choices": [
                {"index": 0, "message": {"role": "assistant", "content": "你好"}, "finish_reason": "stop"}
              ],
              "usage": {"prompt_tokens": 10, "completion_tokens": 2, "total_tokens": 12}
            }
        """.trimIndent()
        val response = json.decodeFromString(ChatCompletionResponse.serializer(), raw)
        assertEquals("你好", response.choices.first().message?.content)
        assertEquals(12, response.usage?.totalTokens)
    }

    @Test
    fun modelListResponseParsesIds() {
        val raw = """{"object":"list","data":[{"id":"deepseek-chat"},{"id":"deepseek-reasoner"}]}"""
        val response = json.decodeFromString(ModelListResponse.serializer(), raw)
        assertEquals(listOf("deepseek-chat", "deepseek-reasoner"), response.data.map { it.id })
    }
}
