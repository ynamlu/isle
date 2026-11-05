package com.isle.companion.domain.scenario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioCatalogTest {

    @Test
    fun catalogIsNotEmptyAndIdsAreUnique() {
        val all = ScenarioCatalog.all
        assertTrue(all.size >= 8)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.steps.isNotEmpty() && it.opening.isNotBlank() })
    }

    @Test
    fun byIdFindsScenario() {
        assertEquals("exam_anxiety", ScenarioCatalog.byId("exam_anxiety")?.id)
        assertEquals(null, ScenarioCatalog.byId("not-exist"))
    }
}
