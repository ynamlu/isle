package com.isle.companion.domain.memory

import android.content.Context
import com.isle.companion.util.DeviceUtils
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
 *
 * 降级策略（任一命中即退回关键词匹配，不影响其他功能）：
 *  - 模拟器缺少 ONNX Runtime 所需 CPU 指令（native SIGILL 无法 catch）；
 *  - 模型加载或推理失败。
 */
class OnnxEmbeddingProvider(private val context: Context) : EmbeddingProvider {

    private val embedder: TextEmbedder? by lazy {
        if (DeviceUtils.isEmulator) {
            null
        } else {
            runCatching { TextEmbedder(context) }.getOrNull()
        }
    }

    override suspend fun embed(text: String): FloatArray? = withContext(Dispatchers.Default) {
        embedder?.embed(text)
    }
}
