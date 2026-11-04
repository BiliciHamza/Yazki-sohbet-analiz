package com.hamzabilici.yazki.data

import com.hamzabilici.yazki.core.analysis.AnalysisWarning
import com.hamzabilici.yazki.core.analysis.AwardType
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.core.analysis.Counted
import com.hamzabilici.yazki.core.analysis.DayCount
import com.hamzabilici.yazki.core.analysis.GroupAward
import com.hamzabilici.yazki.core.analysis.MonthCount
import com.hamzabilici.yazki.core.analysis.ParticipantStats
import com.hamzabilici.yazki.core.analysis.Silence
import com.hamzabilici.yazki.core.analysis.Streak
import com.hamzabilici.yazki.core.analysis.TimeStats
import com.hamzabilici.yazki.core.analysis.Totals
import com.hamzabilici.yazki.core.parser.MediaType
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * [ChatStats] ↔ JSON. Kayıtlı sohbetler bu biçimde saklanır.
 *
 * Okuma hoşgörülüdür: eksik alanlar varsayılan değer alır, tanınmayan enum adları atlanır.
 * Böylece yeni sürümlerde alan eklemek eski kayıtları bozmaz. Yalnızca hesaplanmış
 * istatistikler yazılır; mesaj metinleri hiçbir zaman saklanmaz.
 */
internal object StatsJson {

    const val VERSION = 1

    // ---------------------------------------------------------------- yazma

    fun encode(s: ChatStats): JSONObject = JSONObject()
        .put("participants", JSONArray().apply { s.participants.forEach { put(encodeParticipant(it)) } })
        .put("totals", encodeTotals(s.totals))
        .put("time", encodeTime(s.time))
        .put("topEmojis", encodeCounted(s.topEmojis))
        .put("topDomains", encodeCounted(s.topDomains))
        .put("awards", JSONArray().apply {
            s.awards.forEach { a ->
                put(JSONObject().put("type", a.type.name).put("winner", a.winner).putFinite("value", a.value))
            }
        })
        .put("warnings", JSONArray().apply { s.warnings.forEach { put(encodeWarning(it)) } })
        .put("minutePrecision", s.minutePrecision)

    private fun encodeParticipant(p: ParticipantStats): JSONObject = JSONObject()
        .put("name", p.name)
        .put("messages", p.messages)
        .put("words", p.words)
        .put("characters", p.characters)
        .put("textMessages", p.textMessages)
        .put("media", encodeEnumMap(p.media))
        .put("deleted", p.deleted)
        .put("edited", p.edited)
        .put("links", p.links)
        .put("questions", p.questions)
        .put("exclamations", p.exclamations)
        .put("shouts", p.shouts)
        .put("emojiMessages", p.emojiMessages)
        .put("laughter", p.laughter)
        .put("nightMessages", p.nightMessages)
        .put("dayStarts", p.dayStarts)
        .put("phrases", encodeEnumMap(p.phrases))
        .put("addressTerms", encodeStringMap(p.addressTerms))
        .put("topWords", encodeCounted(p.topWords))
        .put("topEmojis", encodeCounted(p.topEmojis))
        .put("started", p.conversationsStarted)
        .put("ended", p.conversationsEnded)
        .put("doubleTexts", p.doubleTexts)
        .put("profanity", p.profanity)
        .put("topSwears", encodeCounted(p.topSwears))
        .put("afterHours", p.afterHoursMessages)
        .apply {
            p.medianReply?.let { put("medianReplySec", it.seconds) }
            p.ghostRatio?.let { putFinite("ghostRatio", it) }
        }

    private fun encodeTotals(t: Totals): JSONObject = JSONObject()
        .put("messages", t.messages)
        .put("words", t.words)
        .put("characters", t.characters)
        .put("media", t.media)
        .put("deleted", t.deleted)
        .put("edited", t.edited)
        .put("links", t.links)
        .put("laughter", t.laughter)
        .put("profanity", t.profanity)

