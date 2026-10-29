package com.isle.companion.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI 兼容的模型列表响应（GET /models）。
 * 仅用于连接探活，不同服务可能只有 id 字段。
 */
@Serializable
data class ModelListResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = "",
    @SerialName("owned_by") val ownedBy: String? = null
)
