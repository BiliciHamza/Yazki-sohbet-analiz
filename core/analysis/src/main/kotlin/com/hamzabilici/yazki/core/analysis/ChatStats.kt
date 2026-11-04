package com.hamzabilici.yazki.core.analysis

import com.hamzabilici.yazki.core.parser.MediaType
import com.hamzabilici.yazki.core.turkish.PhraseCategory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Analiz eşikleri. Dokümandaki "Analiz metrikleri kataloğu" ile birebir. */
data class AnalysisConfig(
    /**
     * Bu süreden uzun sessizlikten sonraki ilk mesaj yeni bir sohbet başlatır (ne yazdığı fark etmez).
     * 3 saat: gün içinde aralıklarla konuşan çiftlerde de "kim başlatıyor" anlamlı çıkar.
     */
    val conversationGap: Duration = Duration.ofHours(3),
    /** Cevap süresi hesabında bu süreden uzun aralıklar dışarıda bırakılır. */
    val replyCutoff: Duration = Duration.ofHours(6),
    /** Cevap gelmeden aynı kişinin bu süreden uzun aralıkla tekrar yazması çift mesajdır. */
    val doubleTextGap: Duration = Duration.ofMinutes(10),
    /** Bu süre içinde cevap gelmeyen mesaj "hayalet" sayılır. */
    val ghostWindow: Duration = Duration.ofHours(24),
    /** Gece kuşu: [nightStartHour, nightEndHour) arası. */
    val nightStartHour: Int = 0,
    val nightEndHour: Int = 5,
    /** Erkenci kuş: [nightEndHour, earlyEndHour) arası. */
    val earlyEndHour: Int = 8,
    /** Mesai saatleri: hafta içi [workStartHour, workEndHour). Dışı ve hafta sonu "mesai dışı". */
    val workStartHour: Int = 9,
    val workEndHour: Int = 18,
    val topWordCount: Int = 10,
    val topEmojiCount: Int = 10,
    /** Grup ödülünde "Roman yazarı" için gereken en az mesaj. */
    val minMessagesForAverageAward: Int = 20,
    /**
     * WhatsApp medyasız dışa aktarımı en yeni ~40 bin mesajla sınırlıyor.
     * Bu sayının üstündeki dosyalarda "eski mesajlar eksik olabilir" uyarısı gösterilir.
     */
    val exportLimitWarningThreshold: Int = 39_000,
)

data class ChatStats(
    val participants: List<ParticipantStats>,
    val totals: Totals,
    val time: TimeStats,
    val topEmojis: List<Counted>,
    val topDomains: List<Counted>,
    val awards: List<GroupAward>,
    val warnings: List<AnalysisWarning>,
    /**
     * Zaman damgaları dakika hassasiyetinde mi (Android dışa aktarımı)? O zaman
     * aynı dakikadaki cevaplar 0 sn ölçülür; arayüz bunu "1 dk içinde" diye göstermeli.
     */
    val minutePrecision: Boolean = true,
) {
    val isGroup: Boolean get() = participants.size > 2
}

data class Totals(
    /** Sistem mesajları hariç, silinen ve medya dahil. */
    val messages: Int,
    val words: Int,
    val characters: Int,
    val media: Int,
    val deleted: Int,
    val edited: Int,
    val links: Int,
    val laughter: Int,
    val profanity: Int = 0,
)