    private fun encodeTime(t: TimeStats): JSONObject = JSONObject().apply {
        t.first?.let { put("first", it.toString()) }
        t.last?.let { put("last", it.toString()) }
        put("spanDays", t.spanDays)
        put("activeDays", t.activeDays)
        t.busiestDay?.let { put("busiestDay", JSONObject().put("date", it.date.toString()).put("count", it.count)) }
        put("heatmap", JSONArray().apply { t.heatmap.forEach { row -> put(JSONArray().apply { row.forEach { put(it) } }) } })
        put("monthly", JSONArray().apply {
            t.monthly.forEach { m ->
                put(JSONObject().put("month", m.month.toString()).put("total", m.total).put("by", encodeStringMap(m.byParticipant)))
            }
        })
        t.longestStreak?.let {
            put("streak", JSONObject().put("start", it.start.toString()).put("end", it.end.toString()).put("days", it.days))
        }
        t.longestSilence?.let { put("silence", JSONObject().put("from", it.from.toString()).put("to", it.to.toString())) }
        putFinite("nightRatio", t.nightRatio)
        putFinite("earlyRatio", t.earlyRatio)
    }

    private fun encodeWarning(w: AnalysisWarning): JSONObject = when (w) {
        is AnalysisWarning.NearExportLimit -> JSONObject().put("type", "NEAR_LIMIT").put("count", w.messageCount)
        is AnalysisWarning.UnparsedLines ->
            JSONObject().put("type", "UNPARSED").put("count", w.count).putFinite("ratio", w.ratio)
        AnalysisWarning.NoMessages -> JSONObject().put("type", "NO_MESSAGES")
    }

    private fun encodeCounted(list: List<Counted>): JSONArray =
        JSONArray().apply { list.forEach { put(JSONObject().put("k", it.key).put("n", it.count)) } }

    private fun <E : Enum<E>> encodeEnumMap(map: Map<E, Int>): JSONObject =
        JSONObject().apply { map.forEach { (k, v) -> put(k.name, v) } }

