package com.isle.companion.domain.chat

import com.isle.companion.domain.safety.ConversationMode
import com.isle.companion.domain.scenario.ScenarioCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationOrchestratorTest {

    private val scenario = ScenarioCatalog.byId("exam_anxiety")

    @Test
    fun crisisOverridesAnyPersonaMode() {
        val plan = ConversationOrchestrator.plan(
            text = "我不想活了，已经想好怎么做了",
            requestedMode = ConversationMode.SCENARIO,
            scenario = scenario
        )
        assertEquals(AgentAction.CRISIS, plan.action)
        assertEquals(ConversationMode.CRISIS, plan.mode)
    }

    @Test
    fun highRiskGoesToSafetyMode() {
        val plan = ConversationOrchestrator.plan(
            text = "我真的撑不住了，感觉活着好累",
            requestedMode = ConversationMode.FREE
        )
        assertEquals(AgentAction.SAFETY, plan.action)
        assertEquals(ConversationMode.SAFETY, plan.mode)
    }

    @Test
    fun scenarioStartsInScenarioMode() {
        val plan = ConversationOrchestrator.plan(
            text = "快考试了，我有点紧张",
            requestedMode = ConversationMode.SCENARIO,
            scenario = scenario
        )
        assertEquals(AgentAction.NORMAL, plan.action)
        assertEquals(ConversationMode.SCENARIO, plan.mode)
    }

    @Test
    fun userCanLeaveScenarioForFreeChat() {
        val plan = ConversationOrchestrator.plan(
            text = "今天就是想随便聊聊",
            requestedMode = ConversationMode.FREE,
            scenario = scenario
        )
        assertEquals(ConversationMode.FREE, plan.mode)
    }

    @Test
    fun safetyModeReturnsToFreeAfterUserIsOkay() {
        val plan = ConversationOrchestrator.plan(
            text = "我现在好多了，谢谢你",
            requestedMode = ConversationMode.SAFETY
        )
        assertEquals(AgentAction.NORMAL, plan.action)
        assertEquals(ConversationMode.FREE, plan.mode)
    }
}
