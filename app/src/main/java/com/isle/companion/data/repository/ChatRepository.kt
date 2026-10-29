package com.isle.companion.data.repository

import com.isle.companion.data.local.dao.MessageDao
import com.isle.companion.data.local.entity.MessageEntity

class ChatRepository(private val dao: MessageDao) {

    suspend fun recent(characterId: Long, limit: Int = 12): List<MessageEntity> =
        dao.recent(characterId, limit).reversed()

    suspend fun addUserMessage(characterId: Long, content: String, imagePath: String? = null): Long =
        dao.insert(
            MessageEntity(
                characterId = characterId,
                role = "user",
                content = content,
                imagePath = imagePath
            )
        )

    suspend fun addAssistantMessage(characterId: Long, content: String = ""): Long =
        dao.insert(
            MessageEntity(
                characterId = characterId,
                role = "assistant",
                content = content
            )
        )

    suspend fun updateContent(id: Long, content: String) = dao.updateContent(id, content)

    suspend fun deleteMessage(id: Long) = dao.deleteById(id)

    suspend fun clear(characterId: Long) = dao.deleteByCharacter(characterId)

    suspend fun imagePaths(characterId: Long): List<String> = dao.imagePathsByCharacter(characterId)
}
