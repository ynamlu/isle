package com.isle.companion.data.remote

import com.isle.companion.data.prefs.AiSettings
import com.isle.companion.data.remote.dto.ApiMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 真实模型服务的端到端测试（默认跳过）。
 *
 * 读取 `local.properties` 注入到 BuildConfig 的 AI_BASE_URL / AI_MODEL / AI_API_KEY，
 * 走完整客户端链路验证「测试连接 / 非流式补全 / 流式补全」。
 *
 * 运行方式：
 *   ./gradlew :app:testDebugUnitTest --tests '*OpenAiClientE2eTest*' -Disle.e2e=1
 */
class OpenAiClientE2eTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun settings(): AiSettings = AiSettings()

    private fun client(): OpenAiClient {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return OpenAiClient(json, okHttp, retrofit.create(OpenAiApi::class.java))
    }

    @Before
    fun requireExplicitE2eRun() {
        val enabled = System.getProperty("isle.e2e") == "1" || System.getenv("ISLE_E2E") == "1"
        assumeTrue("默认跳过真实网络测试；用 -Disle.e2e=1 显式开启", enabled)
        assumeTrue(
            "local.properties 未配置 AI_API_KEY，无法做真实接口测试",
            settings().effectiveApiKey.isNotBlank()
        )
    }

    @Test
    fun testConnectionSucceeds() = runBlocking {
        val settings = settings()
        val result = client().testConnection(settings)
        assertTrue(
            "连接失败：${result.exceptionOrNull()?.message}",
            result.isSuccess
        )
        println("[E2E] ${result.getOrThrow()} · ${settings.effectiveBaseUrl} · ${settings.effectiveModel}")
    }

    @Test
    fun nonStreamingCompletionReturnsContent() = runBlocking {
        val result = client().complete(
            settings = settings(),
            messages = listOf(ApiMessage(role = "user", content = JsonPrimitive("只回复两个字：你好"))),
            maxTokensOverride = 32
        )
        val content = result.getOrThrow()
        assertTrue("模型返回内容为空", content.isNotBlank())
        println("[E2E] 非流式回复：$content")
    }

    @Test
    fun streamingCompletionReceivesDeltas() {
        val finished = CountDownLatch(1)
        val received = StringBuilder()
        val failure = AtomicReference<Throwable?>(null)

        val source = client().stream(
            settings = settings(),
            messages = listOf(
                ApiMessage(role = "user", content = JsonPrimitive("用一句话夸夸我"))
            ),
            onDelta = { received.append(it) },
            onComplete = { finished.countDown() },
            onError = {
                failure.set(it)
                finished.countDown()
            }
        )

        assertNotNull("EventSource 创建失败（地址非法？）", source)
        assertTrue("流式请求超时（60s）", finished.await(60, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("流式请求失败：${it.message}", it) }
        assertTrue("未收到流式内容", received.isNotEmpty())
        println("[E2E] 流式回复：$received")
    }
}
