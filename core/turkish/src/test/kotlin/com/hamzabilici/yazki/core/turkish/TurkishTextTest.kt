package com.hamzabilici.yazki.core.turkish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TurkishTextTest {

    @Test
    fun turkishLowercase() {
        assertEquals("ıspanak istanbul", TurkishText.lowercase("ISPANAK İSTANBUL"))
    }

    @Test
    fun foldMakesSpellingsEqual() {
        assertEquals("ozur dilerim", TurkishText.fold("ÖZÜR DİLERİM"))
        assertEquals("ozur dilerim", TurkishText.fold("Özür dilerim"))
        assertEquals("cagri", TurkishText.fold("Çağrı"))
    }

    @Test
    fun wordsDropSuffixAfterApostropheAndUrls() {
        assertEquals(
            listOf("İstanbul", "gidiyoruz", "bak"),
            TurkishText.words("İstanbul'da gidiyoruz! bak: https://ornek.com/a?b=1"),
        )
    }

    @Test
    fun squeezeRepeats() {
        assertEquals("tamam", TurkishText.squeezeRepeats("tamaaaam"))
        assertEquals("kusura", TurkishText.squeezeRepeats("kusura"))
        assertEquals("okey", TurkishText.squeezeRepeats("okeyyyy"))
    }

    @Test
    fun laughterWords() {
        listOf("sjsjsj", "ksksks", "jsjsjs", "kdkdkd", "skdjsk", "hahaha", "ahahah", "hehe", "asdfgh")
            .forEach { assertTrue(it, Laughter.isLaughterWord(it)) }
        listOf("salak", "dalga", "daha", "kafa", "hasta", "selam", "aha", "haklisin")
            .forEach { assertFalse(it, Laughter.isLaughterWord(it)) }
    }

    @Test
    fun laughterCountIncludesEmoji() {
        assertEquals(3, Laughter.count("SJSJSJ 😂😂 çok iyi"))
    }

    @Test
    fun phraseCounter() {
        val counts = PhraseCounter.count("Özür dilerim, tamaaam geliyorum 5 dk ❤️❤️")
        assertEquals(1, counts[PhraseCategory.APOLOGY])
        assertEquals(1, counts[PhraseCategory.OKAY])
        assertEquals(2, counts[PhraseCategory.ON_MY_WAY])
        assertEquals(2, counts[PhraseCategory.LOVE])
    }

    @Test
    fun phraseCounterDoesNotDoubleCount() {
        val counts = PhraseCounter.count("seni seviyorum")
        assertEquals(1, counts[PhraseCategory.LOVE])
    }

    @Test
    fun collapseRepeats() {
        assertEquals("sevgilim", TurkishText.collapseRepeats("sevgilimmmm"))
        assertEquals("gunaydin", TurkishText.collapseRepeats("gunaydinnn"))
        assertEquals("tamam", TurkishText.collapseRepeats("tamaam"))
        val same = "kanka"
        assertTrue("Tekrar yoksa aynı nesne dönmeli", same === TurkishText.collapseRepeats(same))
    }

    @Test
    fun greetingsAndPolitenessWithElongation() {
        val counts = PhraseCounter.count("GÜNAYDINNNN canım! iyi geceleeer. Teşekkürler, nbr? Hayırlı cumalar")
        assertEquals(1, counts[PhraseCategory.GOOD_MORNING])
        assertEquals(1, counts[PhraseCategory.GOOD_NIGHT])
        assertEquals(1, counts[PhraseCategory.THANKS])
        assertEquals(1, counts[PhraseCategory.HOW_ARE_YOU])
        assertEquals(1, counts[PhraseCategory.FRIDAY_GREETING])
    }

    @Test
    fun addressTerms() {
        val counts = AddressTerms.count("knk naber, kankaaa gel. Sevgilimmmm özledim, aşkımm, ABİ")
        assertEquals(2, counts["kanka"])
        assertEquals(1, counts["sevgilim"])
        assertEquals(1, counts["aşkım"])
        assertEquals(1, counts["abi"])
        assertEquals(null, counts["canım"])
    }

    @Test
    fun profanityIsRecognizedWithElongationsAndSuffixes() {
        listOf(
            "amk", "AMKKKK", "aq", "amq", "SİKTİRRR", "siktir", "orospuuu", "orospuluk", "Amına", "amina",
            "piç", "yavşaklık", "şerefsizler", "serefsiz", "oç", "göt", "götveren", "ibne", "SIKERIM",
            "sikeyim", "hassiktir", "yarrrrak", "yarrağı", "taşşak", "kahpe", "pezevenk", "puşt", "pust",
        ).forEach { assertTrue(it, Profanity.isProfane(it)) }
    }

    @Test
    fun innocentWordsAreNeverProfanity() {
        listOf(
            "sıkıldım", "sikildim", "sıkıntı", "sikinti", "sık", "sik", "sıktı", "sikti", "sıkıcı", "sikici",
            "SIKILDIM", "sıkıştır", "sikistir", "götür", "götürdüm", "yara", "yaramaz", "amin", "Amin",
            "amca", "ambulans", "bokser", "piknik", "ibadet", "kalkan", "orman", "pasta", "taşa", "dalga",
            "gotur", "amcam", "sıkı", "sikma",
        ).forEach { assertFalse(it, Profanity.isProfane(it)) }
    }

    @Test
    fun stopWords() {
        assertTrue(StopWords.isStopWord("ve"))
        assertTrue(StopWords.isStopWord("null"))
        assertTrue(StopWords.isStopWord("2026"))
        assertFalse(StopWords.isStopWord("kahve"))
    }
}
