package com.hamzabilici.yazki.core.parser

import java.util.Locale

/**
 * Mesaj gövdesinin medya, silinmiş, düzenlenmiş veya sistem mesajı olup olmadığını belirler.
 *
 * Türkçe ve İngilizce, Android ve iOS ibareleri desteklenir. Yeni bir dil eklemek için
 * buradaki listelere ekleme yapmak yeterli.
 */
internal object MessageClassifier {

    data class Result(
        val kind: MessageKind,
        val text: String,
        val mediaType: MediaType? = null,
        val edited: Boolean = false,
    )

    private val ROOT = Locale.ROOT

    private val DELETED = setOf(
        "bu mesaj silindi",
        "bu mesajı sildiniz",
        "this message was deleted",
        "you deleted this message",
    )

    private val EDITED_SUFFIXES = listOf(
        "<bu mesaj düzenlendi>",
        "<this message was edited>",
    )

    /** Tam eşleşen medya ibareleri (küçük harfe çevrilmiş, kenar boşlukları atılmış). */
    private val MEDIA_EXACT: Map<String, MediaType> = mapOf(
        // Android
        "<medya dahil edilmedi>" to MediaType.UNKNOWN,
        "<media omitted>" to MediaType.UNKNOWN,
        "null" to MediaType.VIEW_ONCE,
        // iOS - Türkçe
        "görüntü dahil edilmedi" to MediaType.IMAGE,
        "fotoğraf dahil edilmedi" to MediaType.IMAGE,
        "video dahil edilmedi" to MediaType.VIDEO,
        "ses dahil edilmedi" to MediaType.AUDIO,
        "çıkartma dahil edilmedi" to MediaType.STICKER,
        "gif dahil edilmedi" to MediaType.GIF,
        "kişi kartı dahil edilmedi" to MediaType.CONTACT,
        // iOS - İngilizce
        "image omitted" to MediaType.IMAGE,
        "video omitted" to MediaType.VIDEO,
        "audio omitted" to MediaType.AUDIO,
        "sticker omitted" to MediaType.STICKER,
        "gif omitted" to MediaType.GIF,
        "contact card omitted" to MediaType.CONTACT,
    )

    /** iOS belge satırları dosya adıyla başlar: "rapor.pdf • 3 sayfa belge dahil edilmedi". */
    private val DOCUMENT_SUFFIXES = listOf("belge dahil edilmedi", "document omitted")

    /** Medya dahil dışa aktarımda dosya adı: "<ekli: 00000012-PHOTO-...jpg>" (iOS). */
    private val ATTACHED_IOS = Regex("""^<(?:ekli|attached):\s*(.+)>$""", RegexOption.IGNORE_CASE)

    /** Medya dahil dışa aktarımda dosya adı: "IMG-20261102-WA0001.jpg (dosya ekli)" (Android). */
    private val ATTACHED_ANDROID = Regex("""^(.+?)\s*\((?:dosya ekli|file attached)\)$""", RegexOption.IGNORE_CASE)

    private val LOCATION = Regex("""^(?:konum|location|canlı konum|live location)\s*:""", RegexOption.IGNORE_CASE)
    private val POLL = Regex("""^(?:anket|poll)\s*:""", RegexOption.IGNORE_CASE)

    /**
     * @param body Gönderen adından sonraki metin, yön işaretleri temizlenmiş.
     * @param hadDirectionMark iOS'ta medya ve sistem mesajlarının başında görünmez bir
     *   soldan-sağa işareti (U+200E) olur; normal kullanıcı metninde olmaz.
     */
    fun classify(body: String, hadDirectionMark: Boolean): Result {
        var text = body.trim()
        var edited = false

        val lower = text.lowercase(ROOT)
        for (suffix in EDITED_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                edited = true
                text = text.substring(0, text.length - suffix.length).trimEnd()
                break
            }
        }

        val key = text.lowercase(ROOT).trimEnd('.')

        if (key in DELETED) return Result(MessageKind.DELETED, text, edited = edited)

        MEDIA_EXACT[key]?.let { return Result(MessageKind.MEDIA, text, it, edited) }
        if (DOCUMENT_SUFFIXES.any { key.endsWith(it) }) {
            return Result(MessageKind.MEDIA, text, MediaType.DOCUMENT, edited)
        }

        val attachedName = ATTACHED_IOS.find(text)?.groupValues?.get(1)
            ?: ATTACHED_ANDROID.find(text)?.groupValues?.get(1)
        if (attachedName != null) {
            return Result(MessageKind.MEDIA, text, mediaTypeFromFileName(attachedName), edited)
        }

        if (LOCATION.containsMatchIn(text)) return Result(MessageKind.MEDIA, text, MediaType.LOCATION, edited)
        if (POLL.containsMatchIn(text)) return Result(MessageKind.MEDIA, text, MediaType.POLL, edited)

        // iOS'ta yön işaretiyle başlayıp hiçbir kalıba uymayan satırlar sistem mesajıdır
        // (grup oluşturma, üye ekleme, şifreleme bildirimi vb.).
        if (hadDirectionMark) return Result(MessageKind.SYSTEM, text)

        return Result(MessageKind.TEXT, text, edited = edited)
    }

    fun mediaTypeFromFileName(fileName: String): MediaType {
        val name = fileName.uppercase(ROOT)
        val ext = name.substringAfterLast('.', "")
        return when {
            "STICKER" in name || ext == "WEBP" -> MediaType.STICKER
            "GIF" in name -> MediaType.GIF
            "PHOTO" in name || name.startsWith("IMG") || ext in setOf("JPG", "JPEG", "PNG", "HEIC") -> MediaType.IMAGE
            "VIDEO" in name || name.startsWith("VID") || ext in setOf("MP4", "MOV", "3GP") -> MediaType.VIDEO
            "AUDIO" in name || name.startsWith("PTT") || ext in setOf("OPUS", "OGG", "M4A", "MP3", "AAC") -> MediaType.AUDIO
            ext == "VCF" -> MediaType.CONTACT
            else -> MediaType.DOCUMENT
        }
    }
}
