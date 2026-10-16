package com.isle.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.isle.companion.data.local.entity.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE characterId = :characterId ORDER BY importance DESC, lastUsedAt DESC")
    fun observeByCharacter(characterId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE characterId = :characterId ORDER BY importance DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun candidates(characterId: Long, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE characterId = :characterId AND content = :content LIMIT 1")
    suspend fun findByContent(characterId: Long, content: String): MemoryEntity?

    @Insert
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE characterId = :characterId")
    suspend fun clearByCharacter(characterId: Long)
}
