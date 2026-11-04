package com.hamzabilici.yazki.core.turkish

/**
 * Türkçe klavye kahkahalarını tanır: "sjsjsj", "ksksks", "kdkdkd", "hahaha", "ahahah",
 * "asdfgh" gibi klavye ezmeleri ve gülen emojiler.
 *
 * Kurallar katlanmış (fold) ve küçük harfli kelimeler üzerinde çalışır.
 */
object Laughter {

    /** s, j, k, d, h harflerinin ikili tekrarları: sj, ks, kd, jd ... */
    private val PAIR_MASH = Regex("""^a?(?:sj|js|ks|sk|kd|dk|jd|dj|sd|ds|kj|jk|sh|hs)+[sjkdh]?$""")

    /** ha / he / hi tekrarları: haha, ahaha, hehe, hihi. */
    private val HA_HA = Regex("""^a?(?:ha|ah|he|eh|hi|ih)+h?a?$""")

    private val HOME_ROW = setOf('a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l')

    private val LAUGHING_EMOJI = listOf("😂", "🤣", "😹", "😆")

    /** Kahkaha kalıplarında geçebilecek bütün harfler. */
    private const val LAUGH_CHARS = "asdfghjkleiı"

    fun isLaughterWord(foldedWord: String): Boolean {
        val w = foldedWord
        if (w.length < 4) return false
        // Hızlı ret: gerçek kelimelerin neredeyse hepsinde r, n, m, t, y... gibi harfler var;
        // regex'leri yalnızca adaylarda çalıştır.
        if (!w.all { it in LAUGH_CHARS }) return false
        if (PAIR_MASH.matches(w)) return true
        if (HA_HA.matches(w) && w.count { it == 'h' } >= 2) return true
        return isKeyboardMash(w)
    }

    /**
     * "asdfgh", "sdfghj" gibi orta sıra ezmeleri. "salak", "dalga" gibi gerçek kelimeleri
     * elemek için 'a' oranı düşük tutulur.
     */
    private fun isKeyboardMash(w: String): Boolean {
        if (w.length < 5) return false
        if (!w.all { it in HOME_ROW }) return false
        val consonants = w.filter { it != 'a' }.toSet()
        val aCount = w.count { it == 'a' }
        return consonants.size >= 3 && aCount * 4 <= w.length
    }

    fun countEmoji(text: String): Int {
        // Gülen emojilerin hepsi vekil çift; hiç yoksa taramaya gerek yok.
        if (text.none { it.isHighSurrogate() }) return 0
        return LAUGHING_EMOJI.sumOf { countOccurrences(text, it) }
    }

    /** Bir mesajdaki kahkaha sayısı: kahkaha kelimeleri + gülen emojiler. */
    fun count(text: String): Int =
        TurkishText.foldedWords(text).count(::isLaughterWord) + countEmoji(text)

    /** Önceden bölünmüş mesaj için. */
    fun count(text: TokenizedText): Int =
        text.squeezed.count(::isLaughterWord) + countEmoji(text.raw)
}

internal fun countOccurrences(text: String, needle: String): Int {
    if (needle.isEmpty()) return 0
    var count = 0
    var from = 0
    while (true) {
        val idx = text.indexOf(needle, from)
        if (idx < 0) return count
        count++
        from = idx + needle.length
    }
}
