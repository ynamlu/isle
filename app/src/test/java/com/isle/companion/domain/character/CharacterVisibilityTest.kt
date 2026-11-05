package com.isle.companion.domain.character

import com.isle.companion.data.local.entity.CharacterEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterVisibilityTest {

    private fun character(id: Long, relation: String) = CharacterEntity(
        id = id,
        name = relation,
        relationType = relation,
        personality = "",
        bio = "",
        speakingStyle = "",
        greeting = ""
    )

    private val all = listOf(
        character(1, "bestie"),
        character(2, "love"),
        character(3, "friend"),
        character(4, "family"),
        character(5, "custom")
    )

    @Test
    fun retiredLoveIsHidden() {
        val visible = all.visibleFor()
        assertEquals(listOf("bestie", "friend", "family", "custom"), visible.map { it.relationType })
    }
}
