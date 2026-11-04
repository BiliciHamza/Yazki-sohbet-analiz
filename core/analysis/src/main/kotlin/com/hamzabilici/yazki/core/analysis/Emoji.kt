package com.hamzabilici.yazki.core.analysis

import java.text.BreakIterator

/** Metindeki emojileri grafem kümesi bazında çıkarır; ten rengi varyantlarını birleştirir. */
internal object Emoji {

    private const val VARIATION_SELECTOR = 0xFE0F
    private val SKIN_TONES = 0x1F3FB..0x1F3FF

    private fun isEmojiCodePoint(cp: Int): Boolean =
        cp in 0x1F000..0x1FAFF ||
            cp in 0x2600..0x27BF ||
            cp in 0x2300..0x23FF ||
            cp in 0x2B00..0x2BFF

    fun extract(text: String): List<String> {
        if (text.none { it.isSurrogate() || it.code in 0x2300..0x2BFF }) return emptyList()
        val result = ArrayList<String>()
        val it = BreakIterator.getCharacterInstance()
        it.setText(text)
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val cluster = text.substring(start, end)
            if (cluster.codePoints().anyMatch(::isEmojiCodePoint)) {
                val normalized = normalize(cluster)
                if (normalized.isNotEmpty()) result += normalized
            }
            start = end
            end = it.next()
        }
        return result
    }

    private fun normalize(cluster: String): String {
        val sb = StringBuilder()
        cluster.codePoints().forEach { cp ->
            if (cp != VARIATION_SELECTOR && cp !in SKIN_TONES) sb.appendCodePoint(cp)
        }
        return sb.toString()
    }
}
