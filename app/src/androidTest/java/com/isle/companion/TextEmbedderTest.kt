package com.isle.companion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.isle.companion.domain.memory.TextEmbedder
import com.isle.companion.domain.memory.VectorMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 验证端侧 embedding 模型可在真机加载并推理。
 * 运行方式：./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class TextEmbedderTest {

    @Test
    fun embeddingProducesSemanticVectors() {
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
