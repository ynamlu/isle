package com.isle.companion.data.remote

import com.isle.companion.data.prefs.AiSettings
import com.isle.companion.data.remote.dto.ApiMessage
import com.isle.companion.data.remote.dto.ChatCompletionRequest
import kotlinx.coroutines.CancellationException
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
import retrofit2.HttpException

/**
 * OpenAI 兼容协议客户端：统一处理鉴权、请求构建、探活与流式解析。
 *
 * 不绑定任何厂商——地址、模型、Key 全部来自 [AiSettings]，
 * 可在 local.properties 中预置，也可在应用设置页随时修改。
 */
class OpenAiClient(
    private val json: Json,
    private val okHttpClient: OkHttpClient,
    private val api: OpenAiApi
) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    /**
     * 非流式补全（记忆抽取等内部任务使用）。
     */
    suspend fun complete(
        settings: AiSettings,
        messages: List<ApiMessage>,
        maxTokensOverride: Int? = null
    ): Result<String> = try {
        val response = api.chatCompletion(
            url = OpenAiUrls.chatCompletionsUrl(settings.effectiveBaseUrl),
            authorization = settings.authorizationHeader(),
            request = ChatCompletionRequest(
                model = settings.effectiveModel,
                messages = messages,
                stream = false,
                temperature = settings.temperature,
                maxTokens = maxTokensOverride ?: settings.maxTokens
            )
        )
        Result.success(response.choices.firstOrNull()?.message?.content.orEmpty())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toUserFacing())
    }

    /**
     * 获取模型列表，供设置页快速切换模型。
     */
    suspend fun listModels(settings: AiSettings): Result<List<String>> = try {
        val response = api.models(
            url = OpenAiUrls.modelsUrl(settings.effectiveBaseUrl),
            authorization = settings.authorizationHeader()
        )
        Result.success(response.data.map { it.id }.filter { it.isNotBlank() }.distinct())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toUserFacing())
    }

    /**
     * 测试连接：优先请求 /models（不消耗 token）；服务未实现该接口时，
     * 用 1 token 的对话请求兜底。
     */
    suspend fun testConnection(settings: AiSettings): Result<String> = try {
        if (settings.effectiveBaseUrl.isBlank()) {
            throw OpenAiException("请先填写 API 地址")
        }
        if (settings.requiresApiKey && settings.effectiveApiKey.isBlank()) {
            throw OpenAiException("请先填写 API Key")
        }

        val modelsResult = runCatching {
            api.models(
                url = OpenAiUrls.modelsUrl(settings.effectiveBaseUrl),
                authorization = settings.authorizationHeader()
            )
        }
        val models = modelsResult.getOrNull()
        if (models != null) {
            val names = models.data.map { it.id }.filter { it.isNotBlank() }
            when {
                names.isEmpty() -> Result.success("连接正常 · ${settings.effectiveModel}")
                settings.effectiveModel in names ->
                    Result.success("连接正常 · ${settings.effectiveModel} 可用（共 ${names.size} 个模型）")
                else ->
                    Result.success(
                        "连接正常 · 服务共 ${names.size} 个模型，未在列表中看到 ${settings.effectiveModel}"
                    )
            }
        } else {
            val error = modelsResult.exceptionOrNull()
            // 鉴权失败不兜底，直接给出明确提示
            if (error is HttpException && error.code() in AUTH_ERROR_CODES) {
                Result.failure(error.toUserFacing())
            } else {
                complete(
                    settings = settings,
                    messages = listOf(ApiMessage(role = "user", content = JsonPrimitive("ping"))),
                    maxTokensOverride = 1
                ).fold(
                    onSuccess = { Result.success("连接正常 · ${settings.effectiveModel}") },
                    onFailure = { Result.failure(it) }
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e.toUserFacing())
    }

    /**
     * 流式补全。返回 EventSource 可用于取消；地址非法时同步回调 onError 并返回 null。
     */
    fun stream(
        settings: AiSettings,
        messages: List<ApiMessage>,
        onDelta: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): EventSource? {
        val url = runCatching { OpenAiUrls.chatCompletionsUrl(settings.effectiveBaseUrl) }
            .getOrElse {
                onError(it.toUserFacing())
                return null
            }

        val payload = ChatCompletionRequest(
            model = settings.effectiveModel,
            messages = messages,
            stream = true,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens
        )
        val request = try {
            Request.Builder()
                .url(url)
                .apply {
                    settings.authorizationHeader()?.let { addHeader("Authorization", it) }
                }
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(
                    json.encodeToString(ChatCompletionRequest.serializer(), payload)
                        .toRequestBody(jsonMedia)
                )
                .build()
        } catch (e: Exception) {
            onError(e.toUserFacing())
            return null
        }

        return try {
            EventSources.createFactory(okHttpClient).newEventSource(
                request,
                object : EventSourceListener() {
                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        when (val event = OpenAiStreamParser.parse(json, data)) {
                            is OpenAiStreamParser.Event.Done -> onComplete()
                            is OpenAiStreamParser.Event.Delta -> onDelta(event.text)
                            is OpenAiStreamParser.Event.Failure ->
                                onError(OpenAiException(event.message))
                            OpenAiStreamParser.Event.Ignored -> Unit
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        onComplete()
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: Response?
                    ) {
                        val error = if (response != null) {
                            val body = runCatching { response.body.string() }.getOrNull()
                            OpenAiException(
                                OpenAiErrorParser.describe(
                                    code = response.code,
                                    responseBody = body,
                                    fallback = "流式请求失败"
                                )
                            )
                        } else {
                            t?.toUserFacing() ?: OpenAiException("流式请求失败")
                        }
                        onError(error)
                    }
                }
            )
        } catch (e: Exception) {
            onError(e.toUserFacing())
            null
        }
    }

    private fun AiSettings.authorizationHeader(): String? =
        effectiveApiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    private fun Throwable.toUserFacing(): Throwable = when (this) {
        is OpenAiException -> this
        is HttpException -> OpenAiException(
            OpenAiErrorParser.describe(
                code = code(),
                responseBody = runCatching { response()?.errorBody()?.string() }.getOrNull(),
                fallback = "请求失败"
            ),
            this
        )
        is IllegalArgumentException -> OpenAiException(message ?: "API 地址格式不正确", this)
        else -> this
    }

    private companion object {
        val AUTH_ERROR_CODES = setOf(401, 403)
    }
}
