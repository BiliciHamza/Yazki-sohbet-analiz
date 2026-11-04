package com.hamzabilici.yazki.data

import androidx.core.util.AtomicFile
import com.hamzabilici.yazki.core.analysis.ChatStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Kayıtlı sohbet listesindeki bir satır. İstatistiklerin kendisi ayrı dosyadadır. */
data class SavedChatInfo(
    val id: String,
    val title: String?,
    /** Aktarıldığı an, epoch milisaniye. */
    val importedAt: Long,
    val messages: Int,
    val participants: List<String>,
    val firstDay: LocalDate?,
    val lastDay: LocalDate?,
    /** Sohbet türü ve küfür ayarları. */
    val options: ChatOptions,
    /** Aynı sohbet tekrar aktarılınca yeni kayıt yerine eskisini güncellemek için. */
    internal val key: String,
)

/**
 * Analiz sonuçlarını uygulamanın özel klasöründe saklar:
 *
 * ```
 * files/analyses/index.json   → liste + seçili sohbet
 * files/analyses/<id>.json    → {"v":1, "meta":{...}, "stats":{...}}
 * ```
 *
 * Yalnızca hesaplanmış istatistikler yazılır, mesaj metni yazılmaz. Yazmalar
 * [AtomicFile] ile yapılır; yazma yarıda kesilse bile eski dosya bozulmaz. Dizin
 * okunamazsa kayıt dosyalarındaki "meta" bölümünden yeniden kurulur.
 *
 * Bütün işlemler tek bir kilit altında, IO iş parçacığında çalışır.
 */
class SavedChatStore(private val dir: File) {

    data class Index(val items: List<SavedChatInfo>, val selectedId: String?)

    private val mutex = Mutex()
    private var cache: Index? = null

    suspend fun index(): Index = locked { indexLocked() }

    /**
     * Analizi kaydeder ve seçili yapar. Aynı sohbet (aynı ad, aynı ilk mesaj zamanı,
     * aynı kişiler) daha önce kaydedildiyse o kayıt güncellenir.
     *
     * @param options null ise önceki kaydın ayarları, o da yoksa katılımcı sayısına göre öneri kullanılır.
     */
    suspend fun save(
        title: String?,
        stats: ChatStats,
        options: ChatOptions? = null,
        now: Long = System.currentTimeMillis(),
    ): SavedChatInfo =
        locked {
            val index = indexLocked()
            val key = keyOf(title, stats)
            val existing = index.items.firstOrNull { it.key == key }
            val info = SavedChatInfo(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title,
                importedAt = now,
                messages = stats.totals.messages,
                participants = stats.participants.map { it.name },
                firstDay = stats.time.first?.toLocalDate(),
                lastDay = stats.time.last?.toLocalDate(),
                options = options ?: existing?.options ?: ChatOptions.defaultFor(stats.participants.size),
                key = key,
            )
            val file = JSONObject()
                .put("v", StatsJson.VERSION)
                .put("meta", encodeInfo(info))
                .put("stats", StatsJson.encode(stats))
            writeAtomic(fileFor(info.id), file.toString())

            val items = listOf(info) + index.items.filter { it.id != info.id }
            writeIndex(Index(items, info.id))
            info
        }

    /** Kayıtlı istatistikleri okur. Dosya yoksa veya bozuksa null. */
    suspend fun load(id: String): ChatStats? = locked {
        val file = fileFor(id).takeIf { it.exists() } ?: return@locked null
        runCatching {
            val root = JSONObject(String(AtomicFile(file).readFully(), Charsets.UTF_8))
            StatsJson.decode(root.getJSONObject("stats"))
        }.getOrNull()
    }

    /** Sohbetin türünü ve küfür ayarlarını günceller. İstatistik dosyasına dokunulmaz. */
    suspend fun updateOptions(id: String, options: ChatOptions): SavedChatInfo? = locked {
        val index = indexLocked()
        val current = index.items.firstOrNull { it.id == id } ?: return@locked null
        val updated = current.copy(options = options)
        writeIndex(index.copy(items = index.items.map { if (it.id == id) updated else it }))
        updated
    }

    suspend fun select(id: String?) = locked {
        val index = indexLocked()
        if (index.selectedId != id) writeIndex(index.copy(selectedId = id))
    }

    suspend fun delete(id: String): Index = locked {
        val index = indexLocked()
        AtomicFile(fileFor(id)).delete()
        val updated = Index(
            items = index.items.filter { it.id != id },
            selectedId = index.selectedId.takeIf { it != id },
        )
        writeIndex(updated)
        updated
    }

