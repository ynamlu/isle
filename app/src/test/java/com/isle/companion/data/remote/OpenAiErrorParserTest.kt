package com.isle.companion.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * OpenAI 兼容服务错误体解析的单元测试。
 */
class OpenAiErrorParserTest {

    @Test
    fun extractsNestedErrorMessage() {
        assertEquals(
            "Authentication Fails",
            OpenAiErrorParser.extractMessage(
                """{"error":{"message":"Authentication Fails","type":"authentication_error"}}"""
            )
        )
    }

    @Test
    fun extractsPlainStringError() {
        assertEquals(
            "rate limited",
            OpenAiErrorParser.extractMessage("""{"error":"rate limited"}""")
        )
    }

    @Test
    fun extractsTopLevelMessage() {
        assertEquals(
            "model not found",
            OpenAiErrorParser.extractMessage("""{"message":"model not found"}""")
        )
    }

    @Test
    fun nonJsonBodyReturnsNull() {
        assertNull(OpenAiErrorParser.extractMessage("Bad Gateway"))
        assertNull(OpenAiErrorParser.extractMessage(""))
        assertNull(OpenAiErrorParser.extractMessage(null))
    }

    @Test
    fun describeIncludesHttpCodeAndDetail() {
        assertEquals(
            "Authentication Fails（HTTP 401）",
            OpenAiErrorParser.describe(401, """{"error":{"message":"Authentication Fails"}}""", "请求失败")
        )
        assertEquals(
            "请求失败（HTTP 500）",
            OpenAiErrorParser.describe(500, "oops", "请求失败")
        )
    }
}
