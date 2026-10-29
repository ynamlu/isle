package com.isle.companion.data.remote

import com.isle.companion.data.prefs.DeepSeekSettings
import com.isle.companion.data.remote.dto.ApiMessage
import com.isle.companion.data.remote.dto.BalanceResponse
import com.isle.companion.data.remote.dto.ChatCompletionChunk
import com.isle.companion.data.remote.dto.ChatCompletionRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * DeepSeek 客户端：统一处理鉴权、请求构建与流式解析。
 */
class DeepSeekClient(
    private val json: Json,
    private val okHttpClient: OkHttpClient,
    private val api: DeepSeekApi
) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /**
     * 非流式补全。
     */
    suspend fun complete(
        settings: DeepSeekSettings,
        messages: List<ApiMessage>,
        maxTokensOverride: Int? = null
    ): Result<String> = runCatching {
        val response = api.chatCompletion(
            url = endpoint(settings.baseUrl, "chat/completions"),
            authorization = "Bearer ${settings.effectiveApiKey}",
            request = ChatCompletionRequest(
                model = settings.model,
                messages = messages,
                stream = false,
                temperature = settings.temperature,
                maxTokens = maxTokensOverride ?: settings.maxTokens
            )
        )
        response.choices.firstOrNull()?.message?.content.orEmpty()
    }

    /** 查询余额；自定义代理若不支持该接口会失败，调用方可回退到 ping。 */
    suspend fun balance(settings: DeepSeekSettings): Result<BalanceResponse> = runCatching {
        api.balance(
            url = balanceUrl(settings.baseUrl),
            authorization = "Bearer ${settings.effectiveApiKey}"
        )
    }

    /**
     * 测试连接：优先查余额（不发对话、不产生 token），失败时用 1 token 的 ping 兜底。
     */
    suspend fun testConnection(settings: DeepSeekSettings): Result<String> {
        val balance = balance(settings).getOrNull()
        if (balance != null) {
            val info = balance.balanceInfos.firstOrNull()
            return Result.success(
                if (info == null) {
                    "连接正常"
                } else {
                    "连接正常 · 余额 ${info.currency} ${info.totalBalance}"
                }
            )
        }
        return complete(
            settings = settings,
            messages = listOf(ApiMessage(role = "user", content = JsonPrimitive("ping"))),
            maxTokensOverride = 1
        ).map { "连接正常" }
    }

    private fun balanceUrl(baseUrl: String): String {
        val root = baseUrl.trimEnd('/').removeSuffix("/v1")
        return "$root/user/balance"
    }

    /**
     * 流式补全。返回 EventSource，可用于取消。
     */
    fun stream(
        settings: DeepSeekSettings,
        messages: List<ApiMessage>,
        onDelta: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): EventSource {
        val payload = ChatCompletionRequest(
            model = settings.model,
            messages = messages,
            stream = true,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens
        )
        val request = Request.Builder()
            .url(endpoint(settings.baseUrl, "chat/completions"))
            .addHeader("Authorization", "Bearer ${settings.effectiveApiKey}")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(ChatCompletionRequest.serializer(), payload).toRequestBody(jsonMedia))
            .build()

        return EventSources.createFactory(okHttpClient).newEventSource(
            request,
            object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]") {
                        onComplete()
                        return
                    }
                    runCatching {
                        json.decodeFromString(ChatCompletionChunk.serializer(), data)
                    }.onSuccess { chunk ->
                        val delta = chunk.choices.firstOrNull()?.delta
                        delta?.content?.takeIf { it.isNotEmpty() }?.let(onDelta)
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    onComplete()
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    onError(t ?: IllegalStateException("流式请求失败（HTTP ${response?.code}）"))
                }
            }
        )
    }

    private fun endpoint(baseUrl: String, path: String): String =
        baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}