    // ---------------------------------------------------------------- iç işler

    private suspend fun <T> locked(block: () -> T): T =
        mutex.withLock { withContext(Dispatchers.IO) { block() } }

    private val indexFile get() = File(dir, "index.json")

    /** Kimlikler yalnızca UUID; dosya yolu dışına çıkılamaz. */
    private fun fileFor(id: String): File {
        require(ID_PATTERN.matches(id)) { "Geçersiz kayıt kimliği" }
        return File(dir, "$id.json")
    }

    private fun indexLocked(): Index {
        cache?.let { return it }
        val loaded = readIndex() ?: rebuildIndex()
        cache = loaded
        return loaded
    }

    private fun readIndex(): Index? {
        if (!AtomicFile(indexFile).baseFile.exists()) return null
        return runCatching {
            val root = JSONObject(String(AtomicFile(indexFile).readFully(), Charsets.UTF_8))
            val items = root.optJSONArray("items")
            val list = (0 until (items?.length() ?: 0)).mapNotNull { items?.optJSONObject(it)?.let(::decodeInfo) }
                // Dosyası silinmiş kayıtları gösterme
                .filter { fileFor(it.id).exists() }
            Index(list, root.optString("selected").ifEmpty { null }?.takeIf { s -> list.any { it.id == s } })
        }.getOrNull()
    }

    /** Dizin yok ya da bozuksa kayıt dosyalarını tarayıp yeniden kurar. */
    private fun rebuildIndex(): Index {
        val files = dir.listFiles { f -> f.isFile && f.name != "index.json" && f.name.endsWith(".json") }.orEmpty()
        val items = files.mapNotNull { f ->
            runCatching {
                val root = JSONObject(String(AtomicFile(f).readFully(), Charsets.UTF_8))
                decodeInfo(root.getJSONObject("meta"))
            }.getOrNull()
        }.sortedByDescending { it.importedAt }
        val index = Index(items, null)
        if (items.isNotEmpty()) writeIndex(index)
        return index
    }

    private fun writeIndex(index: Index) {
        val root = JSONObject()
            .put("v", StatsJson.VERSION)
            .put("items", JSONArray().apply { index.items.forEach { put(encodeInfo(it)) } })
        index.selectedId?.let { root.put("selected", it) }
        writeAtomic(indexFile, root.toString())
        cache = index
    }

    private fun writeAtomic(file: File, text: String) {
        dir.mkdirs()
        val atomic = AtomicFile(file)
        val out = atomic.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(out)
        } catch (e: Exception) {
            atomic.failWrite(out)
            throw e
        }
    }

    private fun encodeInfo(i: SavedChatInfo): JSONObject = JSONObject()
        .put("id", i.id)
        .put("importedAt", i.importedAt)
        .put("messages", i.messages)
        .put("participants", JSONArray().apply { i.participants.forEach { put(it) } })
        .put("key", i.key)
        .put("type", i.options.type.name)
        .put("countProfanity", i.options.countProfanity)
        .put("censor", i.options.censor)
        .apply {
            i.title?.let { put("title", it) }
            i.firstDay?.let { put("firstDay", it.toString()) }
            i.lastDay?.let { put("lastDay", it.toString()) }
        }

    private fun decodeInfo(o: JSONObject): SavedChatInfo? {
        val id = o.optString("id").takeIf { ID_PATTERN.matches(it) } ?: return null
        val people = o.optJSONArray("participants")
        return SavedChatInfo(
            id = id,
            title = o.optString("title").ifEmpty { null },
            importedAt = o.optLong("importedAt"),
            messages = o.optInt("messages"),
            participants = (0 until (people?.length() ?: 0)).map { people!!.optString(it) },
            firstDay = o.optString("firstDay").ifEmpty { null }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            lastDay = o.optString("lastDay").ifEmpty { null }?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            options = ChatOptions(
                type = ChatType.fromName(o.optString("type")) ?: ChatType.suggestFor(people?.length() ?: 0),
                countProfanity = o.optBoolean("countProfanity", true),
                censor = o.optBoolean("censor", true),
            ),
            key = o.optString("key"),
        )
    }

    companion object {
        private val ID_PATTERN = Regex("[0-9a-f-]{36}")

        internal fun keyOf(title: String?, stats: ChatStats): String =
            listOf(
                title.orEmpty(),
                stats.time.first?.toString().orEmpty(),
                stats.participants.map { it.name }.sorted().joinToString("\u0001"),
            ).joinToString("\u0000")
    }
}
