package com.hamzabilici.yazki.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** Paylaş menüsünden veya "Birlikte aç" ile gelen intent'ten sohbet dosyasının adresini çıkarır. */
object SharedChatIntent {

    val HANDLED_ACTIONS = setOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_VIEW)

    fun chatUri(intent: Intent, resolver: ContentResolver): Uri? = when (intent.action) {
        Intent.ACTION_VIEW -> intent.data
        Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        Intent.ACTION_SEND_MULTIPLE ->
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                ?.let { pickChatFile(it, resolver) }
        else -> null
    }

    /**
     * Medyalı paylaşımda sohbet metni fotoğraf ve seslerle birlikte gelir;
     * önce .txt, sonra .zip seçilir.
     */
    private fun pickChatFile(uris: List<Uri>, resolver: ContentResolver): Uri? {
        if (uris.size <= 1) return uris.firstOrNull()
        val named = uris.map { it to ChatImporter.displayName(resolver, it).orEmpty().lowercase() }
        return named.firstOrNull { it.second.endsWith(".txt") }?.first
            ?: named.firstOrNull { it.second.endsWith(".zip") }?.first
            ?: uris.first()
    }
}
