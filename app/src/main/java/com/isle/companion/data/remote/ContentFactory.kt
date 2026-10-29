package com.isle.companion.data.remote

import android.util.Base64
import com.isle.companion.data.remote.dto.ApiMessage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.File

/**
 * 构造 OpenAI 兼容协议的消息内容，支持纯文本与「文本 + 图片」多模态。
 */
object ContentFactory {

    fun textMessage(role: String, text: String): ApiMessage =
        ApiMessage(role = role, content = JsonPrimitive(text))

    /**
     * 多模态用户消息：图片 + 文字。
     * 图片以 data URI（base64）形式内联，需确认所选模型支持图片输入。
     */
    fun imageMessage(role: String, text: String, imageFile: File): ApiMessage {
        val base64 = Base64.encodeToString(imageFile.readBytes(), Base64.NO_WRAP)
        val mime = if (imageFile.extension.lowercase() == "png") "image/png" else "image/jpeg"
        val parts: JsonArray = buildJsonArray {
            add(buildJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive(text))
            })
            add(buildJsonObject {
                put("type", JsonPrimitive("image_url"))
                put("image_url", buildJsonObject {
                    put("url", JsonPrimitive("data:$mime;base64,$base64"))
                })
            })
        }
        return ApiMessage(role = role, content = parts)
    }

    fun assistantMessage(text: String): ApiMessage =
        ApiMessage(role = "assistant", content = JsonPrimitive(text))

    fun systemMessage(text: String): ApiMessage =
        ApiMessage(role = "system", content = JsonPrimitive(text))
}
