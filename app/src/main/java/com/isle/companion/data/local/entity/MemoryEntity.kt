package com.isle.companion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 长期记忆实体：从对话中抽取的关键事实。
 */
@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId")]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: Long,
    /** 记忆内容（一句话事实） */
    val content: String,
    /** 类别：basic/preference/event/relationship/promise */
    val category: String,
    /** 重要度 0..1 */
    val importance: Float = 0.5f,
    /** 语义向量（端侧 embedding，序列化后的 float 数组） */
    val embedding: ByteArray? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEntity) return false
        return id == other.id &&
            characterId == other.characterId &&
            content == other.content &&
            category == other.category &&
            importance == other.importance &&
            createdAt == other.createdAt &&
            lastUsedAt == other.lastUsedAt &&
            (embedding?.contentEquals(other.embedding) ?: (other.embedding == null))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + characterId.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + importance.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastUsedAt.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        return result
    }
}
