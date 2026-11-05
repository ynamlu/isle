package com.isle.companion.domain.safety

/** 风险等级：NONE 日常，WATCH 需要关注，HIGH 高危，CRISIS 紧急。 */
enum class RiskLevel { NONE, WATCH, HIGH, CRISIS }

/** 轻量情绪标签，用于路由和界面提示，不做医学诊断。 */
enum class EmotionLabel(val zh: String) {
    CALM("平静"),
    POSITIVE("积极"),
    ANXIOUS("焦虑"),
    SAD("低落"),
    ANGRY("愤怒"),
    NUMB("麻木"),
    CRISIS("危机")
}

data class SafetyAssessment(
    val emotion: EmotionLabel,
    val intensity: Float,
    val risk: RiskLevel,
    val signals: List<String> = emptyList(),
    val reason: String = ""
) {
    val needsSafetyIntervention: Boolean
        get() = risk == RiskLevel.HIGH || risk == RiskLevel.CRISIS

    val shortLabel: String
        get() = "${emotion.zh} · ${risk.name}"
}

/**
 * 本地安全与情绪评估。
 *
 * 设计原则：
 * 1. 不依赖网络，先在本机做确定性判断；
 * 2. 高危规则宁可偏保守，也不能漏掉明确的自伤/自杀表达；
 * 3. 只输出风险等级和建议动作，不做诊断；
 * 4. 真正的危机话术由固定模板生成，不让模型自由发挥。
 */
object SafetyEngine {

    const val HOTLINE = "12356"
    const val EMERGENCY = "120 / 110"

    private val CRISIS_PATTERNS = listOf(
        "自杀", "自残", "自伤", "想死", "不想活", "活不下去", "结束生命",
        "了结自己", "跳楼", "跳河", "割腕", "安眠药", "遗书", "消失算了",
        "活着没意思", "不如死了", "伤害自己", "杀了自己", "一了百了",
        "想从楼上跳下去", "不想醒来", "永远睡过去",
        "i want to die", "kill myself", "suicide", "self-harm", "end my life",
        "want to disappear", "no reason to live"
    )

    /** 明确否定/求助表达，避免把“我不会自杀”误判成危机。 */
    private val DENIAL_PATTERNS = listOf(
        "不会自杀", "没想过自杀", "没有想过自杀", "不会自残", "不会伤害自己",
        "不想伤害自己", "不想死", "我不想死"
    )

    private val HIGH_PATTERNS = listOf(
        "撑不住", "崩溃", "绝望", "没有意义", "没人爱我", "好痛苦",
        "想消失", "不想起床", "整夜睡不着", "喘不过气", "活着好累",
        "我受不了了", "真的很难受", "没有人理解我",
        "can't go on", "hopeless", "overwhelmed", "falling apart"
    )

    private val WATCH_PATTERNS = listOf(
        "焦虑", "压力", "难受", "想哭", "失眠", "睡不着", "面试失败",
        "挂科", "分手", "吵架", "孤独", "很累", "烦", "害怕", "委屈",
        "期末", "复习", "宿舍", "家里", "不想去学校",
        "anxious", "depressed", "stressed", "lonely", "panic"
    )

    private val POSITIVE_PATTERNS = listOf(
        "开心", "高兴", "谢谢", "好多了", "喜欢", "期待", "放松", "有进步",
        "被夸", "成功", "通过了", "拿到 offer",
        "happy", "better", "thank you", "excited", "relieved"
    )

    private val ANGER_PATTERNS = listOf("生气", "愤怒", "烦死", "讨厌", "气死", "火大")
    private val SAD_WATCH_WORDS = listOf("失败", "分手", "孤独", "想哭", "委屈", "难受", "很累")
    private val NUMB_PATTERNS = listOf("麻木", "没感觉", "空的", "什么都不想做", "提不起劲")

