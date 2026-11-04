package com.hamzabilici.yazki.core.analysis

import com.hamzabilici.yazki.core.parser.MediaType
import com.hamzabilici.yazki.core.parser.WhatsAppParser
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

class ChatAnalyzerTest {

    private val chat = WhatsAppParser.parse(
        """
        01.11.2026 10:00 - Mesajlar ve aramalar uçtan uca şifrelidir.
        01.11.2026 10:00 - Ali: Günaydın! Kahve içelim mi?
        01.11.2026 10:02 - Ayşe: sjsjsj olur tamam 😂
        01.11.2026 10:03 - Ali: <Medya dahil edilmedi>
        01.11.2026 10:20 - Ali: geliyorum
        01.11.2026 10:21 - Ayşe: Özür dilerim geç kaldım
        02.11.2026 02:30 - Ali: NEREDESİN
        02.11.2026 02:40 - Ayşe: https://www.ornek.com.tr/yol bak
        03.11.2026 09:00 - Ayşe: Bu mesaj silindi
        05.11.2026 09:00 - Ali: kahve ❤️
        """.trimIndent(),
    )

    private val stats = ChatAnalyzer().analyze(chat)
    private val ali get() = stats.participants.first { it.name == "Ali" }
    private val ayse get() = stats.participants.first { it.name == "Ayşe" }

    @Test
    fun totalsExcludeSystemMessages() {
        assertEquals(9, stats.totals.messages)
        assertEquals(1, stats.totals.media)
        assertEquals(1, stats.totals.deleted)
        assertEquals(1, stats.totals.links)
        assertEquals(listOf("Ali", "Ayşe"), stats.participants.map { it.name })
        assertEquals(5, ali.messages)
        assertEquals(4, ayse.messages)
        assertEquals(1, ali.media[MediaType.UNKNOWN])
    }

    @Test
    fun conversationStartsAndEnds() {
        // Sohbetler: 01.11 10:00, 02.11 02:30, 03.11 09:00, 05.11 09:00 (6 saatten uzun boşluklar)
        assertEquals(3, ali.conversationsStarted)
        assertEquals(1, ayse.conversationsStarted)
        assertEquals(4, ali.conversationsEnded + ayse.conversationsEnded)
    }

    @Test
    fun replyTimesAndDoubleTexts() {
        // Ayşe'nin cevapları: 2 dk, 1 dk, 10 dk → medyan 2 dk
        assertEquals(Duration.ofMinutes(2), ayse.medianReply)
        // Ali: 10:03 → 10:20 arası 17 dk, cevap gelmeden tekrar yazdı
        assertEquals(1, ali.doubleTexts)
    }

    @Test
    fun turkishLayer() {
        assertEquals(2, ayse.laughter)
        assertEquals(1, ayse.phrases[PhraseCategory.APOLOGY])
        assertEquals(2, ayse.phrases[PhraseCategory.OKAY])
        assertEquals(1, ali.phrases[PhraseCategory.ON_MY_WAY])
        assertEquals(1, ali.phrases[PhraseCategory.LOVE])
        assertEquals(1, ali.shouts)
        assertEquals(1, ali.questions)
        assertTrue(ali.topWords.any { it.key == "kahve" && it.count == 2 })
    }

    @Test
    fun timeStats() {
        val t = stats.time
        assertEquals(5L, t.spanDays)
        assertEquals(4, t.activeDays)
        assertEquals(LocalDate.of(2026, 11, 1), t.busiestDay?.date)
        assertEquals(5, t.busiestDay?.count)
        assertEquals(3, t.longestStreak?.days)
        assertEquals(LocalDate.of(2026, 11, 3).atTime(9, 0), t.longestSilence?.from)
        // 01.11.2026 Pazar, saat 10
        assertEquals(5, t.heatmap[DayOfWeek.SUNDAY.value - 1][10])
        assertEquals(2.0 / 9, t.nightRatio, 1e-9)
        assertEquals(1, ali.nightMessages)
        assertEquals(1, ayse.nightMessages)
    }

    @Test
    fun emojisAndDomains() {
        assertEquals("ornek.com.tr", stats.topDomains.single().key)
        assertTrue(stats.topEmojis.any { it.key == "😂" })
        assertTrue("Varyasyon seçici atılmalı", stats.topEmojis.any { it.key == "❤" })
    }

    @Test
    fun androidChatIsMinutePrecision() {
        assertTrue(stats.minutePrecision)
    }