data class ParticipantStats(
    val name: String,
    val messages: Int,
    val words: Int,
    val characters: Int,
    val textMessages: Int,
    val media: Map<MediaType, Int>,
    val deleted: Int,
    val edited: Int,
    val links: Int,
    val questions: Int,
    val exclamations: Int,
    val shouts: Int,
    val emojiMessages: Int,
    val laughter: Int,
    /** Gece (varsayılan 00-05) atılan mesaj sayısı. */
    val nightMessages: Int,
    /** Kaç günün ilk mesajını bu kişi attı. */
    val dayStarts: Int,
    val phrases: Map<PhraseCategory, Int>,
    /** Kullandığı hitaplar, çoktan aza: "kanka" → 120, "abi" → 4. */
    val addressTerms: Map<String, Int>,
    /** En sık kelimeler. Küfürler bu listede hiçbir zaman yoktur; ayrı tutulur ([topSwears]). */
    val topWords: List<Counted>,
    val topEmojis: List<Counted>,
    val conversationsStarted: Int,
    val conversationsEnded: Int,
    val doubleTexts: Int,
    /** Konuşan kişi değiştiğinde bu kişinin cevap verme süresinin medyanı. */
    val medianReply: Duration?,
    /** Bu kişinin mesaj bloklarından [AnalysisConfig.ghostWindow] içinde cevapsız kalanların oranı (0..1). */
    val ghostRatio: Double?,
    /** Küfür içeren kelime sayısı. */
    val profanity: Int = 0,
    /** En sık kullandığı küfürler. Arayüz yalnızca sansür kapalıyken gösterir. */
    val topSwears: List<Counted> = emptyList(),
    /** Hafta içi mesai saatleri dışında veya hafta sonu atılan mesajlar. */
    val afterHoursMessages: Int = 0,
) {
    val mediaTotal: Int get() = media.values.sum()

    /** Metin mesajı başına ortalama kelime. */
    val averageWords: Double get() = if (textMessages == 0) 0.0 else words.toDouble() / textMessages

    /** Emoji içeren metin mesajı oranı (0..1). */
    val emojiRatio: Double get() = if (textMessages == 0) 0.0 else emojiMessages.toDouble() / textMessages
}

data class TimeStats(
    val first: LocalDateTime?,
    val last: LocalDateTime?,
    /** İlk ve son mesaj arasındaki gün sayısı (ikisi dahil). */
    val spanDays: Long,
    val activeDays: Int,
    val busiestDay: DayCount?,
    /** 7 × 24 matris; satır 0 = Pazartesi, sütun = saat. */
    val heatmap: List<List<Int>>,
    val monthly: List<MonthCount>,
    val longestStreak: Streak?,
    val longestSilence: Silence?,
    /** Gece (00-05) atılan mesaj oranı (0..1). */
    val nightRatio: Double,
    /** Sabah erken (05-08) atılan mesaj oranı (0..1). */
    val earlyRatio: Double,
)

data class Counted(val key: String, val count: Int)

data class DayCount(val date: LocalDate, val count: Int)

data class MonthCount(val month: YearMonth, val total: Int, val byParticipant: Map<String, Int>)

data class Streak(val start: LocalDate, val end: LocalDate, val days: Int)

data class Silence(val from: LocalDateTime, val to: LocalDateTime) {
    val duration: Duration get() = Duration.between(from, to)
}

enum class AwardType {
    /** Çenesi düşük: en çok mesaj. */
    MOST_MESSAGES,

    /** Roman yazarı: en yüksek ortalama mesaj uzunluğu. */
    LONGEST_MESSAGES,

    /** Hayalet: mesaj atanlar arasında en az katılım. */
    GHOST,

    /** Podcast'çi: en çok sesli mesaj. */
    MOST_AUDIO,

    /** Sticker kralı: en çok çıkartma. */
    MOST_STICKERS,

    /** Işık hızı: en kısa medyan cevap süresi. */
    FASTEST_REPLY,
}

/**
 * @param value Ödülün dayanağı olan sayı: mesaj sayısı, ortalama kelime veya
 *   [AwardType.FASTEST_REPLY] için saniye. Biçimlendirme arayüzün işi.
 */
data class GroupAward(val type: AwardType, val winner: String, val value: Double)

sealed interface AnalysisWarning {
    /** Dosya WhatsApp'ın dışa aktarım sınırına yakın; eski mesajlar eksik olabilir. */
    data class NearExportLimit(val messageCount: Int) : AnalysisWarning

    /** Satırların bir kısmı tanınamadı. */
    data class UnparsedLines(val count: Int, val ratio: Double) : AnalysisWarning

    /** Dosyada hiç kullanıcı mesajı bulunamadı. */
    data object NoMessages : AnalysisWarning
}
