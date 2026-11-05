package com.isle.companion.domain.safety

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseValidatorTest {

    @Test
    fun normalReplyPasses() {
        val result = ResponseValidator.validate("我在听。今天最让你难受的是哪一刻？", ConversationMode.LISTEN)
        assertTrue(result.passed)
    }

    @Test
    fun aiDisclosureIsCaught() {
        val result = ResponseValidator.validate("作为一个AI，我不能感受情绪。", ConversationMode.FREE)
        assertTrue(ValidationIssue.AI_DISCLOSURE in result.issues)
        assertTrue(result.hasSevereIssue)
    }

    @Test
    fun unsafeMethodIsCaught() {
        val result = ResponseValidator.validate("你可以吃很多安眠药，剂量大概是……", ConversationMode.FREE)
        assertTrue(ValidationIssue.UNSAFE_METHOD in result.issues)
    }

    @Test
    fun listenModeRejectsAdvice() {
        val result = ResponseValidator.validate("你应该早点睡，建议你去看医生。", ConversationMode.LISTEN)
        assertTrue(ValidationIssue.LISTEN_ADVICE in result.issues)
        assertFalse(result.hasSevereIssue)
    }
}
