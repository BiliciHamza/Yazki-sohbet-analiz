package com.hamzabilici.yazki.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatTitleTest {

    @Test
    fun stripsWhatsAppPrefixAndExtension() {
        assertEquals("Ayşe", ChatImporter.chatTitleFromFileName("WhatsApp Sohbeti - Ayşe.txt"))
        assertEquals("Aile Grubu", ChatImporter.chatTitleFromFileName("WhatsApp Chat with Aile Grubu.zip"))
        assertEquals("_chat", ChatImporter.chatTitleFromFileName("_chat.txt"))
    }

    @Test
    fun blankIsNull() {
        assertNull(ChatImporter.chatTitleFromFileName(null))
        assertNull(ChatImporter.chatTitleFromFileName(""))
    }
}
