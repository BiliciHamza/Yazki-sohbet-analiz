package com.hamzabilici.yazki.core.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ChatFileReaderTest {

    private val sample = "02.11.2026 20:34 - Ali: selam\n02.11.2026 20:35 - Ayşe: merhaba\n"

    @Test
    fun readsPlainText() {
        val chat = ChatFileReader.parse(ByteArrayInputStream(sample.toByteArray()))
        assertEquals(2, chat.messages.size)
        assertEquals("Ayşe", chat.messages[1].sender)
    }

    @Test
    fun readsIosZipWithMedia() {
        val zip = zipOf(
            "00000012-PHOTO-2026-11-02.jpg" to byteArrayOf(1, 2, 3),
            "_chat.txt" to sample.toByteArray(),
        )
        val chat = ChatFileReader.parse(ByteArrayInputStream(zip))
        assertEquals(listOf("Ali", "Ayşe"), chat.participants)
    }

    @Test
    fun readsAndroidZip() {
        val zip = zipOf("WhatsApp Sohbeti - Ayşe.txt" to sample.toByteArray())
        val chat = ChatFileReader.parse(ByteArrayInputStream(zip))
        assertEquals(2, chat.messages.size)
    }

    @Test(expected = ChatFileException::class)
    fun zipWithoutTextFails() {
        ChatFileReader.parse(ByteArrayInputStream(zipOf("photo.jpg" to byteArrayOf(1))))
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }
}
