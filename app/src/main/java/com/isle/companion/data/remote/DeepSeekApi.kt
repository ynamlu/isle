package com.isle.companion.data.remote

import com.isle.companion.data.remote.dto.ChatCompletionRequest
import com.isle.companion.data.remote.dto.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * DeepSeek OpenAI 兼容接口（非流式）。
 * 使用 @Url 以支持运行时可配置的 Base URL。
 */
interface DeepSeekApi {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
