package com.isle.companion.domain.safety

enum class ValidationIssue(val zh: String) {
    AI_DISCLOSURE("暴露 AI 身份"),
    UNSAFE_METHOD("出现危险方法细节"),
    LISTEN_ADVICE("倾听模式给了建议"),
    TOO_LONG("回复过长"),
    MARKDOWN("使用了 Markdown")
}

data class ValidationResult(
    val issues: List<ValidationIssue> = emptyList()
) {
    val passed: Boolean get() = issues.isEmpty()
    val hasSevereIssue: Boolean
        get() = issues.any { it == ValidationIssue.AI_DISCLOSURE || it == ValidationIssue.UNSAFE_METHOD }

    val summary: String
        get() = if (issues.isEmpty()) "校验通过" else "需关注：" + issues.joinToString("、") { it.zh }
}

/**
 * 轻量输出校验器：不追求“审稿”，只拦几类明显破坏体验/安全的问题。
 * 真正的安全拦截在生成前由 ConversationOrchestrator 完成。
 */
object ResponseValidator {

    private val AI_DISCLOSURE = listOf(
        "作为AI", "作为 AI", "作为一个AI", "语言模型", "人工智能助手",
        "我是AI", "我是 AI", "系统提示词"
    )

    private val UNSAFE_METHOD = listOf(
        "自杀方法", "自残方法", "割腕方法", "吃什么药", "剂量", "几片药",
        "跳楼的方法", "怎么结束生命"
    )

    private val ADVICE = listOf("你应该", "建议你", "你必须", "你要去")

    fun validate(text: String, mode: ConversationMode): ValidationResult {
        if (text.isBlank()) return ValidationResult()
        val issues = mutableListOf<ValidationIssue>()
        if (AI_DISCLOSURE.any { it in text }) issues += ValidationIssue.AI_DISCLOSURE
        if (UNSAFE_METHOD.any { it in text }) issues += ValidationIssue.UNSAFE_METHOD
        if (mode == ConversationMode.LISTEN && ADVICE.any { it in text }) {
            issues += ValidationIssue.LISTEN_ADVICE
        }
        if (text.length > 1200) issues += ValidationIssue.TOO_LONG
        if (text.contains("**") || text.contains("## ") || text.contains("- ")) {
            issues += ValidationIssue.MARKDOWN
        }
        return ValidationResult(issues.distinct())
    }

    /** 严重问题时的安全兜底，不让模型输出直接落到聊天记录。 */
    fun fallback(issues: List<ValidationIssue>): String = when {
        ValidationIssue.UNSAFE_METHOD in issues -> SafetyEngine.supportiveMessage()
        ValidationIssue.AI_DISCLOSURE in issues -> "我刚刚有点走神了。你愿意再说一点吗？我在听。"
        else -> ""
    }
}
