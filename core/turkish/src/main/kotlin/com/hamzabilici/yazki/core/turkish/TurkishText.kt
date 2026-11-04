package com.hamzabilici.yazki.core.turkish

import java.util.Locale

/** Türkçeye duyarlı temel metin işlemleri. */
object TurkishText {

    val LOCALE: Locale = Locale.forLanguageTag("tr-TR")

    private val URL = Regex("""(?:https?://|www\.)\S+""", RegexOption.IGNORE_CASE)

    /** [fold] işleminin değiştirdiği karakterler. */
    private const val FOLD_CHARS = "çğıöşüâîû̇"

    /** Türkçe kurallarıyla küçük harf: "I" → "ı", "İ" → "i". */
    fun lowercase(text: String): String = text.lowercase(LOCALE)

    /**
     * Küçük harfe çevirip Türkçe karakterleri ASCII karşılıklarına indirger.
     * "Özür dilerim", "ozur dilerim" ve "ÖZÜR DİLERİM" aynı anahtara düşer.
     */
    fun fold(text: String): String = foldLowercase(lowercase(text))

    /** [fold] ile aynı, ama metin zaten Türkçe küçük harfe çevrilmiş; tekrar küçültmez. */
    fun foldLowercase(lower: String): String {
        // Hızlı yol: Türkçe karakter yoksa küçük harfli hali zaten anahtardır.
        if (lower.none { it in FOLD_CHARS }) return lower
        val sb = StringBuilder(lower.length)
        for (ch in lower) {
            sb.append(
                when (ch) {
                    'ç' -> 'c'
                    'ğ' -> 'g'
                    'ı' -> 'i'
                    'ö' -> 'o'
                    'ş' -> 's'
                    'ü' -> 'u'
                    'â' -> 'a'
                    'î' -> 'i'
                    'û' -> 'u'
                    '̇' -> continue // "İ" bazı ortamlarda i + birleşik nokta olarak gelir
                    else -> ch
                },
            )
        }
        return sb.toString()
    }

    /** Üç ve daha fazla tekrar eden harfi teke indirir: "tamaaaam" → "tamam", "okeyyy" → "okey". */
    fun squeezeRepeats(word: String): String {
        if (word.length < 3) return word
        var hasRun = false
        for (i in 2 until word.length) {
            if (word[i] == word[i - 1] && word[i] == word[i - 2]) {
                hasRun = true
                break
            }
        }
        if (!hasRun) return word // çoğu kelimede kopya oluşturmadan dön
        val sb = StringBuilder(word.length)
        var i = 0
        while (i < word.length) {
            val ch = word[i]
            var j = i
            while (j < word.length && word[j] == ch) j++
            val run = j - i
            if (run >= 3) sb.append(ch) else repeat(run) { sb.append(ch) }
            i = j
        }
        return sb.toString()
    }

    /**
     * Tekrar eden her harfi teke indirir: "sevgilimmm" → "sevgilim", "günaydınnn" → "günaydın",
     * "tamaam" → "tamam". Sözlük eşleştirmesinde hem sözlük hem kelime bu işlemden geçer,
     * böylece "teşekkürler" (kk) gibi gerçek çift harfli kelimeler de tutarlı eşleşir.
     */
    fun collapseRepeats(word: String): String {
        if (word.length < 2) return word
        var prev = word[0]
        var changed = false
        for (i in 1 until word.length) {
            if (word[i] == prev) {
                changed = true
                break
            }
            prev = word[i]
        }
        if (!changed) return word // çoğu kelimede kopya oluşturmadan dön
        val sb = StringBuilder(word.length)
        sb.append(word[0])
        for (i in 1 until word.length) {
            if (word[i] != word[i - 1]) sb.append(word[i])
        }
        return sb.toString()
    }

    fun removeUrls(text: String): String {
        // Hızlı yol: mesajların çok azında bağlantı var; regex'i yalnızca gerektiğinde çalıştır.
        if (!mayContainUrl(text)) return text
        return URL.replace(text, " ")
    }

    fun mayContainUrl(text: String): Boolean =
        text.contains("://") || text.contains("www.", ignoreCase = true)

    /**
     * Metni kelimelere böler. Harf ve rakam dizileri kelimedir; kesme işaretinden sonraki
     * ek atılır ("İstanbul'da" → "İstanbul"). Bağlantılar kelime sayılmaz.
     * Büyük/küçük harf korunur; gerekirse çağıran [lowercase] veya [fold] uygular.
     */
    fun words(text: String): List<String> {
        val clean = removeUrls(text)
        val result = ArrayList<String>()
        val current = StringBuilder()
        var skippingSuffix = false
        var i = 0
        while (i < clean.length) {
            val cp = clean.codePointAt(i)
            val len = Character.charCount(cp)
            when {
                Character.isLetterOrDigit(cp) || cp == 0x0307 -> {
                    if (!skippingSuffix) current.appendCodePoint(cp)
                }
                (cp == '\''.code || cp == '’'.code) && current.isNotEmpty() -> {
                    skippingSuffix = true
                }
                else -> {
                    if (current.isNotEmpty()) result += current.toString()
                    current.setLength(0)
                    skippingSuffix = false
                }
            }
            i += len
        }
        if (current.isNotEmpty()) result += current.toString()
        return result
    }

    /** [words] + [fold] + [squeezeRepeats]: kahkaha tespiti için anahtarlar. */
    fun foldedWords(text: String): List<String> = words(text).map { squeezeRepeats(fold(it)) }
}

/**
 * Bir mesajın tek seferde hazırlanmış kelime biçimleri. Analiz motoru her mesajı
 * yalnızca bir kez bölüp katlar; sayaçlar bu listeleri paylaşır.
 */
class TokenizedText(val raw: String) {
    /** Asıl yazımıyla kelimeler. */
    val words: List<String> = TurkishText.words(raw)

    /** Türkçe küçük harf, katlama yok: "Sıkıldım" → "sıkıldım". Küfür tespiti ı/i farkına ihtiyaç duyar. */
    val lower: List<String> = words.map(TurkishText::lowercase)

    /** Küçük harf + ASCII katlama: "Özür" → "ozur". */
    val folded: List<String> = lower.map(TurkishText::foldLowercase)

    /** Kahkaha tespiti için: 3+ tekrar teke iner ("hahahaaa" → "hahaha"). */
    val squeezed: List<String> = folded.map(TurkishText::squeezeRepeats)

    /** Sözlük eşleştirmesi için: her tekrar teke iner ("sevgilimmm" → "sevgilim"). */
    val collapsed: List<String> = folded.map(TurkishText::collapseRepeats)
}
