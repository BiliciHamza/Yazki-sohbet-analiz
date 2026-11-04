package com.hamzabilici.yazki.core.parser

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.util.zip.ZipInputStream

class ChatFileException(message: String) : IOException(message)

/**
 * WhatsApp dışa aktarım dosyasını açar. Düz `.txt` ve medyalı/medyasız `.zip` desteklenir;
 * zip içinde `_chat.txt` (iOS) veya ilk `.txt` dosyası (Android) okunur.
 *
 * Dosya belleğe tamamen alınmaz; dönen [Reader] akış halinde okunur.
 */
object ChatFileReader {

    private const val IOS_CHAT_ENTRY = "_chat.txt"

    /** Akışı açıp ayrıştırır. Akışı kapatır. */
    fun parse(input: InputStream): ParsedChat =
        openReader(input).use { reader -> WhatsAppParser.parse(reader.buffered().lineSequence()) }

    /** Akışın içeriğini metin olarak açar. Dönen okuyucuyu kapatmak çağıranın sorumluluğundadır. */
    fun openReader(input: InputStream): Reader {
        val buffered = if (input is BufferedInputStream) input else BufferedInputStream(input)
        return if (isZip(buffered)) openZipEntry(buffered) else buffered.reader(Charsets.UTF_8)
    }

    private fun isZip(input: BufferedInputStream): Boolean {
        input.mark(4)
        val magic = ByteArray(4)
        val read = input.readNBytesCompat(magic)
        input.reset()
        return read == 4 &&
            magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte() &&
            magic[2] == 3.toByte() && magic[3] == 4.toByte()
    }

    private fun openZipEntry(input: InputStream): Reader {
        val zip = ZipInputStream(input, Charsets.UTF_8)
        // Zip akışında geri dönülemediği için ilk uygun .txt girdisi okunur.
        // WhatsApp zip'lerinde zaten tek bir sohbet metni bulunur.
        while (true) {
            val entry = zip.nextEntry ?: break
            val name = entry.name.substringAfterLast('/')
            val isChatText = !entry.isDirectory &&
                !entry.name.startsWith("__MACOSX") &&
                !name.startsWith("._") &&
                (name == IOS_CHAT_ENTRY || name.endsWith(".txt", ignoreCase = true))
            if (isChatText) return zip.reader(Charsets.UTF_8)
        }
        zip.close()
        throw ChatFileException("Zip dosyasında sohbet metni (.txt) bulunamadı")
    }

    /** InputStream.readNBytes Android'de API 33+; burada elle okunur. */
    private fun InputStream.readNBytesCompat(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val n = read(buffer, total, buffer.size - total)
            if (n < 0) break
            total += n
        }
        return total
    }
}