    private fun encodeStringMap(map: Map<String, Int>): JSONObject =
        JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }

    /** JSON NaN/sonsuz kabul etmez; böyle bir değer yazılmaz, okurken varsayılan alınır. */
    private fun JSONObject.putFinite(key: String, value: Double): JSONObject =
        if (value.isFinite()) put(key, value) else this

    // ---------------------------------------------------------------- okuma

    fun decode(o: JSONObject): ChatStats {
        val participants = o.optJSONArray("participants").objects().map(::decodeParticipant)
        return ChatStats(
            participants = participants,
            totals = decodeTotals(o.optJSONObject("totals") ?: JSONObject()),
            time = decodeTime(o.optJSONObject("time") ?: JSONObject()),
            topEmojis = decodeCounted(o.optJSONArray("topEmojis")),
            topDomains = decodeCounted(o.optJSONArray("topDomains")),
            awards = o.optJSONArray("awards").objects().mapNotNull { a ->
                val type = enumOrNull<AwardType>(a.optString("type")) ?: return@mapNotNull null
                GroupAward(type, a.optString("winner"), a.optDouble("value", 0.0))
            },
            warnings = o.optJSONArray("warnings").objects().mapNotNull(::decodeWarning),
            minutePrecision = o.optBoolean("minutePrecision", true),
        )
    }

    private fun decodeParticipant(p: JSONObject) = ParticipantStats(
        name = p.optString("name"),
        messages = p.optInt("messages"),
        words = p.optInt("words"),
        characters = p.optInt("characters"),
        textMessages = p.optInt("textMessages"),
        media = decodeEnumMap<MediaType>(p.optJSONObject("media")),
        deleted = p.optInt("deleted"),
        edited = p.optInt("edited"),
        links = p.optInt("links"),
        questions = p.optInt("questions"),
        exclamations = p.optInt("exclamations"),
        shouts = p.optInt("shouts"),
        emojiMessages = p.optInt("emojiMessages"),
        laughter = p.optInt("laughter"),
        nightMessages = p.optInt("nightMessages"),
        dayStarts = p.optInt("dayStarts"),
        phrases = decodeEnumMap<PhraseCategory>(p.optJSONObject("phrases")),
        addressTerms = decodeStringMap(p.optJSONObject("addressTerms")),
        topWords = decodeCounted(p.optJSONArray("topWords")),
        topEmojis = decodeCounted(p.optJSONArray("topEmojis")),
        conversationsStarted = p.optInt("started"),
        conversationsEnded = p.optInt("ended"),
        doubleTexts = p.optInt("doubleTexts"),
        medianReply = if (p.has("medianReplySec")) Duration.ofSeconds(p.optLong("medianReplySec")) else null,
        ghostRatio = if (p.has("ghostRatio")) p.optDouble("ghostRatio").takeIf { it.isFinite() } else null,
        profanity = p.optInt("profanity"),
        topSwears = decodeCounted(p.optJSONArray("topSwears")),
        afterHoursMessages = p.optInt("afterHours"),
    )

    private fun decodeTotals(t: JSONObject) = Totals(
        messages = t.optInt("messages"),
        words = t.optInt("words"),
        characters = t.optInt("characters"),
        media = t.optInt("media"),
        deleted = t.optInt("deleted"),
        edited = t.optInt("edited"),
        links = t.optInt("links"),
        laughter = t.optInt("laughter"),
        profanity = t.optInt("profanity"),
    )

    private fun decodeTime(t: JSONObject): TimeStats {
        val heatmap = List(7) { d ->
            val row = t.optJSONArray("heatmap")?.optJSONArray(d)
            List(24) { h -> row?.optInt(h) ?: 0 }
        }
        return TimeStats(
            first = t.optString("first").toDateTimeOrNull(),
            last = t.optString("last").toDateTimeOrNull(),
            spanDays = t.optLong("spanDays"),
            activeDays = t.optInt("activeDays"),
            busiestDay = t.optJSONObject("busiestDay")?.let { b ->
                b.optString("date").toDateOrNull()?.let { DayCount(it, b.optInt("count")) }
            },
            heatmap = heatmap,
            monthly = t.optJSONArray("monthly").objects().mapNotNull { m ->
                val ym = runCatching { YearMonth.parse(m.optString("month")) }.getOrNull() ?: return@mapNotNull null
                MonthCount(ym, m.optInt("total"), decodeStringMap(m.optJSONObject("by")))
            },
            longestStreak = t.optJSONObject("streak")?.let { s ->
                val start = s.optString("start").toDateOrNull()
                val end = s.optString("end").toDateOrNull()
                if (start != null && end != null) Streak(start, end, s.optInt("days")) else null
            },
            longestSilence = t.optJSONObject("silence")?.let { s ->
                val from = s.optString("from").toDateTimeOrNull()
                val to = s.optString("to").toDateTimeOrNull()
                if (from != null && to != null) Silence(from, to) else null
            },
            nightRatio = t.optDouble("nightRatio", 0.0).finiteOr(0.0),
            earlyRatio = t.optDouble("earlyRatio", 0.0).finiteOr(0.0),
        )
    }

    private fun decodeWarning(w: JSONObject): AnalysisWarning? = when (w.optString("type")) {
        "NEAR_LIMIT" -> AnalysisWarning.NearExportLimit(w.optInt("count"))
        "UNPARSED" -> AnalysisWarning.UnparsedLines(w.optInt("count"), w.optDouble("ratio", 0.0).finiteOr(0.0))
        "NO_MESSAGES" -> AnalysisWarning.NoMessages
        else -> null
    }

    private fun decodeCounted(a: JSONArray?): List<Counted> =
        a.objects().map { Counted(it.optString("k"), it.optInt("n")) }

    private inline fun <reified E : Enum<E>> decodeEnumMap(o: JSONObject?): Map<E, Int> {
        if (o == null) return emptyMap()
        val result = LinkedHashMap<E, Int>()
        for (key in o.keys()) {
            val e = enumOrNull<E>(key) ?: continue // yeni sürümden kalma bilinmeyen tür: atla
            result[e] = o.optInt(key)
        }
        return result
    }

    /** Sıra korunur (hitaplar çoktan aza yazılır). */
    private fun decodeStringMap(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        val result = LinkedHashMap<String, Int>()
        for (key in o.keys()) result[key] = o.optInt(key)
        return result.entries.sortedByDescending { it.value }.associate { it.key to it.value }
    }

    private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? =
        enumValues<E>().firstOrNull { it.name == name }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }

    private fun String.toDateTimeOrNull(): LocalDateTime? =
        if (isEmpty()) null else runCatching { LocalDateTime.parse(this) }.getOrNull()

    private fun String.toDateOrNull(): LocalDate? =
        if (isEmpty()) null else runCatching { LocalDate.parse(this) }.getOrNull()

    private fun Double.finiteOr(fallback: Double) = if (isFinite()) this else fallback
}
