package com.hamzabilici.yazki.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ChatImporter
import com.hamzabilici.yazki.data.ChatOptions
import com.hamzabilici.yazki.data.ImportResult
import com.hamzabilici.yazki.data.SavedChatInfo
import com.hamzabilici.yazki.data.SavedChatStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Sohbet türü ve analiz ayarları ekranı.
 *
 * @param isNew Yeni aktarılan sohbet mi? Öyleyse onaylanınca Karne'ye geçilir; vazgeçilirse
 *   önerilen ayarlarla devam edilir (sohbet kaybolmaz).
 */
data class SetupRequest(
    val resultId: String,
    val participants: Int,
    val initial: ChatOptions,
    val isNew: Boolean,
)

data class AppUiState(
    /** Kayıtlı sohbetler, en yeni üstte. */
    val saved: List<SavedChatInfo> = emptyList(),
    /** Karne ve İstatistik'te gösterilen seçili sohbet. */
    val result: ImportResult? = null,
    val loading: Boolean = false,
    /** Uygulama açılırken kayıtlar okunuyor. */
    val restoring: Boolean = true,
    @param:StringRes val error: Int? = null,
    /** Açıksa tür ve analiz ayarları ekranı gösterilir. */
    val setup: SetupRequest? = null,
    /** Her yeni sohbet kurulumu tamamlandığında artar; arayüz bununla Karne sekmesine geçer. */
    val resultVersion: Int = 0,
)

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val importer = ChatImporter(application.contentResolver)
    private val store = SavedChatStore(File(application.filesDir, "analyses"))

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    /** Aktarma veya seçme işi; yenisi başlayınca eskisi iptal edilir. */
    private var job: Job? = null

    /** Açılışta kayıtları ve en son seçili sohbeti geri yükler. */
    private val restoreJob: Job = viewModelScope.launch {
        val index = runCatching { store.index() }.getOrNull()
        val selected = index?.selectedId?.let { id -> index.items.firstOrNull { it.id == id } }
        val stats = selected?.let { runCatching { store.load(it.id) }.getOrNull() }
        _state.update { s ->
            s.copy(
                saved = index?.items.orEmpty(),
                // Bu arada paylaş menüsünden yeni bir sohbet geldiyse onu ezme.
                result = s.result ?: if (selected != null && stats != null) {
                    ImportResult(selected.id, selected.title, stats, selected.options)
                } else {
                    null
                },
                restoring = false,
            )
        }
    }

    fun import(uri: Uri) {
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch {
            try {
                val analyzed = importer.import(uri)
                if (analyzed.stats.totals.messages == 0) {
                    _state.update { it.copy(loading = false, error = R.string.error_no_messages) }
                    return@launch
                }
                // Liste geri yüklenmeden kaydedersek açılıştaki güncelleme yeni kaydı ezebilir.
                restoreJob.join()
                // Kaydetme başarısız olsa bile (dolu disk vb.) sonucu göster.
                val info = runCatching { store.save(analyzed.title, analyzed.stats) }.getOrNull()
                val saved = runCatching { store.index().items }.getOrNull()
                val options = info?.options ?: ChatOptions.defaultFor(analyzed.stats.participants.size)
                val result = ImportResult(info?.id ?: UNSAVED_ID, analyzed.title, analyzed.stats, options)
                _state.update {
                    it.copy(
                        result = result,
                        saved = saved ?: it.saved,
                        loading = false,
                        error = null,
                        // Önce sohbetin türü sorulur; Karne'ye kurulum bitince geçilir.
                        setup = SetupRequest(result.id, analyzed.stats.participants.size, options, isNew = true),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = R.string.error_not_chat) }
            }
        }
    }

    /** Karne'den "Analiz ayarları"na basılınca. */
    fun editOptions() {
        val result = _state.value.result ?: return
        _state.update {
            it.copy(setup = SetupRequest(result.id, result.stats.participants.size, result.options, isNew = false))
        }
    }

    /** Tür ve ayarlar seçildi. Ayarlar saklanır, karne anında güncellenir (yeniden analiz gerekmez). */
    fun confirmSetup(options: ChatOptions) {
        val request = _state.value.setup ?: return
        _state.update { s ->
            s.copy(
                setup = null,
                result = s.result?.let { r -> if (r.id == request.resultId) r.copy(options = options) else r },
                saved = s.saved.map { if (it.id == request.resultId) it.copy(options = options) else it },
                resultVersion = if (request.isNew) s.resultVersion + 1 else s.resultVersion,
            )
        }
        if (request.resultId != UNSAVED_ID) {
            viewModelScope.launch { runCatching { store.updateOptions(request.resultId, options) } }
        }
    }

    /** Kurulumdan geri tuşuyla çıkılınca: yeni sohbette önerilen ayarlarla devam et, düzenlemede vazgeç. */
    fun dismissSetup() {
        val request = _state.value.setup ?: return
        if (request.isNew) confirmSetup(request.initial) else _state.update { it.copy(setup = null) }
    }

    /** Kayıtlı bir sohbeti seçer; Karne ve İstatistik onu gösterir. */
    fun select(id: String) {
        if (_state.value.result?.id == id) return
        val info = _state.value.saved.firstOrNull { it.id == id } ?: return
        job?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        job = viewModelScope.launch {
            val stats = runCatching { store.load(id) }.getOrNull()
            if (stats == null) {
                _state.update { it.copy(loading = false, error = R.string.error_saved_unreadable) }
                return@launch
            }
            runCatching { store.select(id) }
            _state.update { it.copy(result = ImportResult(id, info.title, stats, info.options), loading = false) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            val index = runCatching { store.delete(id) }.getOrNull() ?: return@launch
            _state.update {
                it.copy(
                    saved = index.items,
                    result = it.result?.takeIf { r -> r.id != id },
                )
            }
        }
    }

    fun showError(@StringRes messageRes: Int) {
        job?.cancel()
        _state.update { it.copy(loading = false, error = messageRes) }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private companion object {
        /** Kaydedilemeyen (ör. disk dolu) ama ekranda gösterilen sonuç. */
        const val UNSAVED_ID = "unsaved"
    }
}
