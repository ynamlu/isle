package com.isle.companion

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.isle.companion.data.local.IsleDatabase
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.data.local.entity.MemoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证多角色之间记忆严格隔离，且删除角色会级联清除其记忆。
 */
@RunWith(AndroidJUnit4::class)
class MemoryIsolationTest {

    @Test
    fun memoriesAreIsolatedPerCharacter() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, IsleDatabase::class.java).build()
        val characterDao = db.characterDao()
        val memoryDao = db.memoryDao()

        try {
            val first = characterDao.insert(newCharacter("小屿"))
            val second = characterDao.insert(newCharacter("阿泽"))

            memoryDao.insert(
                MemoryEntity(
                    characterId = first,
                    content = "记得你喜欢在周末爬山",
                    category = "preference"
                )
            )
            memoryDao.insert(
                MemoryEntity(
                    characterId = second,
                    content = "记得你最近在准备考试",
                    category = "event"
                )
            )

            val firstMemories = memoryDao.candidates(first, 10)
            val secondMemories = memoryDao.candidates(second, 10)

            assertEquals("第一个角色只应看到自己的记忆", 1, firstMemories.size)
            assertEquals("记得你喜欢在周末爬山", firstMemories.first().content)

            assertEquals("第二个角色只应看到自己的记忆", 1, secondMemories.size)
            assertEquals("记得你最近在准备考试", secondMemories.first().content)

            // 删除第一个角色后，其记忆应被级联清除，且不影响第二个角色
            characterDao.delete(characterDao.getById(first)!!)

            assertEquals("删除角色后其记忆应被清除", 0, memoryDao.candidates(first, 10).size)
            assertEquals("另一个角色的记忆不受影响", 1, memoryDao.candidates(second, 10).size)
        } finally {
            db.close()
        }
    }

    private fun newCharacter(name: String) = CharacterEntity(
        name = name,
        relationType = "friend",
        personality = "",
        bio = "",
        speakingStyle = "",
        greeting = ""
    )
}
