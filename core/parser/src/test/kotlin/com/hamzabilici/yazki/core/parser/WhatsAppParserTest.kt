package com.hamzabilici.yazki.core.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WhatsAppParserTest {

    @Test
    fun androidTurkish() {
        val chat = WhatsAppParser.parse(
            """
            02.11.2026 20:30 - Mesajlar ve aramalar uçtan uca şifrelidir. Bu sohbetin dışında hiç kimse, WhatsApp bile, mesajlarınızı okuyamaz ve aramalarınızı dinleyemez. Daha fazla bilgi edinmek için dokunun.
            02.11.2026 20:34 - Ali: Selam naber
            02.11.2026 20:35 - Ayşe Yılmaz: İyiyim sen?
            ikinci satır
            üçüncü satır
            02.11.2026 20:36 - Ali: <Medya dahil edilmedi>
            02.11.2026 20:37 - Ayşe Yılmaz: Bu mesaj silindi
            02.11.2026 20:38 - Ali: tamam geliyorum <Bu mesaj düzenlendi>
            """.trimIndent(),
        )

        assertEquals(ExportPlatform.ANDROID, chat.platform)
        assertEquals(DateOrder.DAY_FIRST, chat.dateOrder)
        assertFalse("Android dışa aktarımında saniye yok", chat.hasSeconds)
        assertEquals(listOf("Ali", "Ayşe Yılmaz"), chat.participants)
        assertEquals(6, chat.messages.size)
        assertEquals(0, chat.unparsedLines)

        val system = chat.messages[0]
        assertEquals(MessageKind.SYSTEM, system.kind)
        assertNull(system.sender)

        assertEquals(LocalDateTime.of(2026, 11, 2, 20, 34), chat.messages[1].timestamp)
        assertEquals("İyiyim sen?\nikinci satır\nüçüncü satır", chat.messages[2].text)

        assertEquals(MessageKind.MEDIA, chat.messages[3].kind)
        assertEquals(MediaType.UNKNOWN, chat.messages[3].mediaType)
        assertEquals(MessageKind.DELETED, chat.messages[4].kind)

        val edited = chat.messages[5]
        assertTrue(edited.edited)
        assertEquals("tamam geliyorum", edited.text)
        assertEquals(MessageKind.TEXT, edited.kind)
    }

    @Test
    fun androidEnglishWithAmPmAndNarrowSpace() {
        val chat = WhatsAppParser.parse(
            """
            11/2/26, 8:34${' '}PM - Ali: Hello
            11/2/26, 9:05${' '}AM - Bob: <Media omitted>
            11/13/26, 12:01${' '}AM - Ali: You deleted this message
            """.trimIndent(),
        )
        assertEquals(DateOrder.MONTH_FIRST, chat.dateOrder)
        assertEquals(LocalDateTime.of(2026, 11, 2, 20, 34), chat.messages[0].timestamp)
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 5), chat.messages[1].timestamp)
        assertEquals(LocalDateTime.of(2026, 11, 13, 0, 1), chat.messages[2].timestamp)
        assertEquals(MessageKind.DELETED, chat.messages[2].kind)
    }

    @Test
    fun iosTurkishWithDirectionMarks() {
        val lrm = '‎'
        val chat = WhatsAppParser.parse(
            """
            [02.11.2026 20:30:00] Aile Grubu: ${lrm}Mesajlar ve aramalar uçtan uca şifrelidir.
            [02.11.2026 20:30:05] Aile Grubu: ${lrm}Ali grubu oluşturdu
            [02.11.2026 20:34:01] Ali: Selam
            $lrm[02.11.2026 20:34:10] Ayşe: ${lrm}görüntü dahil edilmedi
            $lrm[02.11.2026 20:34:20] Ayşe: ${lrm}ses dahil edilmedi
            $lrm[02.11.2026 20:35:00] Ali: ${lrm}Çıkartma dahil edilmedi
            $lrm[02.11.2026 20:36:00] Ali: ${lrm}<ekli: 00000012-PHOTO-2026-11-02-20-36-00.jpg>
            [02.11.2026 20:37:00] Ayşe: ${lrm}rapor.pdf • 3 sayfa ${lrm}belge dahil edilmedi
            """.trimIndent(),
        )

        assertEquals(ExportPlatform.IOS, chat.platform)
        assertTrue("iOS dışa aktarımında saniye var", chat.hasSeconds)
        assertEquals(listOf("Ali", "Ayşe"), chat.participants)
        assertFalse("Grup adı katılımcı sayılmamalı", "Aile Grubu" in chat.participants)

        val kinds = chat.messages.map { it.kind }
        assertEquals(
            listOf(
                MessageKind.SYSTEM, MessageKind.SYSTEM, MessageKind.TEXT, MessageKind.MEDIA,
                MessageKind.MEDIA, MessageKind.MEDIA, MessageKind.MEDIA, MessageKind.MEDIA,
            ),
            kinds,
        )
        assertEquals(
            listOf(MediaType.IMAGE, MediaType.AUDIO, MediaType.STICKER, MediaType.IMAGE, MediaType.DOCUMENT),
            chat.messages.drop(3).map { it.mediaType },
        )
        assertEquals(LocalDateTime.of(2026, 11, 2, 20, 34, 1), chat.messages[2].timestamp)
    }

    @Test
    fun iosEnglish() {
        val chat = WhatsAppParser.parse(
            """
            [11/2/26, 8:34:01 PM] Ali: Hi
            [11/2/26, 8:35:00 PM] Bob: ${'‎'}image omitted
            """.trimIndent(),
        )
        assertEquals(DateOrder.MONTH_FIRST, chat.dateOrder)
        assertEquals(LocalDateTime.of(2026, 11, 2, 20, 34, 1), chat.messages[0].timestamp)
        assertEquals(MediaType.IMAGE, chat.messages[1].mediaType)
    }

    @Test
    fun dayMonthOrderIsDecidedFromWholeFile() {
        // İlk satır tek başına belirsiz (03/04); 25/04 gün önde olduğunu kanıtlıyor.
        val chat = WhatsAppParser.parse(
            """
            03/04/2026, 10:00 - Ali: bir
            25/04/2026, 10:00 - Ali: iki
            """.trimIndent(),
        )
        assertEquals(DateOrder.DAY_FIRST, chat.dateOrder)
        assertEquals(LocalDateTime.of(2026, 4, 3, 10, 0), chat.messages[0].timestamp)
    }

    @Test
    fun twoDigitYearAndCommaSeparatedAndroid() {
        val chat = WhatsAppParser.parse("2.11.26, 09:05 - Ali: merhaba")
        assertEquals(LocalDateTime.of(2026, 11, 2, 9, 5), chat.messages.single().timestamp)
    }

    @Test
    fun yearFirst() {
        val chat = WhatsAppParser.parse("2026-11-02 21:15 - Ali: selam")
        assertEquals(DateOrder.YEAR_FIRST, chat.dateOrder)
        assertEquals(LocalDateTime.of(2026, 11, 2, 21, 15), chat.messages.single().timestamp)
    }

    @Test
    fun specialCharacterNamesAndTildePrefix() {
        val chat = WhatsAppParser.parse(
            """
            02.11.2026 20:34 - Øyvind (iş) 🚀: test
            02.11.2026 20:35 - ~ Çağrı: yeni üye
            02.11.2026 20:36 - +90 555 123 45 67: numara
            """.trimIndent(),
        )
        assertEquals(listOf("Øyvind (iş) 🚀", "Çağrı", "+90 555 123 45 67"), chat.participants)
    }

    @Test
    fun colonInsideMessageTextIsKept() {
        val chat = WhatsAppParser.parse("02.11.2026 20:34 - Ali: saat: 21:00 buluşalım")
        val m = chat.messages.single()
        assertEquals("Ali", m.sender)
        assertEquals("saat: 21:00 buluşalım", m.text)
    }

    @Test
    fun groupSubjectChangeWithColonIsSystem() {
        val chat = WhatsAppParser.parse("02.11.2026 20:34 - Ali \"Aile: 2026\" grup adını oluşturdu")
        assertEquals(MessageKind.SYSTEM, chat.messages.single().kind)
        assertTrue(chat.participants.isEmpty())
    }

    @Test
    fun androidNullLineIsMediaNotWord() {
        val chat = WhatsAppParser.parse("02.11.2026 20:34 - Ali: null")
        assertEquals(MessageKind.MEDIA, chat.messages.single().kind)
        assertEquals(MediaType.VIEW_ONCE, chat.messages.single().mediaType)
    }

    @Test
    fun androidAttachedFile() {
        val chat = WhatsAppParser.parse("02.11.2026 20:34 - Ali: PTT-20261102-WA0003.opus (dosya ekli)")
        assertEquals(MediaType.AUDIO, chat.messages.single().mediaType)
    }

    @Test
    fun garbageBeforeFirstMessageIsReported() {
        val chat = WhatsAppParser.parse(
            """
            bu bir başlık değil
            02.11.2026 20:34 - Ali: selam
            """.trimIndent(),
        )
        assertEquals(1, chat.unparsedLines)
        assertEquals(1, chat.messages.size)
    }

    @Test
    fun windowsLineEndingsAndBom() {
        val chat = WhatsAppParser.parse("﻿02.11.2026 20:34 - Ali: selam\r\n02.11.2026 20:35 - Veli: merhaba\r\n")
        assertEquals(listOf("selam", "merhaba"), chat.messages.map { it.text })
    }
}
