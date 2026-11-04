package com.hamzabilici.yazki.core.parser

import java.time.LocalDateTime

/** Bir mesajın türü. Sistem mesajları hiçbir istatistiğe katılmaz. */
enum class MessageKind {
    TEXT,
    MEDIA,
    DELETED,
    SYSTEM,
}

/** Medya mesajının alt türü. Dışa aktarımdaki "dahil edilmedi" / "ekli" satırlarından türetilir. */
enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    STICKER,
    GIF,
    DOCUMENT,
    CONTACT,
    LOCATION,
    POLL,

    /** Tek seferlik medya ve Android'in "null" yazdığı satırlar. */
    VIEW_ONCE,

    /** "<Medya dahil edilmedi>" gibi türü belirtilmemiş medya. */
    UNKNOWN,
}

enum class ExportPlatform { ANDROID, IOS }

/** Tarihteki gün ve ay sırası. Dışa aktarım telefonun yerel ayarına göre değişir. */
enum class DateOrder { DAY_FIRST, MONTH_FIRST, YEAR_FIRST }

data class ChatMessage(
    val timestamp: LocalDateTime,
    /** Sistem mesajlarında null. */
    val sender: String?,
    /** Mesaj metni. Medya ve silinen mesajlarda dışa aktarımdaki ham ibare. */
    val text: String,
    val kind: MessageKind,
    val mediaType: MediaType? = null,
    val edited: Boolean = false,
)

data class ParsedChat(
    val messages: List<ChatMessage>,
    /** Mesaj atan kişiler, ilk görünme sırasıyla. Sessiz üyeler dışa aktarımda hiç görünmez. */
    val participants: List<String>,
    val platform: ExportPlatform?,
    val dateOrder: DateOrder,
    /** Okunan toplam satır sayısı. */
    val totalLines: Int,
    /** Hiçbir mesaja bağlanamayan satırlar (ilk mesajdan önceki çöp satırlar, geçersiz tarihler). */
    val unparsedLines: Int,
    /**
     * Zaman damgalarında saniye var mı? Android dışa aktarımı yalnızca saat:dakika verir;
     * bu durumda aynı dakikadaki cevaplar 0 sn görünür ve süreler dakika hassasiyetinde yorumlanmalıdır.
     */
    val hasSeconds: Boolean = false,
) {
    /** Sistem mesajları hariç mesajlar. */
    val userMessages: List<ChatMessage> get() = messages.filter { it.kind != MessageKind.SYSTEM }

    /** Ayrıştırılamayan satır oranı. Yüksekse format tanınmamış demektir. */
    val unparsedRatio: Double
        get() = if (totalLines == 0) 0.0 else unparsedLines.toDouble() / totalLines
}
