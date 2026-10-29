package com.isle.companion.di

import android.content.Context
import com.isle.companion.data.demo.DemoDataSeeder
import com.isle.companion.data.local.IsleDatabase
import com.isle.companion.data.prefs.SettingsRepository
import com.isle.companion.data.prefs.UserProfileRepository
import com.isle.companion.data.remote.DeepSeekApi
import com.isle.companion.data.remote.DeepSeekClient
import com.isle.companion.data.repository.CharacterRepository
import com.isle.companion.data.repository.ChatRepository
import com.isle.companion.data.repository.MemoryRepository
import com.isle.companion.domain.memory.EmbeddingProvider
import com.isle.companion.domain.memory.MemoryExtractor
import com.isle.companion.domain.memory.OnnxEmbeddingProvider
import com.isle.companion.util.AvatarStorage
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 轻量依赖容器：集中构建数据库、网络与仓储实例。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** 供需要 Context 的能力（图片存储等）使用 */
    val context: Context get() = appContext

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val deepSeekApi: DeepSeekApi = retrofit.create(DeepSeekApi::class.java)

    private val database: IsleDatabase = IsleDatabase.get(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val userProfileRepository = UserProfileRepository(appContext)
    val avatarStorage = AvatarStorage(appContext)
    val deepSeekClient = DeepSeekClient(json, okHttpClient, deepSeekApi)
    val characterRepository = CharacterRepository(database.characterDao(), avatarStorage, database.messageDao())
    val chatRepository = ChatRepository(database.messageDao())
    val embeddingProvider: EmbeddingProvider = OnnxEmbeddingProvider(appContext)
    val memoryRepository = MemoryRepository(database.memoryDao(), embeddingProvider)
    val memoryExtractor = MemoryExtractor(json, deepSeekClient)

    val demoDataSeeder = DemoDataSeeder(
        context = appContext,
        characterDao = database.characterDao(),
        messageDao = database.messageDao(),
        memoryDao = database.memoryDao(),
        characterRepository = characterRepository,
        chatRepository = chatRepository,
        memoryRepository = memoryRepository
    )
}
