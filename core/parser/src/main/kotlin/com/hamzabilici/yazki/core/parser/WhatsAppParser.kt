package com.hamzabilici.yazki.core.parser

import java.time.DateTimeException
import java.time.LocalDateTime

/**
 * WhatsApp "Sohbeti dışa aktar" metnini ayrıştırır.
 *
 * Desteklenen biçimler:
 * - Android: `02.11.2026 20:34 - Ali: mesaj`, `11/2/26, 8:34 PM - Ali: message`
 * - iOS:     `[02.11.2026 20:34:01] Ali: mesaj`, `[11/2/26, 8:34:01 PM] Ali: message`
 * - Yıl önde: `2026-11-02 20:34 - Ali: mesaj`
 *
 * Gün/ay sırası tek tek satırlardan anlaşılamaz (`03.04` hem 3 Nisan hem 4 Mart olabilir),
 * bu yüzden ayrıştırma iki geçişlidir: önce bütün başlıklar toplanır, sıra tüm dosyaya
 * bakılarak belirlenir, sonra mesajlar oluşturulur.
 */
object WhatsAppParser {

    private const val MAX_SENDER_LENGTH = 80

    private const val DATE = """(\d{1,4})([./-])(\d{1,2})\2(\d{1,4})"""
    private const val TIME = """(\d{1,2})[:.](\d{2})(?:[:.](\d{2}))?"""
    private const val AMPM = """(?:\s?([AaPp]\.?\s?[Mm]\.?|ÖÖ|ÖS|öö|ös))?"""

    private val ANDROID_HEADER = Regex("""^$DATE,?\s$TIME$AMPM\s[-–]\s(.*)$""")
    private val IOS_HEADER = Regex("""^\[$DATE,?\s$TIME$AMPM]\s(.*)$""")

    /** Yön ve biçim işaretleri: LRM, RLM, gömme/yalıtma işaretleri, BOM. */
    private val INVISIBLE_MARKS = Regex("[‎‏‪-‮⁦-⁩﻿]")

    fun parse(text: String): ParsedChat = parse(text.lineSequence())

    fun parse(lines: Sequence<String>): ParsedChat {
        val headers = ArrayList<RawHeader>()
        var totalLines = 0
        var orphanLines = 0
        var current: RawHeader? = null

        for (rawLine in lines) {
            totalLines++
            val line = normalizeSpaces(rawLine)
            val header = matchHeader(line)
            if (header != null) {
                current = header
                headers += header
            } else if (current != null) {
                current.continuation.append('\n').append(line)
            } else if (line.isNotBlank()) {
                orphanLines++
            }
        }

        val order = detectDateOrder(headers)
        val messages = ArrayList<ChatMessage>(headers.size)
        val participants = LinkedHashSet<String>()
        var invalidDates = 0

        for (h in headers) {
            val timestamp = h.toDateTime(order)
            if (timestamp == null) {
                invalidDates++
                continue
            }
            val message = buildMessage(h, timestamp)
            if (message.kind != MessageKind.SYSTEM && message.sender != null) {
                participants += message.sender
            }
            messages += message
        }

        val platform = when {
            headers.isEmpty() -> null
            headers.count { it.ios } * 2 >= headers.size -> ExportPlatform.IOS
            else -> ExportPlatform.ANDROID
        }

        return ParsedChat(
            messages = messages,
            participants = participants.toList(),
            platform = platform,
            dateOrder = order,
            totalLines = totalLines,
            unparsedLines = orphanLines + invalidDates,
            hasSeconds = headers.any { it.hasSeconds },
        )
    }

    // ---------------------------------------------------------------- satır başlığı

    private class RawHeader(
        val first: Int,
        val second: Int,
        val third: Int,
        val firstIsYear: Boolean,
        val hour: Int,
        val minute: Int,
        val second_: Int,
        val hasSeconds: Boolean,
        val meridiem: String?,
        val body: String,
        val ios: Boolean,
    ) {
        val continuation = StringBuilder()

        fun toDateTime(order: DateOrder): LocalDateTime? {
            val (year, month, day) = when (order) {
                DateOrder.YEAR_FIRST -> Triple(first, second, third)
                DateOrder.DAY_FIRST -> Triple(expandYear(third), second, first)
                DateOrder.MONTH_FIRST -> Triple(expandYear(third), first, second)
            }
            var h = hour
            when (meridiem?.let(::meridiemOf)) {
                Meridiem.AM -> if (h == 12) h = 0
                Meridiem.PM -> if (h < 12) h += 12
                null -> Unit
            }
            return try {
                LocalDateTime.of(year, month, day, h, minute, second_)
            } catch (_: DateTimeException) {
                null
            }
        }
    }

    private enum class Meridiem { AM, PM }

    private fun meridiemOf(raw: String): Meridiem? {
        val m = raw.replace(".", "").replace(" ", "").uppercase()
        return when (m) {
            "AM", "ÖÖ" -> Meridiem.AM
            "PM", "ÖS" -> Meridiem.PM
            else -> null
        }
    }

    private fun expandYear(y: Int): Int = if (y < 100) 2000 + y else y

