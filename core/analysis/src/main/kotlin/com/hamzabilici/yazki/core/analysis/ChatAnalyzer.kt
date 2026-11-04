package com.hamzabilici.yazki.core.analysis

import com.hamzabilici.yazki.core.parser.ChatMessage
import com.hamzabilici.yazki.core.parser.MediaType
import com.hamzabilici.yazki.core.parser.MessageKind
import com.hamzabilici.yazki.core.parser.ParsedChat
import com.hamzabilici.yazki.core.turkish.AddressTerms
import com.hamzabilici.yazki.core.turkish.Laughter
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import com.hamzabilici.yazki.core.turkish.PhraseCounter
import com.hamzabilici.yazki.core.turkish.Profanity
import com.hamzabilici.yazki.core.turkish.StopWords
import com.hamzabilici.yazki.core.turkish.TokenizedText
import com.hamzabilici.yazki.core.turkish.TurkishText
import java.text.BreakIterator
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.TreeMap

/** Ayrıştırılmış sohbetten [ChatStats] üretir. Tamamen cihaz içinde, ağ erişimi olmadan çalışır. */
class ChatAnalyzer(private val config: AnalysisConfig = AnalysisConfig()) {

    fun analyze(chat: ParsedChat): ChatStats {
        // Dışa aktarımda mesajlar nadiren kronolojik sırada olmayabiliyor; kararlı sıralama.
        val messages = chat.messages
            .filter { it.kind != MessageKind.SYSTEM && it.sender != null }
            .sortedBy { it.timestamp }

        val accs = LinkedHashMap<String, Accumulator>()
        chat.participants.forEach { accs[it] = Accumulator(it) }
        fun acc(m: ChatMessage): Accumulator {
            val sender = m.sender!!
            return accs.getOrPut(sender) { Accumulator(sender) }
        }

        val time = TimeAccumulator(config)
        val allEmojis = HashMap<String, Int>()
        val domains = HashMap<String, Int>()

        var lastDay: LocalDate? = null
        for (m in messages) {
            val a = acc(m)
            a.messages++
            if (m.edited) a.edited++
            val hour = m.timestamp.hour
            if (hour in config.nightStartHour until config.nightEndHour) a.night++
            val weekend = m.timestamp.dayOfWeek.value >= 6
            if (weekend || hour < config.workStartHour || hour >= config.workEndHour) a.afterHours++
            // Günün ilk mesajı: o günü kim başlattı?
            val day = m.timestamp.toLocalDate()
            if (day != lastDay) {
                a.dayStarts++
                lastDay = day
            }
            when (m.kind) {
                MessageKind.TEXT -> a.addText(m.text, allEmojis, domains)
                MessageKind.MEDIA -> a.media.merge(m.mediaType ?: MediaType.UNKNOWN, 1, Int::plus)
                MessageKind.DELETED -> a.deleted++
                MessageKind.SYSTEM -> Unit
            }
            time.add(m)
        }

        addInteraction(messages, accs)

        val participants = accs.values.filter { it.messages > 0 }.map { it.toStats(config.topWordCount) }

        val totals = Totals(
            messages = participants.sumOf { it.messages },
            words = participants.sumOf { it.words },
            characters = participants.sumOf { it.characters },
            media = participants.sumOf { it.mediaTotal },
            deleted = participants.sumOf { it.deleted },
            edited = participants.sumOf { it.edited },
            links = participants.sumOf { it.links },
            laughter = participants.sumOf { it.laughter },
            profanity = participants.sumOf { it.profanity },
        )

        return ChatStats(
            participants = participants,
            totals = totals,
            time = time.build(messages),
            topEmojis = allEmojis.top(config.topEmojiCount),
            topDomains = domains.top(10),
            awards = if (participants.size > 2) awards(participants) else emptyList(),
            warnings = warnings(chat, totals),
            minutePrecision = !chat.hasSeconds,
        )
    }

    // ---------------------------------------------------------------- etkileşim

