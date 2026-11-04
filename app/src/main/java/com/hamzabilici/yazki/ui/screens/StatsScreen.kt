package com.hamzabilici.yazki.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.hamzabilici.yazki.core.analysis.Counted
import com.hamzabilici.yazki.data.ChatOptions
import com.hamzabilici.yazki.ui.showsAfterHours
import com.hamzabilici.yazki.ui.showsPhrase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.analysis.AwardType
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.core.analysis.GroupAward
import com.hamzabilici.yazki.core.analysis.ParticipantStats
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import com.hamzabilici.yazki.data.ImportResult
import com.hamzabilici.yazki.ui.Format
import com.hamzabilici.yazki.ui.components.EmptyState
import com.hamzabilici.yazki.ui.components.Heatmap
import com.hamzabilici.yazki.ui.components.LabeledValue
import com.hamzabilici.yazki.ui.components.MonthlyChart
import com.hamzabilici.yazki.ui.components.PersonAvatar
import com.hamzabilici.yazki.ui.components.SectionCard
import com.hamzabilici.yazki.ui.components.SectionHeader
import com.hamzabilici.yazki.ui.components.StatTile
import com.hamzabilici.yazki.ui.formatDuration
import com.hamzabilici.yazki.ui.theme.YazkiColors
import java.time.Duration

/** Ayrıntılı istatistikler: özet, kişiler, zaman, Türkçe sayaçlar, emoji, bağlantılar, ödüller. */
@Composable
fun StatsScreen(
    result: ImportResult?,
    hasSaved: Boolean,
    onOpenSaved: () -> Unit,
) {
    if (result == null) {
        EmptyState(
            title = stringResource(R.string.empty_stats_title),
            message = stringResource(R.string.empty_stats_body),
            secondaryLabel = if (hasSaved) stringResource(R.string.action_open_saved) else null,
            onSecondary = onOpenSaved,
        )
        return
    }
    val stats = result.stats
    val options = result.options
    // Renkler katılımcının ilk görünme sırasına göre sabit kalsın diye indeks önceden alınır.
    // Sıralama yalnızca sohbet değişince yapılır, her yeniden çizimde değil.
    val people = remember(stats) {
        stats.participants.withIndex().sortedByDescending { it.value.messages }.map { it.index to it.value }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryGrid(stats) }

        item { SectionHeader(stringResource(R.string.section_people)) }
        items(people, key = { it.second.name }) { (index, p) ->
            PersonCard(p, index, stats.totals.messages, stats.minutePrecision, options)
        }

        item { SectionHeader(stringResource(R.string.section_time)) }
        if (stats.time.monthly.size > 1) {
            item {
                SectionCard(title = stringResource(R.string.time_monthly)) { MonthlyChart(stats.time.monthly) }
            }
        }
        item {
            SectionCard(
                title = stringResource(R.string.time_heatmap),
                subtitle = stringResource(R.string.time_heatmap_hint),
            ) { Heatmap(stats.time.heatmap) }
        }
        item { TimeFactsCard(stats) }

        item { SectionHeader(stringResource(R.string.section_turkish)) }
        item { TurkishCountersCard(people, options) }
        if (people.any { it.second.addressTerms.isNotEmpty() }) item { AddressTermsCard(people) }

        if (stats.topEmojis.isNotEmpty()) item { EmojiCard(stats) }
        if (stats.topDomains.isNotEmpty()) item { DomainsCard(stats) }
        if (stats.awards.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_awards)) }
            item { AwardsCard(stats.awards, stats.minutePrecision) }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ---------------------------------------------------------------- özet

