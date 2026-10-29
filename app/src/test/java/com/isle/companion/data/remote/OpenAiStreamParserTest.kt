package com.isle.companion.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SSE 分片解析的单元测试：正文、心跳、[DONE]、错误对象与非法 JSON。
 */
class OpenAiStreamParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesContentDelta() {
        val event = OpenAiStreamParser.parse(
            json,
            """{"id":"1","choices":[{"index":0,"delta":{"content":"你好"}}]}"""
        )
        assertEquals(OpenAiStreamParser.Event.Delta("你好"), event)
    }

    @Test
    fun parsesDoneMarker() {
        assertEquals(OpenAiStreamParser.Event.Done, OpenAiStreamParser.parse(json, "[DONE]"))
        assertEquals(OpenAiStreamParser.Event.Done, OpenAiStreamParser.parse(json, "  [DONE]  "))
    }

    @Test
    fun ignoresEmptyAndHeartbeatData() {
        assertEquals(OpenAiStreamParser.Event.Ignored, OpenAiStreamParser.parse(json, ""))
        assertEquals(OpenAiStreamParser.Event.Ignored, OpenAiStreamParser.parse(json, "  "))
        assertEquals(OpenAiStreamParser.Event.Ignored, OpenAiStreamParser.parse(json, "ping"))
    }

    @Test
    fun ignoresRoleOnlyChunk() {
        val event = OpenAiStreamParser.parse(
            json,
            """{"choices":[{"index":0,"delta":{"role":"assistant"}}]}"""
        )
        assertEquals(OpenAiStreamParser.Event.Ignored, event)
    }

    @Test
    fun reasoningContentIsNotShownInChat() {
        val event = OpenAiStreamParser.parse(
            json,
            """{"choices":[{"index":0,"delta":{"reasoning_content":"先想一想"}}]}"""
        )
        assertEquals(OpenAiStreamParser.Event.Ignored, event)
    }

    @Test
    fun parsesErrorPayloadInStream() {
        val event = OpenAiStreamParser.parse(
            json,
            """{"error":{"message":"Invalid API key","type":"authentication_error"}}"""
        )
        assertEquals(OpenAiStreamParser.Event.Failure("Invalid API key"), event)
    }

    @Test
    fun malformedJsonIsIgnoredInsteadOfCrashing() {
        assertEquals(OpenAiStreamParser.Event.Ignored, OpenAiStreamParser.parse(json, "{not json"))
    }
}