    private fun matchHeader(line: String): RawHeader? {
        // iOS medya satırları başta bir yön işaretiyle başlar; başlığı eşleştirmeden önce at.
        val trimmed = line.trimStart('‎', '‏', '﻿')
        // Hızlı ret: başlıklar rakamla (Android) veya '[' ile (iOS) başlar. Çok satırlı
        // mesajların devam satırlarında regex hiç çalışmaz.
        val lead = trimmed.firstOrNull() ?: return null
        if (lead != '[' && !lead.isDigit()) return null
        val ios = lead == '['
        val match = (if (ios) IOS_HEADER else ANDROID_HEADER).matchEntire(trimmed) ?: return null
        val g = match.groupValues
        val first = g[1].toInt()
        return RawHeader(
            first = first,
            second = g[3].toInt(),
            third = g[4].toInt(),
            firstIsYear = g[1].length == 4,
            hour = g[5].toInt(),
            minute = g[6].toInt(),
            second_ = g[7].toIntOrNull() ?: 0,
            hasSeconds = g[7].isNotEmpty(),
            meridiem = g[8].ifEmpty { null },
            body = g[9],
            ios = ios,
        )
    }

    private fun detectDateOrder(headers: List<RawHeader>): DateOrder {
        if (headers.isEmpty()) return DateOrder.DAY_FIRST
        if (headers.count { it.firstIsYear } * 2 > headers.size) return DateOrder.YEAR_FIRST

        var firstOver12 = false
        var secondOver12 = false
        var usesMeridiem = false
        for (h in headers) {
            if (h.firstIsYear) continue
            if (h.first > 12) firstOver12 = true
            if (h.second > 12) secondOver12 = true
            if (h.meridiem != null) usesMeridiem = true
        }
        return when {
            firstOver12 && !secondOver12 -> DateOrder.DAY_FIRST
            secondOver12 && !firstOver12 -> DateOrder.MONTH_FIRST
            // Kanıt yoksa: ÖÖ/ÖS'siz (24 saat) biçimler Türkiye ve Avrupa'da gün önde,
            // AM/PM kullanan biçimler çoğunlukla ABD'de ay önde.
            usesMeridiem -> DateOrder.MONTH_FIRST
            else -> DateOrder.DAY_FIRST
        }
    }

    // ---------------------------------------------------------------- mesaj gövdesi

    private fun buildMessage(h: RawHeader, timestamp: LocalDateTime): ChatMessage {
        val fullBody = if (h.continuation.isEmpty()) h.body else h.body + h.continuation
        val separator = findSenderSeparator(fullBody)

        if (separator < 0) {
            // Android sistem mesajı: "Mesajlar ve aramalar uçtan uca şifrelidir..."
            return ChatMessage(timestamp, null, stripMarks(fullBody).trim(), MessageKind.SYSTEM)
        }

        val sender = cleanSender(fullBody.substring(0, separator))
        val rawText = fullBody.substring(separator + 2)
        val hadMark = rawText.startsWith('‎') || rawText.startsWith('‏')
        val result = MessageClassifier.classify(stripMarks(rawText), hadMark)

        return ChatMessage(
            timestamp = timestamp,
            sender = if (result.kind == MessageKind.SYSTEM) null else sender,
            text = result.text,
            kind = result.kind,
            mediaType = result.mediaType,
            edited = result.edited,
        )
    }

    /**
     * Gönderen adını metinden ayıran ilk ": " konumunu bulur.
     * Grup adı değişikliği gibi sistem mesajlarında tırnak içinde ":" geçebilir
     * (`Ali "Aile: 2026" grup adını oluşturdu`); tırnak içeren önekler ad sayılmaz.
     */
    private fun findSenderSeparator(body: String): Int {
        val idx = body.indexOf(": ")
        if (idx <= 0 || idx > MAX_SENDER_LENGTH) return -1
        val prefix = body.substring(0, idx)
        if (prefix.any { it == '"' || it == '“' || it == '”' || it == '\n' }) return -1
        return idx
    }

    private fun cleanSender(raw: String): String =
        stripMarks(raw)
            .trim()
            // Rehberde kayıtlı olmayan kişiler yeni sürümlerde "~ Ali" şeklinde görünür.
            .removePrefix("~")
            .trim()

    private fun isInvisibleMark(c: Char): Boolean =
        c == '‎' || c == '‏' || c in '‪'..'‮' || c in '⁦'..'⁩' || c == '﻿'

    /** Görünmez yön işaretlerini atar. Çoğu mesajda işaret yoktur; o zaman kopya oluşturulmaz. */
    private fun stripMarks(s: String): String {
        if (s.none(::isInvisibleMark)) return s
        return INVISIBLE_MARKS.replace(s, "")
    }

    private fun isOddSpace(c: Char): Boolean = c == ' ' || c == ' ' || c == ' '

    /** Dar ve bölünmez boşlukları (AM/PM öncesi U+202F gibi) normal boşluğa çevirir. */
    private fun normalizeSpaces(s: String): String {
        if (s.none(::isOddSpace)) return s
        return s.replace(' ', ' ').replace(' ', ' ').replace(' ', ' ')
    }
}
