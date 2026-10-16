package com.isle.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.isle.companion.data.local.entity.MessageEntity

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE characterId = :characterId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(characterId: Long, limit: Int): List<MessageEntity>

    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Query("UPDATE messages SET content = :content WHERE id = :id")
    suspend fun updateContent(id: Long, content: String)
}