    private fun addInteraction(messages: List<ChatMessage>, accs: Map<String, Accumulator>) {
        if (messages.isEmpty()) return

        for (i in messages.indices) {
            val m = messages[i]
            val a = accs.getValue(m.sender!!)
            if (i == 0) {
                a.started++
                continue
            }
            val prev = messages[i - 1]
            val gap = Duration.between(prev.timestamp, m.timestamp)
            val sameSender = prev.sender == m.sender

            if (gap > config.conversationGap) {
                a.started++
                accs.getValue(prev.sender!!).ended++
            } else if (!sameSender && gap <= config.replyCutoff) {
                a.replySeconds += gap.seconds
            }

            if (sameSender && gap > config.doubleTextGap && gap <= config.conversationGap) {
                a.doubleTexts++
            }
        }
        accs.getValue(messages.last().sender!!).ended++

        // Hayalet endeksi: kişinin her mesaj bloğunun sonunda, başkası pencere içinde cevap verdi mi?
        // Son blok değerlendirilmez; sohbet orada bittiği için cevabın gelip gelmeyeceği bilinmez.
        for (i in 0 until messages.size - 1) {
            val m = messages[i]
            val next = messages[i + 1]
            if (next.sender == m.sender) continue
            val a = accs.getValue(m.sender!!)
            a.blocks++
            if (Duration.between(m.timestamp, next.timestamp) > config.ghostWindow) a.unanswered++
        }
    }

    // ---------------------------------------------------------------- ödüller ve uyarılar

    private fun awards(people: List<ParticipantStats>): List<GroupAward> = buildList {
        people.maxByOrNull { it.messages }?.let {
            add(GroupAward(AwardType.MOST_MESSAGES, it.name, it.messages.toDouble()))
        }
        people.filter { it.textMessages >= config.minMessagesForAverageAward }
            .maxByOrNull { it.averageWords }
            ?.let { add(GroupAward(AwardType.LONGEST_MESSAGES, it.name, it.averageWords)) }
        people.minByOrNull { it.messages }?.let {
            add(GroupAward(AwardType.GHOST, it.name, it.messages.toDouble()))
        }
        people.maxByOrNull { it.media[MediaType.AUDIO] ?: 0 }
            ?.takeIf { (it.media[MediaType.AUDIO] ?: 0) > 0 }
            ?.let { add(GroupAward(AwardType.MOST_AUDIO, it.name, it.media.getValue(MediaType.AUDIO).toDouble())) }
        people.maxByOrNull { it.media[MediaType.STICKER] ?: 0 }
            ?.takeIf { (it.media[MediaType.STICKER] ?: 0) > 0 }
            ?.let { add(GroupAward(AwardType.MOST_STICKERS, it.name, it.media.getValue(MediaType.STICKER).toDouble())) }
        people.filter { it.medianReply != null }
            .minByOrNull { it.medianReply!! }
            ?.let { add(GroupAward(AwardType.FASTEST_REPLY, it.name, it.medianReply!!.seconds.toDouble())) }
    }

    private fun warnings(chat: ParsedChat, totals: Totals): List<AnalysisWarning> = buildList {
        if (totals.messages == 0) add(AnalysisWarning.NoMessages)
        if (totals.messages >= config.exportLimitWarningThreshold) {
            add(AnalysisWarning.NearExportLimit(totals.messages))
        }
        if (chat.unparsedLines > 0 && chat.unparsedRatio >= 0.01) {
            add(AnalysisWarning.UnparsedLines(chat.unparsedLines, chat.unparsedRatio))
        }
    }

    // ---------------------------------------------------------------- kişi başı birikim

