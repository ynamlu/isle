package com.isle.companion.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * OpenAI 兼容协议地址拼接的单元测试。
 * 覆盖 Base URL / host-only / 完整接口 / 自定义前缀 / 非法地址等写法。
 */
class OpenAiUrlsTest {

    @Test
    fun baseUrlWithVersion() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiUrls.chatCompletionsUrl("https://api.openai.com/v1")
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiUrls.chatCompletionsUrl("https://api.deepseek.com/v1/")
        )
    }

    @Test
    fun hostOnlyUrlGetsV1Prefix() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiUrls.chatCompletionsUrl("https://api.openai.com")
        )
        assertEquals(
            "http://10.0.2.2:11434/v1/chat/completions",
            OpenAiUrls.chatCompletionsUrl("http://10.0.2.2:11434")
        )
    }

    @Test
    fun fullEndpointIsNotDuplicated() {
        val full = "https://api.openai.com/v1/chat/completions"
        assertEquals(full, OpenAiUrls.chatCompletionsUrl(full))
        assertEquals(
            "https://api.openai.com/v1/models",
            OpenAiUrls.modelsUrl(full)
        )
    }

    @Test
    fun customPathPrefixIsPreserved() {
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            OpenAiUrls.chatCompletionsUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
        )
    }

    @Test
    fun modelsUrlFollowsSameRules() {
        assertEquals(
            "https://api.openai.com/v1/models",
            OpenAiUrls.modelsUrl("https://api.openai.com/v1/")
        )
        assertEquals(
            "http://192.168.1.5:8000/v1/models",
            OpenAiUrls.modelsUrl("http://192.168.1.5:8000")
        )
    }

    @Test
    fun normalizeTrimsWhitespaceAndDropsQuery() {
        assertEquals(
            "https://api.openai.com/v1",
            OpenAiUrls.normalizeBaseUrl("  https://api.openai.com/v1/  ")
        )
        assertEquals(
            "https://api.openai.com/v1",
            OpenAiUrls.normalizeBaseUrl("https://api.openai.com/v1?foo=bar")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankUrlRejected() {
        OpenAiUrls.chatCompletionsUrl("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun urlWithoutSchemeRejected() {
        OpenAiUrls.chatCompletionsUrl("api.openai.com/v1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonHttpSchemeRejected() {
        OpenAiUrls.chatCompletionsUrl("ftp://api.openai.com/v1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun urlWithoutHostRejected() {
        OpenAiUrls.chatCompletionsUrl("https:///v1")
    }

    @Test
    fun localEndpointsAreDetected() {
        assertTrue(OpenAiUrls.isLocalEndpoint("http://127.0.0.1:11434/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://localhost:8080/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://[::1]:11434/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://10.0.2.2:11434/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://10.1.2.3/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://192.168.1.20:8000/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://172.16.0.8/v1"))
        assertTrue(OpenAiUrls.isLocalEndpoint("http://172.31.255.1/v1"))
    }

    @Test
    fun publicEndpointsAreNotLocal() {
        assertFalse(OpenAiUrls.isLocalEndpoint("https://api.openai.com/v1"))
        assertFalse(OpenAiUrls.isLocalEndpoint("https://api.deepseek.com/v1"))
        assertFalse(OpenAiUrls.isLocalEndpoint("http://172.32.0.1/v1"))
        assertFalse(OpenAiUrls.isLocalEndpoint("http://8.8.8.8/v1"))
    }
}