    @Test
    fun dayStartsAndAddressTerms() {
        // Günlerin ilk mesajı: 01.11 Ali, 02.11 Ali, 03.11 Ayşe, 05.11 Ali
        assertEquals(3, ali.dayStarts)
        assertEquals(1, ayse.dayStarts)

        val s = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                [01.11.2026 09:00:00] Ali: Günaydınnn knk
                [01.11.2026 09:00:20] Ayşe: günaydın kankaaa, sevgilim nerede
                [01.11.2026 09:01:00] Ali: iyi geceler abi
                """.trimIndent(),
            ),
        )
        val a = s.participants.first { it.name == "Ali" }
        val b = s.participants.first { it.name == "Ayşe" }
        assertTrue("iOS saniyeli", !s.minutePrecision)
        assertEquals(mapOf("kanka" to 1, "abi" to 1), a.addressTerms)
        assertEquals(1, b.addressTerms["kanka"])
        assertEquals(1, b.addressTerms["sevgilim"])
        assertEquals(1, a.phrases[PhraseCategory.GOOD_MORNING])
        assertEquals(1, a.phrases[PhraseCategory.GOOD_NIGHT])
        assertEquals(Duration.ofSeconds(20), b.medianReply)
    }

    @Test
    fun conversationStartsAfterThreeHourGap() {
        val s = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                01.11.2026 09:00 - Ali: günaydın
                01.11.2026 09:05 - Ayşe: günaydın
                01.11.2026 13:00 - Ayşe: öğle arası
                01.11.2026 13:01 - Ali: tamam
                01.11.2026 15:30 - Ali: çıktın mı
                01.11.2026 20:00 - Ayşe: geldim
                """.trimIndent(),
            ),
        )
        // 09:00 Ali (ilk), 13:00 Ayşe (3s 55dk), 15:30 Ali (2s 29dk: sayılmaz), 20:00 Ayşe (4s 30dk)
        assertEquals(1, s.participants.first { it.name == "Ali" }.conversationsStarted)
        assertEquals(2, s.participants.first { it.name == "Ayşe" }.conversationsStarted)
    }

    @Test
    fun profanityIsCountedAndKeptOutOfTopWords() {
        val s = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                01.11.2026 10:00 - Ali: amkkk yine mi trafik
                01.11.2026 10:01 - Ali: siktirrr trafik amk
                01.11.2026 10:02 - Ayşe: sıkıldım ya trafik
                """.trimIndent(),
            ),
        )
        val ali = s.participants.first { it.name == "Ali" }
        val ayse = s.participants.first { it.name == "Ayşe" }
        assertEquals(3, ali.profanity)
        assertEquals(listOf(Counted("amk", 2), Counted("siktir", 1)), ali.topSwears)
        assertTrue(ali.topWords.none { it.key.startsWith("amk") || it.key.startsWith("siktir") })
        assertTrue(ali.topWords.any { it.key == "trafik" })
        assertEquals(0, ayse.profanity)
        assertEquals(3, s.totals.profanity)
    }

    @Test
    fun afterHoursCountsEveningsAndWeekends() {
        val s = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                02.11.2026 10:00 - Ali: mesai içi (Pazartesi)
                02.11.2026 19:30 - Ali: mesai dışı
                02.11.2026 08:15 - Ali: sabah erken
                07.11.2026 11:00 - Ali: Cumartesi
                """.trimIndent(),
            ),
        )
        assertEquals(3, s.participants.single().afterHoursMessages)
    }

    @Test
    fun noAwardsInOneToOneChat() {
        assertTrue(stats.awards.isEmpty())
    }

    @Test
    fun groupAwards() {
        val group = WhatsAppParser.parse(
            """
            01.11.2026 10:00 - Ali: a
            01.11.2026 10:01 - Ayşe: b
            01.11.2026 10:02 - Ali: c
            01.11.2026 10:03 - Ali: d
            01.11.2026 10:04 - Can: x
            01.11.2026 10:05 - Can: PTT-20261101-WA0001.opus (dosya ekli)
            """.trimIndent(),
        )
        val s = ChatAnalyzer().analyze(group)
        assertTrue(s.isGroup)
        assertEquals("Ali", s.awards.first { it.type == AwardType.MOST_MESSAGES }.winner)
        assertEquals("Can", s.awards.first { it.type == AwardType.MOST_AUDIO }.winner)
    }

    @Test
    fun nearExportLimitWarning() {
        val s = ChatAnalyzer(AnalysisConfig(exportLimitWarningThreshold = 5)).analyze(chat)
        assertTrue(s.warnings.any { it is AnalysisWarning.NearExportLimit })
    }
}