    private class Accumulator(val name: String) {
        var messages = 0
        var words = 0
        var characters = 0
        var textMessages = 0
        val media = HashMap<MediaType, Int>()
        var deleted = 0
        var edited = 0
        var links = 0
        var questions = 0
        var exclamations = 0
        var shouts = 0
        var emojiMessages = 0
        var laughter = 0
        var night = 0
        var afterHours = 0
        var dayStarts = 0
        var profanity = 0
        val swears = HashMap<String, Int>()
        val phrases = HashMap<PhraseCategory, Int>()
        val addressTerms = HashMap<String, Int>()
        val wordCounts = HashMap<String, Int>()
        val emojis = HashMap<String, Int>()
        var started = 0
        var ended = 0
        var doubleTexts = 0
        val replySeconds = LongBuffer()
        var blocks = 0
        var unanswered = 0

        fun addText(text: String, allEmojis: MutableMap<String, Int>, domains: MutableMap<String, Int>) {
            textMessages++

            // Mesaj bir kez bölünür ve katlanır; bütün sayaçlar aynı listeleri paylaşır.
            val tokens = TokenizedText(text)
            words += tokens.words.size
            characters += graphemeCount(text)

            if (TurkishText.mayContainUrl(text)) {
                URL.findAll(text).forEach { match ->
                    links++
                    domainOf(match.value)?.let { domains.merge(it, 1, Int::plus) }
                }
            }

            if ('?' in text) questions++
            if ('!' in text) exclamations++
            if (isShout(text)) shouts++

            val found = Emoji.extract(text)
            if (found.isNotEmpty()) emojiMessages++
            for (e in found) {
                emojis.merge(e, 1, Int::plus)
                allEmojis.merge(e, 1, Int::plus)
            }

            PhraseCounter.countInto(tokens, phrases)
            AddressTerms.countInto(tokens, addressTerms)

            // Tek geçiş: her kelime ya küfürdür, ya kahkahadır, ya da kelime sıklığına girer.
            // Küfürler kelime listesine hiç girmez; sansür açıkken hiçbir yerde görünmesinler.
            laughter += Laughter.countEmoji(text)
            val squeezed = tokens.squeezed
            for (i in squeezed.indices) {
                val swear = Profanity.match(tokens.words[i], tokens.lower[i])
                if (swear != null) {
                    profanity++
                    swears.merge(swear, 1, Int::plus)
                    continue
                }
                val key = squeezed[i]
                if (Laughter.isLaughterWord(key)) {
                    laughter++
                    continue
                }
                if (StopWords.isStopWord(key)) continue
                // Gösterim için Türkçe küçük harfli asıl yazım saklanır ("Kahve" → "kahve").
                wordCounts.merge(tokens.lower[i], 1, Int::plus)
            }
        }

        fun toStats(topWordCount: Int) = ParticipantStats(
            name = name,
            messages = messages,
            words = words,
            characters = characters,
            textMessages = textMessages,
            media = media.toMap(),
            deleted = deleted,
            edited = edited,
            links = links,
            questions = questions,
            exclamations = exclamations,
            shouts = shouts,
            emojiMessages = emojiMessages,
            laughter = laughter,
            nightMessages = night,
            dayStarts = dayStarts,
            phrases = phrases.toMap(),
            addressTerms = addressTerms.top(Int.MAX_VALUE).associate { it.key to it.count },
            topWords = wordCounts.top(topWordCount),
            topEmojis = emojis.top(3),
            conversationsStarted = started,
            conversationsEnded = ended,
            doubleTexts = doubleTexts,
            medianReply = replySeconds.median()?.let { Duration.ofSeconds(it) },
            ghostRatio = if (blocks == 0) null else unanswered.toDouble() / blocks,
            profanity = profanity,
            topSwears = swears.top(5),
            afterHoursMessages = afterHours,
        )
    }

    // ---------------------------------------------------------------- zaman

    private class TimeAccumulator(private val config: AnalysisConfig) {
        val heatmap = Array(7) { IntArray(24) }
        val days = TreeMap<LocalDate, Int>()
        val months = TreeMap<YearMonth, MutableMap<String, Int>>()
        var night = 0
        var early = 0
        var total = 0

