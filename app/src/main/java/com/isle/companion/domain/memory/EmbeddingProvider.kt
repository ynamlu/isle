package com.isle.companion.domain.memory

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 文本向量化能力抽象，便于替换实现或降级。
 */
interface EmbeddingProvider {
    suspend fun embed(text: String): FloatArray?
}

/**
 * 基于端侧 ONNX 模型的向量化实现。
 * 模型加载失败时自动降级为空实现，不影响应用其他功能。
 */
class OnnxEmbeddingProvider(private val context: Context) : EmbeddingProvider {

    private val embedder: TextEmbedder? by lazy {
        runCatching { TextEmbedder(context) }.getOrNull()
    }

    override suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        embedder?.embed(text)
    }
}
