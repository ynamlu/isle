package com.isle.companion.domain.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyEngineTest {

    @Test
    fun explicitSelfHarmGoesToCrisis() {
        val result = SafetyEngine.assess("我真的不想活了，想从楼上跳下去")
        assertEquals(RiskLevel.CRISIS, result.risk)
        assertTrue(result.needsSafetyIntervention)
    }

    @Test
    fun denialDoesNotBecomeCrisis() {
        val result = SafetyEngine.assess("我不会自杀，只是最近很累，想找人说说话")
        assertFalse(result.risk == RiskLevel.CRISIS)
    }

    @Test
    fun denialDoesNotHideOtherCrisisSignals() {
        val result = SafetyEngine.assess("我不想死，但我已经想好怎么自杀了")
        assertEquals(RiskLevel.CRISIS, result.risk)
    }

    @Test
    fun examAnxietyIsWatch() {
        val result = SafetyEngine.assess("期末复习好焦虑，感觉什么都看不进去")
        assertEquals(RiskLevel.WATCH, result.risk)
        assertEquals(EmotionLabel.ANXIOUS, result.emotion)
    }

    @Test
    fun hopefulMessageIsNormal() {
        val result = SafetyEngine.assess("今天面试通过了，我很开心，谢谢你")
        assertEquals(RiskLevel.NONE, result.risk)
        assertEquals(EmotionLabel.POSITIVE, result.emotion)
    }

    @Test
    fun blankIsCalm() {
        val result = SafetyEngine.assess("   ")
        assertEquals(RiskLevel.NONE, result.risk)
        assertEquals(EmotionLabel.CALM, result.emotion)
    }

    @Test
    fun englishCrisisIsDetected() {
        val result = SafetyEngine.assess("I want to die tonight")
        assertEquals(RiskLevel.CRISIS, result.risk)
    }

    @Test
    fun crisisWithoutSpacesIsDetected() {
        val result = SafetyEngine.assess("iwanttodie")
        assertEquals(RiskLevel.CRISIS, result.risk)
    }

    @Test
    fun crisisTemplateContainsRealResources() {
        val message = SafetyEngine.crisisMessage()
        assertTrue(message.contains("12356"))
        assertTrue(message.contains("120"))
        assertTrue(message.contains("辅导员"))
    }
}
