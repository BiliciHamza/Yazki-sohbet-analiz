package com.hamzabilici.yazki.core.turkish

/** Türkçe sohbete özel sayaç kategorileri. */
enum class PhraseCategory {
    /** tamam, tmm, ok, olur, peki */
    OKAY,

    /** seni seviyorum, canım, aşkım, özledim, kalp emojileri */
    LOVE,

    /** özür dilerim, pardon, kusura bakma */
    APOLOGY,

    /** haklısın, doğru diyorsun */
    YOU_ARE_RIGHT,

    /** geliyorum, yoldayım, 5 dk */
    ON_MY_WAY,

    /** günaydın, gnydn, hayırlı sabahlar */
    GOOD_MORNING,

    /** iyi geceler, iyi uykular, tatlı rüyalar */
    GOOD_NIGHT,

    /** hayırlı cumalar, cumanız mübarek olsun */
    FRIDAY_GREETING,

    /** teşekkürler, tşk, sağ ol, eyvallah */
    THANKS,

    /** naber, nasılsın, napıyorsun: hal hatır sorma */
    HOW_ARE_YOU,
}

/**
 * Mesajlardaki Türkçe kalıpları sayar.
 *
 * Kalıplar katlanmış biçimde yazılır (ç→c, ı→i, ş→s ...) ve hem kalıp hem mesaj
 * [TurkishText.collapseRepeats] ile tekrarsız hale getirilir; böylece "Günaydınnn",
 * "gunaydin" ve "GÜNAYDIN" aynı sayılır.
 *
 * Çok kelimeli kalıplar önce denenir ve eşleşen kelimeler tüketilir;
 * "özür dilerim" hem "özür dilerim" hem "özür" olarak iki kez sayılmaz.
 */
object PhraseCounter {

    private val PHRASES: Map<PhraseCategory, List<String>> = mapOf(
        PhraseCategory.OKAY to listOf(
            "tamam", "tmm", "tm", "tamamdir", "ok", "okey", "okay", "olur", "peki",
        ),
        PhraseCategory.LOVE to listOf(
            "seni seviyorum", "seni cok seviyorum", "seviyorum", "canim", "askim", "ozledim",
            "seni ozledim", "sevgilim", "bitanem", "birtanem", "hayatim",
        ),
        PhraseCategory.APOLOGY to listOf(
            "ozur dilerim", "ozur", "pardon", "kusura bakma", "kusura bakmayin", "affedersin", "sorry",
        ),
        PhraseCategory.YOU_ARE_RIGHT to listOf(
            "haklisin", "haklisiniz", "dogru diyorsun", "dogru soyluyorsun",
        ),
        PhraseCategory.ON_MY_WAY to listOf(
            "geliyorum", "yoldayim", "ciktim", "5dk", "5 dk", "5 dakika", "5 dakikaya",
        ),
        PhraseCategory.GOOD_MORNING to listOf(
            "gunaydin", "gunaydinlar", "gunaydn", "gnydn", "gun aydin", "hayirli sabahlar", "good morning",
        ),
        PhraseCategory.GOOD_NIGHT to listOf(
            "iyi geceler", "iyi gclr", "iyigeceler", "ii geceler", "iyi uykular", "tatli ruyalar", "good night",
        ),
        PhraseCategory.FRIDAY_GREETING to listOf(
            "hayirli cumalar", "hayirli cuma", "cumaniz mubarek olsun", "cumaniz mubarek", "cuman mubarek olsun",
        ),
        PhraseCategory.THANKS to listOf(
            "tesekkurler", "tesekkur ederim", "tesekkur ederiz", "tesekkur", "tsk", "tskler", "tsklr",
            "sagol", "sag ol", "sagolun", "sag olun", "eyvallah", "eyv", "mersi", "thanks", "thx",
        ),
        PhraseCategory.HOW_ARE_YOU to listOf(
            "naber", "nbr", "nasilsin", "nasilsiniz", "nasilsn", "napiyorsun", "napiyosun", "napiyon",
            "napion", "ne yapiyorsun", "ne yapiyosun", "ne var ne yok", "iyi misin", "nasil gidiyor",
            "keyifler nasil",
        ),
    )

    private val LOVE_EMOJI = listOf("❤", "♥", "😍", "🥰", "😘", "💕", "💖", "💗", "💓", "💞", "💘", "🫶")

    private class Entry(val tokens: Array<String>, val category: PhraseCategory)

    /** İlk kelimeye göre gruplu; her grup içinde uzun kalıplar önce. */
    private val BY_FIRST_TOKEN: Map<String, List<Entry>> = PHRASES
        .flatMap { (cat, phrases) ->
            phrases.map { phrase ->
                Entry(phrase.split(' ').map(TurkishText::collapseRepeats).toTypedArray(), cat)
            }
        }
        .sortedByDescending { it.tokens.size }
        .groupBy { it.tokens.first() }

    /** Tek mesaj için kolay kullanım; testler ve tekil çağrılar için. */
    fun count(text: String): Map<PhraseCategory, Int> {
        val result = HashMap<PhraseCategory, Int>()
        countInto(TokenizedText(text), result)
        return result
    }

    /**
     * Sayaçları [into] haritasına ekler. Analiz motoru tüm mesajlar için aynı haritayı
     * kullanır; mesaj başına yeni harita oluşturulmaz.
     */
    fun countInto(text: TokenizedText, into: MutableMap<PhraseCategory, Int>) {
        val words = text.collapsed
        var i = 0
        while (i < words.size) {
            val candidates = BY_FIRST_TOKEN[words[i]]
            var matched: Entry? = null
            if (candidates != null) {
                for (entry in candidates) {
                    if (matches(entry.tokens, words, i)) {
                        matched = entry
                        break
                    }
                }
            }
            if (matched != null) {
                into.merge(matched.category, 1, Int::plus)
                i += matched.tokens.size
            } else {
                i++
            }
        }
        val hearts = countLoveEmoji(text.raw)
        if (hearts > 0) into.merge(PhraseCategory.LOVE, hearts, Int::plus)
    }

    private fun matches(tokens: Array<String>, words: List<String>, start: Int): Boolean {
        if (tokens.size > words.size - start) return false
        for (k in 1 until tokens.size) { // ilk kelime zaten eşleşti
            if (words[start + k] != tokens[k]) return false
        }
        return true
    }

    private fun countLoveEmoji(text: String): Int {
        // Hızlı yol: kalp emojilerinin hepsi ya vekil çift ya da ❤ / ♥.
        if (text.none { it.isHighSurrogate() || it == '❤' || it == '♥' }) return 0
        return LOVE_EMOJI.sumOf { countOccurrences(text, it) }
    }
}
