package com.isle.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.isle.companion.data.local.entity.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE characterId = :characterId ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun recent(characterId: Long, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE characterId = :characterId")
    suspend fun deleteByCharacter(characterId: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT imagePath FROM messages WHERE imagePath IS NOT NULL")
    suspend fun allImagePaths(): List<String>

    @Query("SELECT imagePath FROM messages WHERE characterId = :characterId AND imagePath IS NOT NULL")
    suspend fun imagePathsByCharacter(characterId: Long): List<String>
}
