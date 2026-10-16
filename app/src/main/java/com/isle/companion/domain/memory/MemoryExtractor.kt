package com.isle.companion.domain.memory

import com.isle.companion.data.prefs.DeepSeekSettings
import com.isle.companion.data.prefs.UserProfile
import com.isle.companion.data.remote.ContentFactory
import com.isle.companion.data.remote.DeepSeekClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 从对话中抽取值得长期记住的事实。
 */
class MemoryExtractor(
    private val json: Json,
    private val client: DeepSeekClient
) {

    @Serializable
    data class ExtractedMemory(
        val content: String,
        val category: String = "basic",
        val importance: Float = 0.5f
    )

    suspend fun extract(
        settings: DeepSeekSettings,
        characterName: String,
        userText: String,
        assistantText: String,
        userProfile: UserProfile? = null
    ): List<ExtractedMemory> {
        if (userText.isBlank()) return emptyList()

        val system = "你是「$characterName」，正在和对方聊天。你要把对方告诉过你的事，" +
            "用温柔、亲近的口吻记在心里，像默默写在心里的那句话。"

        val profileHint = buildProfileHint(userProfile)

        val prompt = buildString {
            appendLine("角色：$characterName")
            if (profileHint.isNotBlank()) {
                appendLine(profileHint)
            }
            appendLine("对方说：$userText")
            appendLine("你的回应：$assistantText")
            appendLine()
            appendLine("请记下关于对方的、值得长期记住的事（身份、喜好、经历、重要事件、约定、情绪习惯）。")
            appendLine("要求：")
            appendLine("1. 用「你」称呼对方，写成一句温柔自然的话，像是你在心里默默记住的。")
            appendLine("2. 绝对不能出现「用户」这两个字，也不要写成冷冰冰的资料条目。")
            appendLine("3. 已知的信息不要重复记录，只记这次新出现的内容。")
            appendLine("4. 每条独立完整，能脱离上下文理解。")
            appendLine("5. 只记明确出现的信息，不要推测。")
            appendLine("6. 没有值得记住的就返回空数组 []。")
            appendLine("7. category 取值：basic / preference / event / relationship / promise。")
            appendLine()
            appendLine("示例：")
            appendLine("「记得你喜欢在周末去爬山，说起时语气都变轻快了」")
            appendLine("「知道你最近总加班到很晚，很心疼」")
            appendLine("「7 月 20 号是你的生日，我想给你一个惊喜」")
            appendLine()
            appendLine("只返回 JSON 数组，不要任何其他文字：")
            appendLine("[{\"content\":\"...\",\"category\":\"preference\",\"importance\":0.8}]")
        }

        val result = client.complete(
            settings = settings,
            messages = listOf(
                ContentFactory.systemMessage(system),
                ContentFactory.textMessage("user", prompt)
            )
        )

        return result.getOrNull()?.let(::parse).orEmpty()
    }

    /**
     * 已知的对方资料，供抽取时参考（避免重复记录、称呼更准确）。
     */
    private fun buildProfileHint(userProfile: UserProfile?): String {
        if (userProfile == null || !userProfile.isFilled) return ""
        return buildString {
            append("已知的对方信息：")
            val parts = mutableListOf<String>()
            if (userProfile.name.isNotBlank()) parts += "称呼「${userProfile.name}」"
            if (userProfile.personality.isNotBlank()) parts += "性格：${userProfile.personality}"
            if (userProfile.intro.isNotBlank()) parts += "自述：${userProfile.intro}"
            append(parts.joinToString("；"))
        }
    }

    private fun parse(raw: String): List<ExtractedMemory> {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        val jsonArray = raw.substring(start, end + 1)
        return runCatching {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(ExtractedMemory.serializer()),
                jsonArray
            )
        }.getOrDefault(emptyList())
            .filter { it.content.isNotBlank() }
            .take(MAX_ITEMS)
            .map { it.copy(importance = it.importance.coerceIn(0f, 1f)) }
    }

    companion object {
        private const val MAX_ITEMS = 5
    }
}
