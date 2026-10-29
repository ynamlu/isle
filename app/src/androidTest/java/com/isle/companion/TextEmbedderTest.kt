package com.isle.companion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.isle.companion.domain.memory.TextEmbedder
import com.isle.companion.domain.memory.VectorMath
import com.isle.companion.util.DeviceUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证端侧 embedding 模型可在真机加载并推理。
 * 运行方式：./gradlew connectedDebugAndroidTest
 *
 * 注意：Android 模拟器（QEMU）缺少 ONNX Runtime 所需的部分 ARM64 指令，
 * 推理时会触发 SIGILL 原生崩溃且无法被 catch；因此在模拟器上跳过本用例，
 * 需在真机运行（模拟器上仍会执行记忆隔离等其他仪器化测试）。
 */
@RunWith(AndroidJUnit4::class)
class TextEmbedderTest {

    @Test
    fun embeddingProducesSemanticVectors() {
        if (DeviceUtils.isEmulator) {
            println("[EmbedderTest] 模拟器缺少 ONNX Runtime 所需 CPU 指令，跳过；请连接真机运行")
            return
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val embedder = TextEmbedder(context)

        try {
            val hiking = embedder.embed("我喜欢在周末去爬山")
            val mountain = embedder.embed("周末我常常去登山锻炼")
            val stock = embedder.embed("今天股票市场的波动非常大")

            assertNotNull("向量生成失败", hiking)
            assertEquals("向量维度应为 512", 512, hiking!!.size)

            val similar = VectorMath.cosine(hiking, mountain!!)
            val unrelated = VectorMath.cosine(hiking, stock!!)

            println("[EmbedderTest] sim(爬山, 登山) = $similar")
            println("[EmbedderTest] sim(爬山, 股票) = $unrelated")

            assertTrue("语义相近的句子相似度应显著更高", similar > unrelated + 0.05f)
        } finally {
            embedder.close()
        }
    }
}
