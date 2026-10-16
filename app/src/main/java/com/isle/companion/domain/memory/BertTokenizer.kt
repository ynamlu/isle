package com.isle.companion.domain.memory

import android.content.Context

/**
 * BERT WordPiece 分词器（面向中文场景的精简实现）。
 * 词表来自端侧 embedding 模型自带的 vocab.txt。
 */
class BertTokenizer(private val vocab: Map<String, Int>) {

    fun encode(text: String, maxLength: Int = 256): LongArray {
        val basicTokens = basicTokenize(text)
        val ids = ArrayList<Long>(maxLength.coerceAtMost(512))

        for (token in basicTokens) {
            for (piece in wordPiece(token)) {
                if (ids.size >= maxLength - 2) break
                ids.add(vocab[piece]?.toLong() ?: unkId)
            }
            if (ids.size >= maxLength - 2) break
        }

        val result = ArrayList<Long>(ids.size + 2)
        result.add(clsId)
        result.addAll(ids)
        result.add(sepId)
        return result.toLongArray()
    }

    /**
     * 基础切分：转小写、CJK 字符两侧补空格、按空格与标点切分。
     */
    private fun basicTokenize(text: String): List<String> {
        val cleaned = buildString {
            for (ch in text.lowercase()) {
                if (ch.code == 0 || ch.code == 0xFFFD || ch.isWhitespace()) {
                    append(' ')
                } else if (isCjk(ch)) {
                    append(' ').append(ch).append(' ')
                } else if (isPunctuation(ch)) {
                    append(' ').append(ch).append(' ')
                } else {
                    append(ch)
                }
            }
        }
        return cleaned.split(' ').filter { it.isNotBlank() }
    }

    /**
     * WordPiece：贪心最长匹配。
     */
    private fun wordPiece(token: String): List<String> {
        if (token.length > MAX_WORD_LENGTH) return listOf(UNK)

        val pieces = ArrayList<String>()
        var start = 0
        while (start < token.length) {
            var end = token.length
            var matched: String? = null
            while (start < end) {
                val sub = token.substring(start, end)
                val candidate = if (start > 0) "##$sub" else sub
                if (vocab.containsKey(candidate)) {
                    matched = candidate
                    break
                }
                end--
            }
            if (matched == null) return listOf(UNK)
            pieces.add(matched)
            start = end
        }
        return pieces
    }

    private fun isCjk(ch: Char): Boolean =
        (ch.code in 0x4E00..0x9FFF) ||
            (ch.code in 0x3400..0x4DBF) ||
            (ch.code in 0xF900..0xFAFF) ||
            (ch.code in 0x3040..0x30FF) ||
            (ch.code in 0xAC00..0xD7AF)

    private fun isPunctuation(ch: Char): Boolean =
        ch.isLetterOrDigit().not() && !ch.isWhitespace() && ch.code < 0x2E80

    private val clsId: Long get() = vocab[CLS]?.toLong() ?: 101L
    private val sepId: Long get() = vocab[SEP]?.toLong() ?: 102L
    private val unkId: Long get() = vocab[UNK]?.toLong() ?: 100L

    companion object {
        const val CLS = "[CLS]"
        const val SEP = "[SEP]"
        const val UNK = "[UNK]"
        private const val MAX_WORD_LENGTH = 100

        fun fromAssets(context: Context, assetPath: String = "embedding/vocab.txt"): BertTokenizer {
            val vocab = HashMap<String, Int>(32000)
            context.assets.open(assetPath).bufferedReader().useLines { lines ->
                var index = 0
                lines.forEach { line ->
                    vocab[line.trimEnd('\r')] = index
                    index++
                }
            }
            return BertTokenizer(vocab)
        }
    }
}
