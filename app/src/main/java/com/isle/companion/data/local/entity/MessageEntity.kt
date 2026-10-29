package com.isle.companion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 消息实体：某角色下的单条会话消息。
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterId"), Index("characterId", "timestamp")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val characterId: Long,
    /** 角色：user / assistant */
    val role: String,
    val content: String,
    /** 用户发送的图片路径（可选） */
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
