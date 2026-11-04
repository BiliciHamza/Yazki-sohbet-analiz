package com.hamzabilici.yazki.data

import com.hamzabilici.yazki.core.analysis.ChatAnalyzer
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.core.parser.WhatsAppParser
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsJsonTest {

    @Test
    fun roundTripKeepsEveryField() {
        val stats = sampleStats()
        val text = StatsJson.encode(stats).toString()
        val decoded = StatsJson.decode(JSONObject(text))
        assertEquals(stats, decoded)
    }

    @Test
    fun groupChatWithAwardsRoundTrips() {
        val stats = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                [01.11.2026 10:00:00] Ali: günaydın knk
                [01.11.2026 10:00:30] Ayşe: günaydın abi 😂
                [01.11.2026 10:01:00] Can: PTT-20261101-WA0001.opus (dosya ekli)
                [01.11.2026 10:02:00] Ali: tamam
                """.trimIndent(),
            ),
        )
        assertTrue(stats.awards.isNotEmpty())
        assertEquals(stats, StatsJson.decode(JSONObject(StatsJson.encode(stats).toString())))
    }

    @Test
    fun unknownAndMissingFieldsAreTolerated() {
        // Eski sürüm kaydı: yeni alanlar yok. Gelecek sürüm kaydı: bilinmeyen enum ve alanlar var.
        val json = JSONObject(
            """
            {"participants":[{"name":"Ali","messages":3,
              "phrases":{"OKAY":2,"FUTURE_CATEGORY":5},
              "media":{"IMAGE":1,"HOLOGRAM":9}}],
             "totals":{"messages":3},
             "time":{"first":"2026-11-01T10:00","heatmap":[[1]]},
             "warnings":[{"type":"SOMETHING_NEW"}],
             "brandNewField":true}
            """.trimIndent(),
        )
        val s = StatsJson.decode(json)
        val ali = s.participants.single()
        assertEquals(3, ali.messages)
        assertEquals(0, ali.dayStarts)
        assertEquals(mapOf(com.hamzabilici.yazki.core.turkish.PhraseCategory.OKAY to 2), ali.phrases)
        assertEquals(1, ali.media.size)
        assertTrue(ali.addressTerms.isEmpty())
        assertEquals(7, s.time.heatmap.size)
        assertEquals(24, s.time.heatmap[0].size)
        assertEquals(1, s.time.heatmap[0][0])
        assertTrue(s.warnings.isEmpty())
        assertTrue(s.minutePrecision)
    }

    companion object {
        fun sampleStats(): ChatStats = ChatAnalyzer().analyze(
            WhatsAppParser.parse(
                """
                01.11.2026 10:00 - Ali: Günaydın knk! Kahve içelim mi?
                01.11.2026 10:02 - Ayşe: sjsjsj olur tamam 😂 sevgilimmm
                01.11.2026 10:03 - Ali: <Medya dahil edilmedi>
                01.11.2026 10:20 - Ali: geliyorum
                02.11.2026 02:30 - Ali: NEREDESİN
                02.11.2026 02:40 - Ayşe: https://www.ornek.com.tr/yol bak
                03.11.2026 09:00 - Ayşe: Bu mesaj silindi
                05.11.2026 09:00 - Ali: iyi geceler ❤️
                """.trimIndent(),
            ),
        )
    }
}
