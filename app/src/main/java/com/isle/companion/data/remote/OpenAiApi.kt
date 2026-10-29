package com.isle.companion.data.remote

import com.isle.companion.data.remote.dto.ChatCompletionRequest
import com.isle.companion.data.remote.dto.ChatCompletionResponse
import com.isle.companion.data.remote.dto.ModelListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * OpenAI 兼容协议接口（Chat Completions）。
 *
 * 使用 Retrofit 的 @Url 支持运行时任意服务地址，因此不绑定任何具体厂商。
 */
interface OpenAiApi {

    /** 非流式对话补全 */
    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    /** 模型列表：用于低成本探活，部分服务可能未实现 */
    @GET
    suspend fun models(
        @Url url: String,
        @Header("Authorization") authorization: String?
    ): ModelListResponse
}
