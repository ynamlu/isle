package com.isle.companion.domain.memory

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.Closeable
import java.nio.LongBuffer

/**
 * 端侧中文文本向量化（ONNX Runtime + bge-small-zh-v1.5）。
 * 完全离线运行，用于长期记忆的语义检索。
 */
class TextEmbedder(context: Context) : Closeable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val tokenizer: BertTokenizer = BertTokenizer.fromAssets(context)
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        val options = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
        }
        session = env.createSession(modelBytes, options)
    }

    /**
     * 生成句向量（CLS 池化 + L2 归一化）。
     */
    fun embed(text: String): FloatArray? {
        val ids = tokenizer.encode(text, MAX_TOKENS)
        if (ids.isEmpty()) return null

        val seqLen = ids.size
        val shape = longArrayOf(1, seqLen.toLong())
        val mask = LongArray(seqLen) { 1L }
        val types = LongArray(seqLen) { 0L }

        return runCatching {
            val inputs = mapOf(
                "input_ids" to OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape),
                "attention_mask" to OnnxTensor.createTensor(env, LongBuffer.wrap(mask), shape),
                "token_type_ids" to OnnxTensor.createTensor(env, LongBuffer.wrap(types), shape)
            )
            inputs.values.forEach { /* 保持引用，供 run 使用 */ }

            session.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val output = result[0].value as Array<Array<FloatArray>>
                val cls = output[0][0]
                VectorMath.l2Normalize(cls)
            }
        }.getOrNull()
    }

    override fun close() {
        runCatching { session.close() }
    }

    companion object {
        private const val MODEL_ASSET = "embedding/model_int8.onnx"
        private const val MAX_TOKENS = 256
    }
}