        fun add(m: ChatMessage) {
            val ts = m.timestamp
            total++
            heatmap[ts.dayOfWeek.value - 1][ts.hour]++
            days.merge(ts.toLocalDate(), 1, Int::plus)
            months.getOrPut(YearMonth.from(ts)) { HashMap() }.merge(m.sender!!, 1, Int::plus)
            when (ts.hour) {
                in config.nightStartHour until config.nightEndHour -> night++
                in config.nightEndHour until config.earlyEndHour -> early++
            }
        }

        fun build(sorted: List<ChatMessage>): TimeStats {
            val first = sorted.firstOrNull()?.timestamp
            val last = sorted.lastOrNull()?.timestamp
            return TimeStats(
                first = first,
                last = last,
                spanDays = if (first == null || last == null) 0
                else Duration.between(first.toLocalDate().atStartOfDay(), last.toLocalDate().atStartOfDay()).toDays() + 1,
                activeDays = days.size,
                busiestDay = days.maxByOrNull { it.value }?.let { DayCount(it.key, it.value) },
                heatmap = heatmap.map { it.toList() },
                monthly = months.map { (ym, by) -> MonthCount(ym, by.values.sum(), by.toMap()) },
                longestStreak = longestStreak(),
                longestSilence = longestSilence(sorted),
                nightRatio = if (total == 0) 0.0 else night.toDouble() / total,
                earlyRatio = if (total == 0) 0.0 else early.toDouble() / total,
            )
        }

        private fun longestStreak(): Streak? {
            var best: Streak? = null
            var start: LocalDate? = null
            var prev: LocalDate? = null
            for (day in days.keys) {
                if (prev == null || day != prev.plusDays(1)) start = day
                val s = start!!
                val length = ChronoUnit.DAYS.between(s, day).toInt() + 1
                if (best == null || length > best.days) best = Streak(s, day, length)
                prev = day
            }
            return best
        }

        private fun longestSilence(sorted: List<ChatMessage>): Silence? {
            var best: Silence? = null
            for (i in 1 until sorted.size) {
                val from = sorted[i - 1].timestamp
                val to = sorted[i].timestamp
                if (best == null || Duration.between(from, to) > best.duration) best = Silence(from, to)
            }
            return best
        }
    }

    companion object {
        private val URL = Regex("""(?:https?://|www\.)[^\s<>"]+""", RegexOption.IGNORE_CASE)

        internal fun domainOf(url: String): String? {
            val withoutScheme = url.substringAfter("://", url)
            val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':')
                .lowercase()
                .removePrefix("www.")
            return host.ifBlank { null }
        }

        /** Tamamı büyük harf ve en az 4 harfli mesaj: "NEREDESİN". */
        internal fun isShout(text: String): Boolean {
            var letters = 0
            for (ch in text) {
                if (!ch.isLetter()) continue
                if (!ch.isUpperCase()) return false // ilk küçük harfte çık, kopya oluşturma
                letters++
            }
            return letters >= 4
        }

        internal fun graphemeCount(text: String): Int {
            // Hızlı yol: vekil çift ve birleşik işaret yoksa her char bir grafemdir.
            if (text.none { it.isSurrogate() || Character.getType(it) == Character.NON_SPACING_MARK.toInt() || it.code in 0x200C..0x200D || it.code == 0xFE0F }) {
                return text.length
            }
            val it = BreakIterator.getCharacterInstance()
            it.setText(text)
            var count = 0
            while (it.next() != BreakIterator.DONE) count++
            return count
        }

    }
}

/** Kutulamasız (boxing'siz) büyüyen long dizisi; büyük sohbetlerde cevap sürelerini tutar. */
internal class LongBuffer {
    private var data = LongArray(16)
    var size = 0
        private set

    fun add(value: Long) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = value
    }

    operator fun plusAssign(value: Long) = add(value)

    fun median(): Long? {
        if (size == 0) return null
        val sorted = data.copyOf(size).also { it.sort() }
        val mid = size / 2
        return if (size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}

private fun Map<String, Int>.top(n: Int): List<Counted> =
    entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(n)
        .map { Counted(it.key, it.value) }
