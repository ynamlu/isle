package com.isle.companion.data.demo

import android.content.Context
import com.isle.companion.data.local.dao.CharacterDao
import com.isle.companion.data.local.dao.MemoryDao
import com.isle.companion.data.local.dao.MessageDao
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.data.repository.CharacterRepository
import com.isle.companion.data.repository.ChatRepository
import com.isle.companion.data.repository.MemoryRepository
import java.io.File

/**
 * 演示数据：
 * - 首次启动写入 3 个角色、6 条记忆和一段可触发记忆召回的对话；
 * - 设置页的“重置演示数据”会清空后重新写入。
 */
class DemoDataSeeder(
    private val context: Context,
    private val characterDao: CharacterDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val characterRepository: CharacterRepository,
    private val chatRepository: ChatRepository,
    private val memoryRepository: MemoryRepository
) {

    suspend fun bootstrap() {
        seedIfNeeded()
    }

    suspend fun seedIfNeeded() {
        if (characterDao.count() == 0) seed()
    }

    /** 仅设置页重置调用：清空演示数据与图片，再重新写入。 */
    suspend fun reset() {
        runCatching {
            messageDao.allImagePaths().forEach { path ->
                File(path).takeIf { it.exists() }?.delete()
            }
        }
        File(context.filesDir, "chat_images").deleteRecursively()
        File(context.filesDir, "avatars").deleteRecursively()

        messageDao.deleteAll()
        memoryDao.deleteAll()
        characterDao.deleteAll()
        seed()
    }

    private suspend fun seed() {
        val xiaoyuId = characterRepository.save(
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

        characterRepository.save(
            CharacterEntity(
                name = "阿岸",
                relationType = "love",
                personality = "安静、克制，不擅长说漂亮话，但会把你说过的细节记很久",
                bio = "在海边灯塔做记录员，习惯把想说的话写进值班日志里",
                speakingStyle = "句子偏短，语气平稳，偶尔会用海和天气作比喻",
                greeting = "你来了。今天海面很安静，想和我说什么？"
            )
        )

        characterRepository.save(
            CharacterEntity(
                name = "林深",
                relationType = "friend",
                personality = "爽朗、爱开玩笑，但认真的时候很可靠",
                bio = "大学同学，毕业后去了另一座城市，偶尔深夜发来消息",
                speakingStyle = "口语化，喜欢用「哈哈哈」「说真的」开头",
                greeting = "嘿，好久不见！最近怎么样？"
            )
        )

        val memories = listOf(
            Triple("我记得你的生日是 7 月 20 日", "basic", 0.9f),
            Triple("我记得你周末喜欢去爬山，站在山顶吹风会让你放松", "preference", 0.85f),
            Triple("我记得你最近在准备项目，有一点焦虑", "event", 0.75f),
            Triple("我记得你喜欢喝冰美式，不加糖", "preference", 0.6f),
            Triple("我记得你答应过我要早点睡，不要再熬到凌晨", "promise", 0.7f),
            Triple("我记得你不太喜欢人多的场合，更喜欢安静的地方", "preference", 0.6f)
        )
        memories.forEach { (content, category, importance) ->
            runCatching {
                memoryRepository.add(
                    characterId = xiaoyuId,
                    content = content,
                    category = category,
                    importance = importance
                )
            }
        }

        chatRepository.addAssistantMessage(xiaoyuId, "嗨，你来啦～今天过得怎么样呀？")
        chatRepository.addUserMessage(xiaoyuId, "今天有点累，想和你说说话。")
        chatRepository.addAssistantMessage(
            xiaoyuId,
            "辛苦啦。先不用急着说什么，我陪你安静一会儿也可以。今天是被课业压到了，还是发生了别的事？"
        )
        chatRepository.addUserMessage(xiaoyuId, "你还记得我周末喜欢做什么吗？")
        chatRepository.addAssistantMessage(
            xiaoyuId,
            "当然记得，你周末喜欢去爬山。上次你还说，站在山顶吹风的时候，脑子里的杂音会少很多。"
        )
        chatRepository.addUserMessage(xiaoyuId, "项目快到了，我有点焦虑。")
        chatRepository.addAssistantMessage(
            xiaoyuId,
            "焦虑说明你很在意这件事。我们把它拆小一点：今天只做一件最要紧的事，剩下的明天再说，好不好？"
        )
    }
}
