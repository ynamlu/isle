package com.isle.companion.data.repository

import com.isle.companion.data.local.dao.MemoryDao
import com.isle.companion.data.local.entity.MemoryEntity
import com.isle.companion.domain.memory.EmbeddingProvider
import com.isle.companion.domain.memory.VectorMath
import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val dao: MemoryDao,
    private val embeddingProvider: EmbeddingProvider
) {

    fun observe(characterId: Long): Flow<List<MemoryEntity>> = dao.observeByCharacter(characterId)

    /**
     * 写入记忆：自动补充语义向量。内容重复时只更新最近使用时间。
     */
    suspend fun add(
        characterId: Long,
        content: String,
        category: String = "basic",
        importance: Float = 0.5f
    ) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        val existing = dao.findByContent(characterId, trimmed)
        if (existing != null) {
            dao.update(existing.copy(lastUsedAt = System.currentTimeMillis()))
            return
        }

        val embedding = embeddingProvider.embed(trimmed)
        if (embedding != null) {
            // 语义近似去重：同一件事换种说法时不重复写入，只刷新使用时间/重要性
            val nearDuplicate = dao.allByCharacter(characterId).firstOrNull { existing ->
                val vector = VectorMath.fromBytes(existing.embedding)
                vector != null && VectorMath.cosine(embedding, vector) >= SEMANTIC_DUPLICATE_THRESHOLD
            }
            if (nearDuplicate != null) {
                dao.update(
                    nearDuplicate.copy(
                        lastUsedAt = System.currentTimeMillis(),
                        importance = maxOf(nearDuplicate.importance, importance)
                    )
                )
                return
            }
        }

        dao.insert(
            MemoryEntity(
                characterId = characterId,
                content = trimmed,
                category = category,
                importance = importance,
                embedding = embedding?.let(VectorMath::toBytes)
            )
        )
    }

    suspend fun delete(memory: MemoryEntity) = dao.delete(memory)

    suspend fun clear(characterId: Long) = dao.clearByCharacter(characterId)

    /**
     * 召回与当前输入最相关的记忆。
     * 优先使用端侧语义向量；无向量时回退到关键词匹配。
     */
    suspend fun retrieveRelevant(
        characterId: Long,
        query: String,
        limit: Int = 6
    ): List<MemoryEntity> {
        val pool = dao.candidates(characterId, CANDIDATE_LIMIT)
        if (pool.isEmpty()) return emptyList()
        if (query.isBlank()) return pool.take(limit)

        val queryVector = embeddingProvider.embed(query)

        val scored = pool.map { memory ->
            val vector = VectorMath.fromBytes(memory.embedding)
            val semantic = if (queryVector != null && vector != null) {
                VectorMath.cosine(queryVector, vector)
            } else {
                0f
            }
            val lexical = lexicalScore(memory.content, query)
            memory to (semantic * 0.8f + lexical * 0.2f + memory.importance * 0.1f)
        }

        return scored
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun lexicalScore(content: String, query: String): Float {
        val grams = query
            .filter { it.isLetterOrDigit() || it.code > 0x2E80 }
            .windowed(2, 1)
        if (grams.isEmpty()) return 0f
        return grams.count { content.contains(it) }.toFloat() / grams.size
    }

    companion object {
        private const val CANDIDATE_LIMIT = 200
        private const val SEMANTIC_DUPLICATE_THRESHOLD = 0.92f
    }
}
