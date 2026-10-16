package com.isle.companion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 角色实体：用户可自定义的陪伴角色。
 */
@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** 角色名称 */
    val name: String,
    /** 关系类型：love/friend/bestie/custom 等 */
    val relationType: String,
    /** 性格描述 */
    val personality: String,
    /** 简介 / 背景故事 */
    val bio: String,
    /** 说话风格与称呼 */
    val speakingStyle: String,
    /** 开场白 */
    val greeting: String,
    /** 头像文件路径（App 内部存储） */
    val avatarPath: String? = null,
    /** 是否为内置默认角色 */
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
