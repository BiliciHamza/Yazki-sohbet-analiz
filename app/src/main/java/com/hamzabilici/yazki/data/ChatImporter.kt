package com.hamzabilici.yazki.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.hamzabilici.yazki.core.analysis.ChatAnalyzer
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.core.parser.ChatFileException
import com.hamzabilici.yazki.core.parser.ChatFileReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ekranda gösterilen sohbet. Ham mesaj metni bilerek tutulmaz; yalnızca hesaplanan istatistikler kalır.
 */
data class ImportResult(
    /** Kayıtlı sohbetlerdeki kimliği. */
    val id: String,
    /** Dosya adından türetilen sohbet adı ("WhatsApp Sohbeti - Ayşe.txt" → "Ayşe"). */
    val title: String?,
    val stats: ChatStats,
    /** Sohbet türü ve küfür ayarları; karnede neyin gösterileceğini belirler. */
    val options: ChatOptions = ChatOptions.defaultFor(stats.participants.size),
)

/** Henüz kaydedilmemiş analiz çıktısı. */
data class AnalyzedChat(val title: String?, val stats: ChatStats)

/** Kullanıcının seçtiği veya paylaştığı dosyayı okur, ayrıştırır ve analiz eder. */
class ChatImporter(
    private val resolver: ContentResolver,
    private val analyzer: ChatAnalyzer = ChatAnalyzer(),
) {

    suspend fun import(uri: Uri): AnalyzedChat {
        val name = withContext(Dispatchers.IO) { displayName(resolver, uri) }
        // Ayrıştırma da analiz de işlemci yoğun; okuma akış halinde olduğu için tek blokta yapılır.
        // Ham mesaj listesi bu bloğun dışına çıkmaz, analiz biter bitmez bellekten atılabilir.
        val stats = withContext(Dispatchers.Default) {
            val stream = resolver.openInputStream(uri) ?: throw ChatFileException("Dosya açılamadı")
            analyzer.analyze(ChatFileReader.parse(stream))
        }
        return AnalyzedChat(title = chatTitleFromFileName(name), stats = stats)
    }

    companion object {
        private val TITLE_PREFIXES = listOf(
            "WhatsApp Sohbeti - ",
            "WhatsApp Sohbeti – ",
            "WhatsApp Chat with ",
            "WhatsApp Chat - ",
            "WhatsApp Chat – ",
        )

        fun displayName(resolver: ContentResolver, uri: Uri): String? =
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull() ?: uri.lastPathSegment

        fun chatTitleFromFileName(fileName: String?): String? {
            if (fileName.isNullOrBlank()) return null
            var title = fileName.substringAfterLast('/').substringBeforeLast('.')
            for (prefix in TITLE_PREFIXES) {
                if (title.startsWith(prefix, ignoreCase = true)) {
                    title = title.substring(prefix.length)
                    break
                }
            }
            return title.trim().ifBlank { null }
        }
    }
}
