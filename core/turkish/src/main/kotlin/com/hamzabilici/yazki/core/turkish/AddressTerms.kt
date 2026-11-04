package com.hamzabilici.yazki.core.turkish

/**
 * Hitaplar: "knk", "aşkım", "sevgilim", "hocam" gibi kişilere seslenme biçimleri.
 *
 * Her hitap bir kanonik biçimde toplanır ("knk", "kanki", "kankaaa" → "kanka").
 * Uzatılmış yazımlar ("sevgilimmmm", "kankaaa") [TurkishText.collapseRepeats] sayesinde tanınır.
 * Sayım basittir: kelimenin mesajda geçmesi yeterlidir, cümledeki yeri aranmaz.
 */
object AddressTerms {

    /** Kanonik (gösterilecek) biçim → katlanmış yazım çeşitleri. */
    private val GROUPS: Map<String, List<String>> = linkedMapOf(
        "kanka" to listOf("kanka", "knk", "kanki", "kanks", "kankam", "knkm", "kankacim"),
        "aşkım" to listOf("askim", "askm", "asko", "askimsin"),
        "sevgilim" to listOf("sevgilim", "sevgilm"),
        "canım" to listOf("canim", "cnm", "canimsin"),
        "hayatım" to listOf("hayatim"),
        "bebeğim" to listOf("bebegim", "bebis", "bebisim"),
        "bir tanem" to listOf("birtanem", "bitanem"),
        "tatlım" to listOf("tatlim"),
        "güzelim" to listOf("guzelim"),
        "kardeşim" to listOf("kardesim", "kardes", "krdsm", "kardo", "bro"),
        "abi" to listOf("abi", "abim", "abicim", "abicigim"),
        "abla" to listOf("abla", "ablam", "ablacim", "ablacigim"),
        "hocam" to listOf("hocam"),
        "reis" to listOf("reis", "reisim"),
        "kral" to listOf("kral", "kralim"),
        "dostum" to listOf("dostum"),
        "moruk" to listOf("moruk"),
    )

    /** Tekrarsız yazım → kanonik biçim. */
    private val LOOKUP: Map<String, String> = HashMap<String, String>().apply {
        for ((canonical, variants) in GROUPS) {
            for (v in variants) put(TurkishText.collapseRepeats(v), canonical)
        }
    }

    /** Tanınan bütün kanonik hitaplar, sabit sırayla. */
    val all: List<String> get() = GROUPS.keys.toList()

    fun canonicalOf(collapsedWord: String): String? = LOOKUP[collapsedWord]

    fun count(text: String): Map<String, Int> {
        val result = HashMap<String, Int>()
        countInto(TokenizedText(text), result)
        return result
    }

    fun countInto(text: TokenizedText, into: MutableMap<String, Int>) {
        for (w in text.collapsed) {
            val canonical = LOOKUP[w] ?: continue
            into.merge(canonical, 1, Int::plus)
        }
    }
}