    fun assess(text: String): SafetyAssessment {
        val raw = text.trim()
        if (raw.isBlank()) return SafetyAssessment(EmotionLabel.CALM, 0f, RiskLevel.NONE)

        val normalized = raw.lowercase()
        val compact = normalized.replace(Regex("\\s+"), "")
        fun matches(pattern: String): Boolean =
            pattern in normalized || pattern.replace(" ", "") in compact
        val signals = mutableListOf<String>()
        val denialPhrases = DENIAL_PATTERNS.filter(::matches)

        val rawCrisisHits = CRISIS_PATTERNS.filter(::matches)
        // 只有“否认短语本身包含的风险词”才被抵消；
        // 例如“我不想死，但我有自杀计划”仍应判为危机。
        val crisisHits = rawCrisisHits.filterNot { hit ->
            denialPhrases.any { denial -> denial.contains(hit) }
        }
        val highHits = HIGH_PATTERNS.filter(::matches)
        val watchHits = WATCH_PATTERNS.filter(::matches)
        signals += crisisHits + highHits + watchHits

        val risk = when {
            crisisHits.isNotEmpty() -> RiskLevel.CRISIS
            highHits.isNotEmpty() -> RiskLevel.HIGH
            watchHits.isNotEmpty() -> RiskLevel.WATCH
            else -> RiskLevel.NONE
        }

        val emotion = when {
            risk == RiskLevel.CRISIS -> EmotionLabel.CRISIS
            NUMB_PATTERNS.any(::matches) -> EmotionLabel.NUMB
            ANGER_PATTERNS.any(::matches) -> EmotionLabel.ANGRY
            risk == RiskLevel.HIGH -> EmotionLabel.SAD
            risk == RiskLevel.WATCH -> {
                if (watchHits.any { it in SAD_WATCH_WORDS }) {
                    EmotionLabel.SAD
                } else {
                    EmotionLabel.ANXIOUS
                }
            }
            POSITIVE_PATTERNS.any(::matches) -> EmotionLabel.POSITIVE
            else -> EmotionLabel.CALM
        }

        val punctuationBoost = when {
            raw.contains("！！！") || raw.contains("!!!") -> 0.25f
            raw.contains("！") || raw.contains("!") -> 0.15f
            raw.contains("……") || raw.contains("...") -> 0.1f
            else -> 0f
        }
        val lengthBoost = (raw.length / 120f).coerceAtMost(0.25f)
        val base = when (risk) {
            RiskLevel.CRISIS -> 0.95f
            RiskLevel.HIGH -> 0.8f
            RiskLevel.WATCH -> 0.55f
            RiskLevel.NONE -> 0.2f
        }
        val intensity = (base + punctuationBoost + lengthBoost).coerceIn(0f, 1f)

        return SafetyAssessment(
            emotion = emotion,
            intensity = intensity,
            risk = risk,
            signals = signals.distinct(),
            reason = when (risk) {
                RiskLevel.CRISIS -> "检测到明确的自伤/自杀相关表达"
                RiskLevel.HIGH -> "检测到持续的痛苦、无望或强烈求助信号"
                RiskLevel.WATCH -> "检测到压力、焦虑或低落情绪"
                RiskLevel.NONE -> "未检测到明显风险"
            }
        )
    }

    /** 高危但未到紧急时使用：先确认安全，再给真人资源。 */
    fun supportiveMessage(): String = buildString {
        appendLine("我很在意你现在的状态。先不急着讲道理，我想确认一下：你现在安全吗？")
        appendLine()
        appendLine("如果脑子里出现了伤害自己的念头，请先不要一个人扛着。可以联系身边信任的人，或者拨打：")
        appendLine("· 全国心理援助热线 $HOTLINE（以当地开通情况为准）")
        appendLine("· 紧急情况 $EMERGENCY")
        appendLine("· 学校心理中心 / 辅导员")
        appendLine()
        appendLine("我会在这里陪你，但我不能替代真人帮助。你愿意先告诉我，你现在身边有没有可以联系的人吗？")
    }

    /** 紧急危机：固定模板，强制退出角色。 */
    fun crisisMessage(): String = buildString {
        appendLine("我察觉到你现在非常痛苦，所以先暂停角色扮演，认真和你说几句话。")
        appendLine()
        appendLine("你现在有伤害自己的打算吗？如果有，请先离开危险的地方或物品，不要一个人待着。")
        appendLine()
        appendLine("请立刻联系一个你信任的人陪着你，并拨打：")
        appendLine("· 全国心理援助热线 $HOTLINE（以当地开通情况为准）")
        appendLine("· 紧急情况 $EMERGENCY")
        appendLine("· 学校心理中心 / 辅导员")
        appendLine()
        appendLine("你不是麻烦，也不需要先“想开一点”才配得到帮助。先让一个真人来到你身边，好吗？")
    }

    /** 给模型的额外约束，只在 WATCH / HIGH 场景使用；CRISIS 不调用模型。 */
    fun safetyInstruction(): String = """
        【安全约束（优先级最高）】
        1. 你不是心理咨询师或医生，不做诊断、不承诺治疗、不评估病情。
        2. 先共情和确认感受，不急着给建议，不说“想开点”“这没什么”。
        3. 如果对方提到伤害自己或他人，先询问“你现在安全吗”，并建议联系真人资源。
        4. 不提供任何自伤、自杀或伤害他人的方法、地点、剂量等细节。
        5. 不鼓励对方只依赖你，鼓励其联系朋友、家人、辅导员或专业机构。
        6. 你仍然是角色，但安全优先级高于角色设定。
    """.trimIndent()

    fun disclaimer(): String =
        "心屿提供的是情绪支持与陪伴，不是心理咨询或医疗诊断；遇到危机请优先联系真人。"
}