@Composable
private fun SummaryGrid(stats: ChatStats) {
    val t = stats.time
    val tiles = listOf(
        stringResource(R.string.stat_messages) to Format.number(stats.totals.messages),
        stringResource(R.string.stat_words) to Format.number(stats.totals.words),
        stringResource(R.string.stat_media) to Format.number(stats.totals.media),
        stringResource(R.string.stat_active_days) to Format.number(t.activeDays),
        stringResource(R.string.stat_longest_streak) to stringResource(R.string.value_days, Format.number(t.longestStreak?.days ?: 0)),
        stringResource(R.string.stat_links) to Format.number(stats.totals.links),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEachIndexed { row, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEachIndexed { col, (label, value) ->
                    StatTile(label, value, YazkiColors.person(row * 2 + col), Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- kişiler

@Composable
private fun PersonCard(
    p: ParticipantStats,
    colorIndex: Int,
    totalMessages: Int,
    minutePrecision: Boolean,
    options: ChatOptions,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(p.name, colorIndex, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(
                        R.string.person_summary,
                        Format.number(p.messages),
                        Format.percent(if (totalMessages == 0) 0.0 else p.messages.toDouble() / totalMessages),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (p.topEmojis.isNotEmpty()) {
                Text(p.topEmojis.joinToString("") { it.key }, fontSize = 20.sp)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        LabeledValue(stringResource(R.string.person_average_words), Format.decimal(p.averageWords))
        LabeledValue(stringResource(R.string.person_media), Format.number(p.mediaTotal))
        LabeledValue(stringResource(R.string.person_started), Format.number(p.conversationsStarted))
        LabeledValue(stringResource(R.string.person_day_starts), Format.number(p.dayStarts))
        LabeledValue(stringResource(R.string.person_ended), Format.number(p.conversationsEnded))
        LabeledValue(
            stringResource(R.string.person_median_reply),
            p.medianReply?.let { formatDuration(it, minutePrecision) } ?: stringResource(R.string.value_none),
        )
        if (!options.type.isGroup) {
            LabeledValue(stringResource(R.string.person_double_texts), Format.number(p.doubleTexts))
            LabeledValue(
                stringResource(R.string.person_ghost),
                p.ghostRatio?.let(Format::percent) ?: stringResource(R.string.value_none),
            )
        }
        if (options.type.showsAfterHours) {
            LabeledValue(stringResource(R.string.person_after_hours), Format.number(p.afterHoursMessages))
        }
        LabeledValue(stringResource(R.string.person_deleted), Format.number(p.deleted))
        if (options.countProfanity) {
            LabeledValue(stringResource(R.string.person_profanity), Format.number(p.profanity))
            if (!options.censor && p.topSwears.isNotEmpty()) {
                LabeledValue(
                    stringResource(R.string.person_top_swears),
                    p.topSwears.take(3).joinToString(", ") { it.key },
                )
            }
        }
        val words = topWordsFor(p, options)
        if (words.isNotEmpty()) {
            LabeledValue(stringResource(R.string.person_top_words), words.joinToString(", ") { it.key })
        }
    }
}

/**
 * Sık kelimeler. Analiz küfürleri ayrı tutar; sansür açıkken liste küfürsüzdür,
 * kapalıyken iki liste sayıya göre birleştirilir (eski davranış).
 */
private fun topWordsFor(p: ParticipantStats, options: ChatOptions, n: Int = 5): List<Counted> =
    if (options.censor) {
        p.topWords.take(n)
    } else {
        (p.topWords + p.topSwears).sortedByDescending { it.count }.take(n)
    }

// ---------------------------------------------------------------- zaman

@Composable
private fun TimeFactsCard(stats: ChatStats) {
    val t = stats.time
    SectionCard {
        LabeledValue(stringResource(R.string.time_night), Format.percent(t.nightRatio))
        LabeledValue(stringResource(R.string.time_early), Format.percent(t.earlyRatio))
        t.busiestDay?.let {
            LabeledValue(
                stringResource(R.string.stat_busiest_day),
                stringResource(R.string.value_busiest_day, Format.date(it.date), Format.number(it.count)),
            )
        }
        t.longestSilence?.let {
            LabeledValue(
                stringResource(R.string.time_longest_silence),
                formatDuration(it.duration) + " · " + Format.date(it.from.toLocalDate()),
            )
        }
    }
}

// ---------------------------------------------------------------- Türkçe sayaçlar

private typealias Counter = (ParticipantStats) -> Int

private fun phrase(c: PhraseCategory): Counter = { p -> p.phrases[c] ?: 0 }

/**
 * Türkçe sayaç tablosu.
 *
 * Soldaki etiket sütunu sabittir; kişi sütunları yatay kaydırılır. Böylece kişi sayısı artınca
 * etiketler ("Kahkaha") harf harf alt satıra kırılmaz. Kaydırılacak içerik varsa tablonun hemen
 * üstünde bir kaydırma çubuğu görünür. Kimsenin kullanmadığı ve bu sohbet türünde anlamsız olan
 * satırlar gizlenir.
 */
@Composable
private fun TurkishCountersCard(people: List<Pair<Int, ParticipantStats>>, options: ChatOptions) {
    val shown = people.take(MAX_TABLE_PEOPLE)
    val type = options.type
    val rows: List<Triple<String, String, Counter>> = buildList {
        add(Triple("😂", stringResource(R.string.counter_laughter)) { p: ParticipantStats -> p.laughter })
        if (options.countProfanity) {
            add(Triple("🤬", stringResource(R.string.counter_profanity)) { p: ParticipantStats -> p.profanity })
        }
        listOf(
            Triple("☀️", R.string.counter_morning, PhraseCategory.GOOD_MORNING),
            Triple("🌙", R.string.counter_good_night, PhraseCategory.GOOD_NIGHT),
            Triple("🤗", R.string.counter_how_are_you, PhraseCategory.HOW_ARE_YOU),
            Triple("🌸", R.string.counter_thanks, PhraseCategory.THANKS),
            Triple("🤲", R.string.counter_friday, PhraseCategory.FRIDAY_GREETING),
            Triple("👌", R.string.counter_okay, PhraseCategory.OKAY),
            Triple("❤️", R.string.counter_love, PhraseCategory.LOVE),
            Triple("🙏", R.string.counter_apology, PhraseCategory.APOLOGY),
            Triple("🤝", R.string.counter_right, PhraseCategory.YOU_ARE_RIGHT),
            Triple("🏃", R.string.counter_on_my_way, PhraseCategory.ON_MY_WAY),
        ).forEach { (emoji, label, category) ->
            if (type.showsPhrase(category)) add(Triple(emoji, stringResource(label), phrase(category)))
        }
        add(Triple("🌅", stringResource(R.string.counter_day_starts)) { p: ParticipantStats -> p.dayStarts })
        if (type.showsAfterHours) {
            add(Triple("🌆", stringResource(R.string.counter_after_hours)) { p: ParticipantStats -> p.afterHoursMessages })
        }
        add(Triple("❓", stringResource(R.string.counter_questions)) { p: ParticipantStats -> p.questions })
        add(Triple("📢", stringResource(R.string.counter_shouts)) { p: ParticipantStats -> p.shouts })
    }.filter { (_, _, value) -> shown.any { value(it.second) > 0 } }

    val scroll = rememberScrollState()
    SectionCard {
        // Kaydırma çubuğu: yalnızca sütunlar ekrana sığmadığında.
        if (scroll.maxValue > 0) {
            HorizontalScrollIndicator(scroll)
            Text(
                stringResource(R.string.counter_scroll_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            // Sabit etiket sütunu
            Column(Modifier.width(LABEL_COLUMN)) {
                Box(Modifier.height(HEADER_HEIGHT))
                rows.forEach { (emoji, label, _) ->
                    Row(Modifier.height(ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 17.sp, modifier = Modifier.width(28.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            // Kaydırılan kişi sütunları
            Row(
                Modifier
                    .weight(1f)
                    .horizontalScroll(scroll),
            ) {
                shown.forEach { (index, p) ->
                    Column(Modifier.width(VALUE_COLUMN), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            Modifier.height(HEADER_HEIGHT),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            PersonAvatar(p.name, index, 26.dp)
                            Text(
                                p.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                        rows.forEach { (_, _, value) ->
                            Box(Modifier.height(ROW_HEIGHT), contentAlignment = Alignment.Center) {
                                Text(
                                    Format.number(value(p)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** İnce yatay kaydırma göstergesi; parmak sütunları kaydırdıkça hareket eder. */
@Composable
private fun HorizontalScrollIndicator(scroll: ScrollState) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        val viewport = scroll.viewportSize.toFloat().coerceAtLeast(1f)
        val content = viewport + scroll.maxValue
        val thumbFraction = (viewport / content).coerceIn(0.1f, 1f)
        val progress = if (scroll.maxValue == 0) 0f else scroll.value.toFloat() / scroll.maxValue
        val thumbWidth = maxWidth * thumbFraction
        Box(
            Modifier
                .offset(x = (maxWidth - thumbWidth) * progress)
                .width(thumbWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

private val LABEL_COLUMN = 148.dp
private val VALUE_COLUMN = 68.dp
private val ROW_HEIGHT = 34.dp
private val HEADER_HEIGHT = 50.dp
private const val MAX_TABLE_PEOPLE = 20

/** Kimin kime nasıl seslendiği: kişi başı en sık 4 hitap. */
@Composable
private fun AddressTermsCard(people: List<Pair<Int, ParticipantStats>>) {
    SectionCard(
        title = stringResource(R.string.section_address),
        subtitle = stringResource(R.string.section_address_hint),
    ) {
        people.filter { it.second.addressTerms.isNotEmpty() }.take(6).forEach { (index, p) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(p.name, index, 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        p.addressTerms.entries.take(4).joinToString("  ·  ") { (term, n) -> "$term ${Format.number(n)}" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- emoji, bağlantı, ödül

@Composable
private fun EmojiCard(stats: ChatStats) {
    SectionCard(title = stringResource(R.string.section_emoji)) {
        stats.topEmojis.take(10).chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { e ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(e.key, fontSize = 28.sp)
                        Text(
                            Format.number(e.count),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DomainsCard(stats: ChatStats) {
    SectionCard(title = stringResource(R.string.section_links)) {
        stats.topDomains.take(5).forEach { LabeledValue(it.key, Format.number(it.count)) }
    }
}

@Composable
private fun AwardsCard(awards: List<GroupAward>, minutePrecision: Boolean) {
    SectionCard {
        awards.forEachIndexed { i, award ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            val (emoji, titleRes) = when (award.type) {
                AwardType.MOST_MESSAGES -> "🗣️" to R.string.award_most_messages
                AwardType.LONGEST_MESSAGES -> "📜" to R.string.award_longest_messages
                AwardType.GHOST -> "👻" to R.string.award_ghost
                AwardType.MOST_AUDIO -> "🎙️" to R.string.award_most_audio
                AwardType.MOST_STICKERS -> "🏷️" to R.string.award_most_stickers
                AwardType.FASTEST_REPLY -> "⚡" to R.string.award_fastest_reply
            }
            val value = when (award.type) {
                AwardType.MOST_MESSAGES, AwardType.GHOST ->
                    stringResource(R.string.award_value_messages, Format.number(award.value.toInt()))
                AwardType.LONGEST_MESSAGES -> stringResource(R.string.award_value_words, Format.decimal(award.value))
                AwardType.MOST_AUDIO, AwardType.MOST_STICKERS ->
                    stringResource(R.string.award_value_count, Format.number(award.value.toInt()))
                AwardType.FASTEST_REPLY -> formatDuration(Duration.ofSeconds(award.value.toLong()), minutePrecision)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                Text(emoji, fontSize = 26.sp, modifier = Modifier.width(44.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(award.winner, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
