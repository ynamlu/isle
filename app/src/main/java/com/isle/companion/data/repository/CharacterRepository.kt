package com.isle.companion.data.repository

import com.isle.companion.data.local.dao.CharacterDao
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.util.AvatarStorage
import kotlinx.coroutines.flow.Flow

class CharacterRepository(
    private val dao: CharacterDao,
    private val avatarStorage: AvatarStorage
) {
    fun observeAll(): Flow<List<CharacterEntity>> = dao.observeAll()

    suspend fun get(id: Long): CharacterEntity? = dao.getById(id)

    suspend fun save(character: CharacterEntity): Long {
        val now = System.currentTimeMillis()
        return if (character.id == 0L) {
            dao.insert(character.copy(updatedAt = now))
        } else {
            // 编辑时保留原 createdAt，否则 Room 会把整行覆盖成当前时间，
            // 角色会从列表原位跳到末尾。
            val existing = dao.getById(character.id)
            dao.update(
                character.copy(
                    createdAt = existing?.createdAt ?: character.createdAt,
                    updatedAt = now
                )
            )
            character.id
        }
    }

    suspend fun delete(character: CharacterEntity) {
        avatarStorage.delete(character.avatarPath)
        dao.delete(character)
    }

    /**
     * 首次启动时写入内置默认角色。
     */
    suspend fun ensureDefaultCharacter() {
        if (dao.count() > 0) return
        dao.insert(
            CharacterEntity(
                name = "小屿",
                relationType = "bestie",
                personality = "温柔、细心、有耐心，愿意倾听你的所有心事",
                bio = "住在心屿上的人，喜欢在傍晚的海边散步，也喜欢听你分享一天的喜怒哀乐",
                speakingStyle = "语气亲近自然，喜欢用「嗯嗯」「其实呀」这样的口语，会记得你说过的事",
                greeting = "嗨，你来啦～今天过得怎么样呀？",
                isDefault = true
            )
        )
    }
}
