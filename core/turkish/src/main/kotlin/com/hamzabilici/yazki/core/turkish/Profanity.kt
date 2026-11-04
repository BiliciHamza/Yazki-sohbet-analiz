package com.hamzabilici.yazki.core.turkish

import java.util.Locale

/**
 * Türkçe küfür tespiti.
 *
 * Tasarım ilkeleri:
 * - **Yanlış pozitif yerine eksik sayım.** "sık", "sıkıldım", "sıkıntı", "götür", "yara", "amin"
 *   gibi masum kelimeler asla küfür sayılmaz. Bu yüzden kelimeler ASCII'ye katlanmaz: "ı" ile "i"
 *   ayrı tutulur ("sıkıntı" ≠ "sik..."). Belirsiz kısa biçimler ("sik", "sikti") listede yok.
 * - **Uzatmalar tanınır.** Hem sözlük hem kelime [TurkishText.collapseRepeats] ile tekrarsız
 *   hale getirilir: "amkkkk" → "amk", "siktirrr" → "siktir", "orospuuu" → "orospu".
 * - **Ekler tanınır.** Kökler önek olarak eşleşir: "orospuluk", "şerefsizler", "ibnelik".
 *   Önek olarak tehlikeli kökler ("göt" → "götür") yalnızca tam biçimleriyle listededir.
 *
 * Kelimenin kendisi yalnızca kullanıcı sansürü kapattığında gösterilir; sayım her durumda yapılır.
 */
object Profanity {

    /** Tam eşleşmesi gereken biçimler (kısaltmalar ve önek olarak tehlikeli kökler). */
    private val EXACT_RAW = listOf(
        // kısaltmalar
        "amk", "amq", "aq", "mk", "mq", "sg", "sktr", "sktir", "oç", "oc",
        // "am" ailesi (yalnızca açık biçimler; ASCII "amin" = "amin/amen" olduğu için yok)
        "amın", "amcık",
        // "göt" ailesi: önek olarak "götür", "götürmek" ile çakışır
        "göt", "götü", "götün", "götünü", "götüne", "götten", "götte", "götler",
        // "bok" ailesi: önek olarak "bokser" gibi kelimelerle çakışmasın
        "bok", "boktan", "boku", "bokum", "boka", "bokunu",
        // ASCII biçimi tek başına anlamlı olanlar
        "pust", "sikimi", "sikime",
    )

    /** Önek olarak eşleşen kökler (ekli biçimleri de yakalar). */
    private val PREFIX_RAW = listOf(
        // s*k ailesi: yalnızca belirsiz olmayan biçimler ("sıkıldım", "sıkıntı", "sıktı" hariç)
        "siktir", "siktiğ", "siktig", "siker", "sikey", "sikeceğ", "sikeceg", "sikicem", "sikim",
        "sikik", "sikiş", "sikiyim", "hassiktir", "hasiktir",
        // am ailesi
        "amına", "amina", "amını", "amınak", "aminak", "amcık", "amcik", "amcığ", "amcig", "amk",
        // orospu ailesi
        "orospu", "oruspu", "orosbu", "orspu", "orusbu",
        // yarrak ailesi (tekrarsız biçimde yazılır: "yarrak" → "yarak")
        "yarrak", "yarrağ", "yarrag", "dalyarrak", "dallama",
        // diğerleri
        "piç", "yavşak", "yavsak", "ibne", "kahpe", "pezevenk", "şerefsiz", "serefsiz", "gavat",
        "taşşak", "taşak", "tasak", "sürtük", "surtuk", "kaltak", "puşt", "götveren", "götver",
        "gotveren", "götlek", "götoş", "kancık", "kancik",
    )

    private val EXACT: Set<String> = EXACT_RAW.mapTo(HashSet()) { TurkishText.collapseRepeats(it) }

    /** İlk harfe göre gruplu önekler; her kelimede bütün listeyi taramamak için. */
    private val PREFIXES: Map<Char, List<String>> = PREFIX_RAW
        .map { TurkishText.collapseRepeats(it) }
        .distinct()
        .groupBy { it.first() }

    /**
     * Kelime küfürse tekrarsız küçük harfli anahtarını döner ("AMKKK" → "amk"), değilse null.
     *
     * @param word Asıl yazım (büyük "I" kontrolü için).
     * @param lower Türkçe küçük harfli yazım.
     */
    fun match(word: String, lower: String): String? {
        val key = TurkishText.collapseRepeats(lower)
        if (isProfaneKey(key)) return key
        // Büyük harfle ASCII yazan biri "SIKERIM" yazınca Türkçe küçültme "sıkerım" verir;
        // İngilizce kuralıyla da dene. "SIKILDIM" → "sikildim" yine listede olmadığı için güvenli.
        if (word.indexOf('I') >= 0) {
            val alt = TurkishText.collapseRepeats(word.lowercase(Locale.ROOT))
            if (isProfaneKey(alt)) return alt
        }
        return null
    }

    fun isProfane(word: String): Boolean = match(word, TurkishText.lowercase(word)) != null

    private fun isProfaneKey(key: String): Boolean {
        if (key.isEmpty()) return false
        if (key in EXACT) return true
        val candidates = PREFIXES[key[0]] ?: return false
        for (p in candidates) if (key.startsWith(p)) return true
        return false
    }

    /** Bir mesajdaki küfürleri [into] haritasına ekler; toplam sayıyı döner. */
    fun countInto(text: TokenizedText, into: MutableMap<String, Int>): Int {
        var n = 0
        for (i in text.words.indices) {
            val key = match(text.words[i], text.lower[i]) ?: continue
            into.merge(key, 1, Int::plus)
            n++
        }
        return n
    }
}
