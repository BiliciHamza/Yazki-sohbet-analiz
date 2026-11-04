package com.hamzabilici.yazki.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.core.analysis.ParticipantStats
import com.hamzabilici.yazki.core.parser.MediaType
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import com.hamzabilici.yazki.data.ChatOptions
import com.hamzabilici.yazki.data.ChatType

/** Karne kartı ve hikâye sayfası: bir emoji, kısa başlık, büyük değer, açıklama. */
data class Highlight(
    val emoji: String,
    val title: String,
    val value: String,
    val detail: String,
)

/**
 * Sohbetin öne çıkan anlarını, sohbet türüne uygun kartlar ve sırayla üretir
 * ([ChatType.highlightOrder]). Karne ekranı ve hikâye modu aynı listeyi kullanır.
 * Verisi olmayan kartlar (ör. hiç kahkaha yoksa) listeye girmez.
 */
@Composable
fun highlightsFor(stats: ChatStats, options: ChatOptions): List<Highlight> {
    val list = ArrayList<Highlight>()
    for (kind in options.type.highlightOrder()) {
        highlight(kind, stats, options)?.let { list += it }
    }
    return list
}

@Composable
private fun highlight(kind: HighlightKind, stats: ChatStats, options: ChatOptions): Highlight? {
    val people = stats.participants
    val multi = people.size >= 2
    val type = options.type

    fun best(selector: (ParticipantStats) -> Int): ParticipantStats? =
        people.maxByOrNull(selector)?.takeIf { selector(it) > 0 }

    return when (kind) {
        HighlightKind.TOTAL -> {
            val days = stats.time.spanDays.coerceAtLeast(1)
            Highlight(
                "💬",
                stringResource(R.string.hl_total_title),
                Format.number(stats.totals.messages),
                stringResource(
                    R.string.hl_total_detail,
                    Format.number(days),
                    Format.decimal(stats.totals.messages.toDouble() / days),
                ),
            )
        }

        HighlightKind.TOP_TALKER -> if (!multi) null else people.maxByOrNull { it.messages }?.let { p ->
            Highlight(
                "🗣️",
                stringResource(R.string.hl_top_talker_title),
                p.name,
                stringResource(R.string.hl_top_talker_detail, Format.percent(p.messages.toDouble() / stats.totals.messages)),
            )
        }

        HighlightKind.STARTER -> if (!multi) null else best { it.conversationsStarted }?.let { p ->
            Highlight(
                "👋",
                stringResource(R.string.hl_starter_title),
                p.name,
                stringResource(R.string.hl_starter_detail, Format.number(p.conversationsStarted)),
            )
        }

        HighlightKind.DAY_STARTER -> if (!multi) null else best { it.dayStarts }?.let { p ->
            Highlight(
                "🌅",
                stringResource(R.string.hl_day_starter_title),
                p.name,
                stringResource(R.string.hl_day_starter_detail, Format.number(p.dayStarts)),
            )
        }

        HighlightKind.FASTEST -> if (!multi) {
            null
        } else {
            people.filter { it.medianReply != null }.minByOrNull { it.medianReply!! }?.let { p ->
                Highlight(
                    "⚡",
                    stringResource(R.string.hl_fastest_title),
                    p.name,
                    stringResource(R.string.hl_fastest_detail, formatDuration(p.medianReply!!, stats.minutePrecision)),
                )
            }
        }

        HighlightKind.NIGHT_OWL -> best { it.nightMessages }?.let { p ->
            Highlight(
                "🦉",
                stringResource(R.string.hl_night_title),
                p.name,
                stringResource(R.string.hl_night_detail, Format.number(p.nightMessages)),
            )
        }

        HighlightKind.LAUGHTER -> best { it.laughter }?.let { p ->
            Highlight(
                "😂",
                stringResource(R.string.hl_laughter_title),
                p.name,
                stringResource(R.string.hl_laughter_detail, Format.number(p.laughter)),
            )
        }

        HighlightKind.ADDRESS -> topAddressTerm(stats)?.let { (term, total, leader) ->
            Highlight(
                "🫂",
                stringResource(R.string.hl_address_title),
                "“$term”",
                if (multi) {
                    stringResource(R.string.hl_address_detail, Format.number(total), leader)
                } else {
                    stringResource(R.string.hl_address_detail_single, Format.number(total))
                },
            )
        }

        HighlightKind.MORNING ->
            phrase(stats, PhraseCategory.GOOD_MORNING, "☀️", R.string.hl_morning_title, R.string.hl_morning_detail)
        HighlightKind.GOOD_NIGHT ->
            phrase(stats, PhraseCategory.GOOD_NIGHT, "🌙", R.string.hl_night_greet_title, R.string.hl_night_greet_detail)
        HighlightKind.HOW_ARE_YOU ->
            phrase(stats, PhraseCategory.HOW_ARE_YOU, "🤗", R.string.hl_how_title, R.string.hl_how_detail)
        HighlightKind.THANKS ->
            phrase(stats, PhraseCategory.THANKS, "🌸", R.string.hl_thanks_title, R.string.hl_thanks_detail)
        HighlightKind.FRIDAY ->
            phrase(stats, PhraseCategory.FRIDAY_GREETING, "🤲", R.string.hl_friday_title, R.string.hl_friday_detail)
        HighlightKind.OKAY ->
            phrase(stats, PhraseCategory.OKAY, "👌", R.string.hl_okay_title, R.string.hl_okay_detail)
        HighlightKind.APOLOGY ->
            phrase(stats, PhraseCategory.APOLOGY, "🙏", R.string.hl_apology_title, R.string.hl_apology_detail)
        HighlightKind.LOVE -> phrase(
            stats,
            PhraseCategory.LOVE,
            "❤️",
            R.string.hl_love_title,
            // Sevgilide "en romantik", ailede "en sevecen"; diğer türlerde bu kart hiç yok.
            if (type == ChatType.FAMILY) R.string.hl_love_detail_family else R.string.hl_love_detail,
        )

        HighlightKind.DOUBLE_TEXT -> if (!multi) null else best { it.doubleTexts }?.let { p ->
            Highlight(
                "📨",
                stringResource(R.string.hl_double_title),
                p.name,
                stringResource(R.string.hl_double_detail, Format.number(p.doubleTexts)),
            )
        }

        HighlightKind.PROFANITY -> if (!options.countProfanity) null else best { it.profanity }?.let { p ->
            val favorite = p.topSwears.firstOrNull()
            Highlight(
                "🤬",
                stringResource(R.string.hl_profanity_title),
                p.name,
                if (!options.censor && favorite != null) {
                    // Sansür kapalı: en sevdiği küfür de görünür.
                    stringResource(R.string.hl_profanity_detail_uncensored, Format.number(p.profanity), favorite.key)
                } else {
                    stringResource(R.string.hl_profanity_detail, Format.number(p.profanity))
                },
            )
        }

        HighlightKind.AFTER_HOURS -> best { it.afterHoursMessages }?.let { p ->
            val total = people.sumOf { it.afterHoursMessages }
            Highlight(
                "🌆",
                stringResource(R.string.hl_after_hours_title),
                p.name,
                stringResource(
                    R.string.hl_after_hours_detail,
                    Format.number(p.afterHoursMessages),
                    Format.percent(total.toDouble() / stats.totals.messages.coerceAtLeast(1)),
                ),
            )
        }

        HighlightKind.QUESTIONS -> best { it.questions }?.let { p ->
            Highlight("❓", stringResource(R.string.hl_questions_title), p.name,
                stringResource(R.string.hl_questions_detail, Format.number(p.questions)))
        }

        HighlightKind.DOCUMENTS -> media(stats, MediaType.DOCUMENT, "📎", R.string.hl_documents_title, R.string.hl_documents_detail)
        HighlightKind.PHOTOS -> media(
            stats, MediaType.IMAGE, "📸",
            if (type == ChatType.FAMILY) R.string.hl_photos_title_family else R.string.hl_photos_title,
            R.string.hl_photos_detail,
        )
        HighlightKind.VOICE -> media(stats, MediaType.AUDIO, "🎙️", R.string.hl_voice_title, R.string.hl_voice_detail)

        HighlightKind.LINKS -> best { it.links }?.let { p ->
            Highlight(
                "🔗",
                stringResource(if (type == ChatType.FAMILY) R.string.hl_links_title_family else R.string.hl_links_title),
                p.name,
                stringResource(R.string.hl_links_detail, Format.number(p.links)),
            )
        }

        HighlightKind.STREAK -> stats.time.longestStreak?.takeIf { it.days >= 2 }?.let { s ->
            Highlight(
                "🔥",
                stringResource(R.string.hl_streak_title),
                stringResource(R.string.value_days, Format.number(s.days)),
                stringResource(R.string.result_date_range, Format.date(s.start), Format.date(s.end)),
            )
        }

        HighlightKind.BUSIEST_DAY -> stats.time.busiestDay?.let { d ->
            Highlight(
                "📅",
                stringResource(R.string.hl_busiest_title),
                Format.date(d.date),
                stringResource(R.string.hl_busiest_detail, Format.number(d.count)),
            )
        }

        HighlightKind.EMOJI -> stats.topEmojis.firstOrNull()?.let { e ->
            Highlight(e.key, stringResource(R.string.hl_emoji_title), e.key,
                stringResource(R.string.hl_emoji_detail, Format.number(e.count)))
        }

        HighlightKind.SILENCE -> stats.time.longestSilence?.let { s ->
            Highlight(
                "🤐",
                stringResource(R.string.hl_silence_title),
                formatDuration(s.duration, stats.minutePrecision),
                stringResource(R.string.hl_silence_detail, Format.date(s.from.toLocalDate())),
            )
        }
    }
}

