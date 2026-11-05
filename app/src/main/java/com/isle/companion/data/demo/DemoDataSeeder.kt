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
 * - 首次启动写入 3 个角色（闺蜜/家人/老师）、6 条记忆和一段可触发记忆召回的对话；
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
        migrateLegacyDemo()
        seedIfNeeded()
        ensureDemoCharacters()
    }

    /**
     * 兼容 v1 的演示数据：
     * - 旧「阿岸（恋人）」→「阿岚（家人）」；
     * - 旧「林深（朋友）」→「林深（老师）」。
     * 只匹配旧演示角色的固定名称、关系和简介，避免误改用户自建角色。
     */
    private suspend fun migrateLegacyDemo() {
        characterDao.all().forEach { character ->
            when {
                character.name == "阿岸" &&
                    character.relationType == "love" &&
                    character.bio.contains("海边灯塔") -> {
                    characterRepository.save(
                        character.copy(
                            name = "阿岚",
                            relationType = "family",
                            personality = "稳重、耐心，不会急着说教，习惯先把事情听完",
                            bio = "像家里那个总在厨房留一盏灯的人，记得你爱吃什么，也记得你怕什么",
                            speakingStyle = "语气温和，偶尔会问「吃饭了吗」，但不会用关心压你",
                            greeting = "回来啦。今天累不累？先坐一会儿，慢慢说。"
                        )
                    )
                }

                character.name == "林深" &&
                    character.relationType == "friend" &&
                    character.bio.contains("大学同学") -> {
                    characterRepository.save(
                        character.copy(
                            relationType = "teacher",
                            personality = "温和、有条理，擅长把大问题拆成可以开始的小步骤",
                            bio = "大学里的学业导师，见过很多学生在期末和毕业前焦虑，不会用成绩定义一个人",
                            speakingStyle = "先共情，再提问；喜欢说「我们先不急着解决全部」",
                            greeting = "我在。最近是学习上的事，还是心里有些乱？都可以说。"
                        )
                    )
                }
            }
        }
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
        val xiaoyuId = createXiaoyu()
        createAlan()
        createLinshen()

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

    /**
     * 如果用户删除了某个演示角色，下次启动时补齐；
     * 只有当前角色全部是演示角色时才补，避免影响用户自建角色。
     */
    private suspend fun ensureDemoCharacters() {
        val characters = characterDao.all()
        if (characters.isEmpty()) return
        val demoNames = setOf("小屿", "林深", "阿岚", "阿岸")
        if (characters.any { it.name !in demoNames }) return

        if (characters.none { it.name == "小屿" }) createXiaoyu()
        if (characters.none { it.name == "阿岚" || it.name == "阿岸" }) createAlan()
        if (characters.none { it.name == "林深" }) createLinshen()
    }

    private suspend fun createXiaoyu(): Long = characterRepository.save(
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

    private suspend fun createAlan(): Long = characterRepository.save(
        CharacterEntity(
            name = "阿岚",
            relationType = "family",
            personality = "稳重、耐心，不会急着说教，习惯先把事情听完",
            bio = "像家里那个总在厨房留一盏灯的人，记得你爱吃什么，也记得你怕什么",
            speakingStyle = "语气温和，偶尔会问「吃饭了吗」，但不会用关心压你",
            greeting = "回来啦。今天累不累？先坐一会儿，慢慢说。"
        )
    )

    private suspend fun createLinshen(): Long = characterRepository.save(
        CharacterEntity(
            name = "林深",
            relationType = "teacher",
            personality = "温和、有条理，擅长把大问题拆成可以开始的小步骤",
            bio = "大学里的学业导师，见过很多学生在期末和毕业前焦虑，不会用成绩定义一个人",
            speakingStyle = "先共情，再提问；喜欢说「我们先不急着解决全部」",
            greeting = "我在。最近是学习上的事，还是心里有些乱？都可以说。"
        )
    )
}
