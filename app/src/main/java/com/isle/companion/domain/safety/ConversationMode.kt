package com.isle.companion.domain.safety

/** 对话模式：决定系统提示词与快捷入口。 */
enum class ConversationMode(val label: String, val hint: String) {
    FREE("自由聊", "想说什么都可以"),
    LISTEN("听我吐槽", "只倾听，不打断，不评价"),
    ANALYZE("帮我分析", "一起把事实和情绪分开"),
    BREATHE("陪我呼吸", "先慢下来，做一次 30 秒呼吸"),
    SCENARIO("场景引导", "跟着一个轻量的小步骤走"),
    SAFETY("安全模式", "先确认你的安全，再谈其他"),
    CRISIS("危机支持", "角色扮演已暂停，先联系真人")
}