/** Sohbetin en çok kullanılan hitabı: (hitap, toplam, en çok kullanan). */
private fun topAddressTerm(stats: ChatStats): Triple<String, Int, String>? {
    val totals = HashMap<String, Int>()
    for (p in stats.participants) {
        for ((term, n) in p.addressTerms) totals.merge(term, n, Int::plus)
    }
    val (term, total) = totals.maxByOrNull { it.value }?.toPair() ?: return null
    val leader = stats.participants.maxByOrNull { it.addressTerms[term] ?: 0 }?.name ?: return null
    return Triple(term, total, leader)
}

@Composable
private fun phrase(
    stats: ChatStats,
    category: PhraseCategory,
    emoji: String,
    @StringRes titleRes: Int,
    @StringRes detailRes: Int,
): Highlight? {
    val total = stats.participants.sumOf { it.phrases[category] ?: 0 }
    if (total == 0) return null
    val leader = stats.participants.maxByOrNull { it.phrases[category] ?: 0 } ?: return null
    return Highlight(emoji, stringResource(titleRes), Format.number(total), stringResource(detailRes, leader.name))
}

@Composable
private fun media(
    stats: ChatStats,
    type: MediaType,
    emoji: String,
    @StringRes titleRes: Int,
    @StringRes detailRes: Int,
): Highlight? {
    val leader = stats.participants.maxByOrNull { it.media[type] ?: 0 } ?: return null
    val count = leader.media[type] ?: 0
    if (count == 0) return null
    return Highlight(emoji, stringResource(titleRes), leader.name, stringResource(detailRes, Format.number(count)))
}
