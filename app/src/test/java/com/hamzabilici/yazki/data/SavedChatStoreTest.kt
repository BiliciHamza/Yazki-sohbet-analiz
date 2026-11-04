package com.hamzabilici.yazki.data

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SavedChatStoreTest {

    private lateinit var dir: File

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("yazki-store").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun saveLoadSelectAndDelete() = runBlocking {
        val store = SavedChatStore(dir)
        val stats = StatsJsonTest.sampleStats()

        val a = store.save("Ayşe", stats, now = 1_000)
        assertEquals(listOf(a.id), store.index().items.map { it.id })
        assertEquals(a.id, store.index().selectedId)
        assertEquals(stats, store.load(a.id))

        // Aynı sohbet tekrar aktarılınca yeni kayıt açılmaz, eskisi güncellenir.
        val again = store.save("Ayşe", stats, now = 2_000)
        assertEquals(a.id, again.id)
        assertEquals(1, store.index().items.size)
        assertEquals(2_000L, store.index().items.single().importedAt)

        // Farklı sohbet: en yeni üstte.
        val b = store.save("Aile", stats, now = 3_000)
        assertEquals(listOf(b.id, a.id), store.index().items.map { it.id })

        store.select(a.id)
        assertEquals(a.id, SavedChatStore(dir).index().selectedId) // diskten yeniden okununca da korunur

        val afterDelete = store.delete(a.id)
        assertEquals(listOf(b.id), afterDelete.items.map { it.id })
        assertNull(afterDelete.selectedId)
        assertNull(store.load(a.id))
    }

    @Test
    fun optionsArePersistedAndSurviveReimport() = runBlocking {
        val store = SavedChatStore(dir)
        val stats = StatsJsonTest.sampleStats()

        val saved = store.save("Ayşe", stats)
        assertEquals(ChatOptions(ChatType.FRIEND), saved.options) // 2 kişi → Arkadaş önerisi

        val partner = ChatOptions(ChatType.PARTNER, countProfanity = true, censor = false)
        store.updateOptions(saved.id, partner)
        assertEquals(partner, SavedChatStore(dir).index().items.single().options)

        // Aynı sohbet yeniden aktarılınca seçilen tür korunur.
        assertEquals(partner, store.save("Ayşe", stats).options)
    }

    @Test
    fun corruptIndexIsRebuiltFromFiles() = runBlocking {
        val stats = StatsJsonTest.sampleStats()
        val saved = SavedChatStore(dir).save("Ayşe", stats, now = 5_000)

        File(dir, "index.json").writeText("{bozuk")

        val fresh = SavedChatStore(dir)
        val index = fresh.index()
        assertEquals(listOf(saved.id), index.items.map { it.id })
        assertEquals("Ayşe", index.items.single().title)
        assertEquals(stats.totals.messages, index.items.single().messages)
        assertEquals(stats, fresh.load(saved.id))
    }

    @Test
    fun missingOrCorruptStatsFileReturnsNull() = runBlocking {
        val store = SavedChatStore(dir)
        val saved = store.save("Ayşe", StatsJsonTest.sampleStats())
        File(dir, "${saved.id}.json").writeText("bozuk")
        assertNull(store.load(saved.id))
        assertTrue(store.index().items.isNotEmpty())
    }
}
