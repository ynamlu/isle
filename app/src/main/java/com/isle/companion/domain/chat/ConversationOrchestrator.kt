package com.isle.companion.domain.chat

import com.isle.companion.domain.safety.ConversationMode
import com.isle.companion.domain.safety.RiskLevel
import com.isle.companion.domain.safety.SafetyAssessment
import com.isle.companion.domain.safety.SafetyEngine
import com.isle.companion.domain.scenario.Scenario

/** 路由动作：正常回复 / 安全确认 / 危机支持。 */
enum class AgentAction { NORMAL, SAFETY, CRISIS }

data class AgentPlan(
    val assessment: SafetyAssessment,
    val mode: ConversationMode,
    val action: AgentAction,
    val trace: String
)

/**
 * 多智能体编排器（轻量版）：
 * - 情绪/风险检测：SafetyEngine（本地规则优先）
 * - 安全策略路由：确定性状态机，不让模型自由决定
 * - 人设生成：由 PromptBuilder 根据 mode/scenario/assessment 组装
 *
 * 这样高危场景不会被角色扮演掩盖，日常场景也不会因为安全逻辑变得像客服。
 */
object ConversationOrchestrator {

    fun plan(
        text: String,
        requestedMode: ConversationMode,
        scenario: Scenario? = null
    ): AgentPlan {
        val assessment = SafetyEngine.assess(text)
        val requested = requestedMode

        return when (assessment.risk) {
            RiskLevel.CRISIS -> AgentPlan(
                assessment = assessment,
                mode = ConversationMode.CRISIS,
                action = AgentAction.CRISIS,
                trace = trace(assessment, ConversationMode.CRISIS, "危机支持")
            )

            RiskLevel.HIGH -> AgentPlan(
                assessment = assessment,
                mode = ConversationMode.SAFETY,
                action = AgentAction.SAFETY,
                trace = trace(assessment, ConversationMode.SAFETY, "安全确认")
            )

            RiskLevel.WATCH, RiskLevel.NONE -> {
                val effective = if (requested == ConversationMode.CRISIS || requested == ConversationMode.SAFETY) {
                    ConversationMode.FREE
                } else {
                    requested
                }
                AgentPlan(
                    assessment = assessment,
                    mode = effective,
                    action = AgentAction.NORMAL,
                    trace = trace(
                        assessment,
                        effective,
                        if (effective == ConversationMode.SCENARIO && scenario != null) {
                            "场景引导：${scenario.title}"
                        } else {
                            "角色回复"
                        }
                    )
                )
            }
        }
    }

    private fun trace(
        assessment: SafetyAssessment,
        mode: ConversationMode,
        route: String
    ): String = buildString {
        append("情绪：${assessment.emotion.zh}")
        append(" · 风险：${assessment.risk.name}")
        append(" · 路由：$route")
        append(" · 模式：${mode.label}")
    }
}
