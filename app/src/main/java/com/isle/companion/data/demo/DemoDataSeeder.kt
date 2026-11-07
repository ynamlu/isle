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
 * - 首次启动写入 3 个角色（闺蜜/家人/老师）；
 * - 每个角色 6 条记忆、7 条示例对话；
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

    private val seedPrefs: android.content.SharedPreferences
        get() = context.getSharedPreferences("demo_seed", Context.MODE_PRIVATE)

    suspend fun bootstrap() {
        migrateLegacyDemo()
        seedIfNeeded()
        ensureDemoCharacters()
        ensureDemoContentOnce()
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
        seedPrefs.edit().putBoolean(KEY_DEMO_CONTENT_SEEDED, true).apply()
    }

    private suspend fun seed() {
        val ids = mapOf(
            "小屿" to createXiaoyu(),
            "阿岚" to createAlan(),
            "林深" to createLinshen()
        )
        demoContents.forEach { content ->
            seedContent(ids.getValue(content.name), content)
        }
    }

    private suspend fun seedContent(characterId: Long, content: DemoContent) {
        seedMemories(characterId, content.memories)
        seedMessages(characterId, content.messages)
    }

    private suspend fun seedMemories(
        characterId: Long,
        memories: List<Triple<String, String, Float>>
    ) {
        memories.forEach { (text, category, importance) ->
            runCatching {
                memoryRepository.add(
                    characterId = characterId,
                    content = text,
                    category = category,
                    importance = importance
                )
            }
        }
    }

    private suspend fun seedMessages(characterId: Long, messages: List<Pair<String, String>>) {
        messages.forEach { (role, text) ->
            if (role == "user") {
                chatRepository.addUserMessage(characterId, text)
            } else {
                chatRepository.addAssistantMessage(characterId, text)
            }
        }
    }

    /**
     * 给已有安装补齐新演示内容：
     * - 只处理三个默认角色；
     * - 只在该角色既没有消息也没有记忆时写入，避免覆盖用户数据；
     * - 每个版本只执行一次，之后用户删除内容不会被重新塞回。
     */
    private suspend fun ensureDemoContentOnce() {
        if (seedPrefs.getBoolean(KEY_DEMO_CONTENT_SEEDED, false)) return

        val characters = characterDao.all()
        demoContents.forEach { content ->
            val character = characters.firstOrNull { it.name == content.name } ?: return@forEach
            val hasMessages = chatRepository.recent(character.id, 1).isNotEmpty()
            val hasMemories = memoryDao.allByCharacter(character.id).isNotEmpty()
            if (!hasMemories) seedMemories(character.id, content.memories)
            if (!hasMessages) seedMessages(character.id, content.messages)
        }
        seedPrefs.edit().putBoolean(KEY_DEMO_CONTENT_SEEDED, true).apply()
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

    private data class DemoContent(
        val name: String,
        val memories: List<Triple<String, String, Float>>,
        val messages: List<Pair<String, String>>
    )

    private val demoContents = listOf(
        DemoContent(
            name = "小屿",
            memories = listOf(
                Triple("我记得你的生日是 7 月 20 日", "basic", 0.9f),
                Triple("我记得你周末喜欢去爬山，站在山顶吹风会让你放松", "preference", 0.85f),
                Triple("我记得你最近在准备项目，有一点焦虑", "event", 0.75f),
                Triple("我记得你喜欢喝冰美式，不加糖", "preference", 0.6f),
                Triple("我记得你答应过我要早点睡，不要再熬到凌晨", "promise", 0.7f),
                Triple("我记得你不太喜欢人多的场合，更喜欢安静的地方", "preference", 0.6f)
            ),
            messages = listOf(
                "assistant" to "嗨，你来啦～今天过得怎么样呀？",
                "user" to "今天有点累，想和你说说话。",
                "assistant" to "辛苦啦。先不用急着说什么，我陪你安静一会儿也可以。今天是被课业压到了，还是发生了别的事？",
                "user" to "你还记得我周末喜欢做什么吗？",
                "assistant" to "当然记得，你周末喜欢去爬山。上次你还说，站在山顶吹风的时候，脑子里的杂音会少很多。",
                "user" to "项目快到了，我有点焦虑。",
                "assistant" to "焦虑说明你很在意这件事。我们把它拆小一点：今天只做一件最要紧的事，剩下的明天再说，好不好？"
            )
        ),
        DemoContent(
            name = "阿岚",
            memories = listOf(
                Triple("我记得你回家第一顿饭总想吃番茄鸡蛋面", "preference", 0.85f),
                Triple("我记得你的生日是 7 月 20 日", "basic", 0.9f),
                Triple("我记得你怕让家里失望，但家里更在意你有没有好好吃饭", "relationship", 0.8f),
                Triple("我记得你赶项目时容易忘记喝水", "event", 0.65f),
                Triple("我记得你不太喜欢人多的场合，更喜欢安静地待在家里", "preference", 0.6f),
                Triple("我记得你答应过周末给家里打个电话", "promise", 0.7f)
            ),
            messages = listOf(
                "assistant" to "回来啦。今天累不累？先坐一会儿，慢慢说。",
                "user" to "有点累，项目压得我喘不过气。",
                "assistant" to "先不急着把所有事都解决。饭吃了没有？没吃的话，先给自己弄点热的。",
                "user" to "你还记得我回家最想吃什么吗？",
                "assistant" to "记得，番茄鸡蛋面。你每次回家第一顿都惦记这个。",
                "user" to "最近有点想家。",
                "assistant" to "想家就回来住两天，不用等把事情都做完。家里不催你，先把自己照顾好。"
            )
        ),
        DemoContent(
            name = "林深",
            memories = listOf(
                Triple("我记得你在做心屿项目，最近在准备比赛答辩", "event", 0.85f),
                Triple("我记得你容易把任务想成一大块，拆小之后会好很多", "preference", 0.75f),
                Triple("我记得你上次说，最担心答辩时讲不清设计取舍", "event", 0.8f),
                Triple("我记得你习惯先做最难的，容易卡住", "preference", 0.65f),
                Triple("我记得你答应过这周先跑通一个最小演示", "promise", 0.7f),
                Triple("我记得你不太喜欢当众发言，但准备充分时会稳很多", "preference", 0.6f)
            ),
            messages = listOf(
                "assistant" to "我在。最近是学习上的事，还是心里有些乱？都可以说。",
                "user" to "项目快截止了，我怕做不完。",
                "assistant" to "先不急着解决全部。把今天要做的写成一件最小的事，只做这一件。",
                "user" to "你还记得我上次说卡在哪吗？",
                "assistant" to "记得，你说最担心答辩时讲不清设计取舍。我们先练这一部分，不背整篇稿。",
                "user" to "我怕讲不好。",
                "assistant" to "讲不好通常不是能力问题，是顺序没排好。先讲问题、再做演示、最后讲取舍，三分钟就够。"
            )
        )
    )

    companion object {
        private const val KEY_DEMO_CONTENT_SEEDED = "content_v233"
    }
}
