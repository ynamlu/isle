package com.isle.companion.domain.journal

import com.isle.companion.data.local.entity.MessageEntity
import com.isle.companion.data.prefs.AiSettings
import com.isle.companion.data.remote.ContentFactory
import com.isle.companion.data.remote.OpenAiClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 一张情绪手账的核心文案。 */
@Serializable
data class JournalEntry(
    val quote: String,
    val title: String = "今天，允许自己慢一点",
    val emotion: String = "平静"
)

/**
 * 从一段对话中提炼一句值得收藏的话。
 * 失败时回退到最后一条助手回复，保证手账仍可生成。
 */
class JournalExtractor(
    private val json: Json,
    private val client: OpenAiClient
) {

    suspend fun extract(
        settings: AiSettings,
        characterName: String,
        messages: List<MessageEntity>
    ): JournalEntry {
        val transcript = messages
            .filter { it.content.isNotBlank() }
            .takeLast(20)
            .joinToString("\n") { message ->
                val role = if (message.role == "user") "我" else characterName
                "$role：${message.content.take(300)}"
            }

        if (transcript.isBlank()) return fallbackEntry(messages)

        val prompt = buildString {
            appendLine("下面是一段用户与「$characterName」的情绪支持对话。")
            appendLine("请从中提炼一句最适合被用户收藏的「金句」，像情绪手账上的一句话。")
            appendLine("要求：")
            appendLine("1. 优先从用户自己说过的话里提炼；如果用户没有合适表达，再从助手回复中提炼。")
            appendLine("2. 不超过 42 个汉字，保留原意，不要编造。")
            appendLine("3. 温柔、有力量，不说教，不使用 Markdown。")
            appendLine("4. 如果对话涉及自伤/自杀/危机，不要生成金句，quote 返回空字符串。")
            appendLine("5. 同时给出一个 8–16 字的标题和 2–4 字的情绪词。")
            appendLine()
            appendLine("对话：")
            appendLine(transcript)
            appendLine()
            appendLine("只返回 JSON，不要其他文字：")
            appendLine("{\"quote\":\"...\",\"title\":\"...\",\"emotion\":\"...\"}")
        }

        val result = client.complete(
            settings = settings,
            messages = listOf(
                ContentFactory.systemMessage("你是一位温柔、克制的情绪手账编辑。"),
                ContentFactory.textMessage("user", prompt)
            )
        )

        val parsed = result.getOrNull()?.let(::parse)
        if (parsed != null && parsed.quote.isNotBlank()) return parsed
        return fallbackEntry(messages)
    }

    private fun parse(raw: String): JournalEntry? {
        val text = raw
            .replace(Regex("(?is)<think.*?>.*?</think.*?>"), "")
            .replace(Regex("(?is)```(?:json)?\\s*"), "")
            .replace("```", "")
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            json.decodeFromString(JournalEntry.serializer(), text.substring(start, end + 1))
        }.getOrNull()?.let { entry ->
            entry.copy(
                quote = entry.quote.trim().take(60),
                title = entry.title.trim().ifBlank { "今天，允许自己慢一点" }.take(24),
                emotion = entry.emotion.trim().ifBlank { "平静" }.take(6)
            )
        }
    }

    fun fallback(messages: List<MessageEntity>): JournalEntry = fallbackEntry(messages)

    private fun fallbackEntry(messages: List<MessageEntity>): JournalEntry {
        val lastAssistant = messages.lastOrNull { it.role == "assistant" && it.content.isNotBlank() }
            ?.content
            ?.trim()
            .orEmpty()
        val quote = lastAssistant
            .split('。', '！', '？', '\n')
            .map { it.trim() }
            .firstOrNull { it.length in 8..60 }
            ?: "你不是不够好，只是太久没有被好好听见。"
        return JournalEntry(
            quote = quote,
            title = "把这句话，留给今天的自己",
            emotion = "被听见"
        )
    }
}
