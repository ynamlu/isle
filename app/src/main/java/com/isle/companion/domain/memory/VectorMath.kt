package com.isle.companion.domain.memory

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * 向量工具：余弦相似度与浮点数组的字节序列化（用于存入 Room）。
 */
object VectorMath {

    fun cosine(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    fun l2Normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0f) return vector
        return FloatArray(vector.size) { vector[it] / norm }
    }

    fun toBytes(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun fromBytes(bytes: ByteArray?): FloatArray? {
        if (bytes == null || bytes.isEmpty() || bytes.size % Float.SIZE_BYTES != 0) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.getFloat() }
    }
}
