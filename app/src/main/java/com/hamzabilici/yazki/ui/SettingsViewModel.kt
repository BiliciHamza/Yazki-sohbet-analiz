package com.hamzabilici.yazki.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Uygulama teması: yalnızca açık ve koyu. Kullanıcı henüz seçmediyse (null) telefonun teması
 * kullanılır; ilk seçimden sonra uygulama o temada kalır.
 */
enum class ThemeMode { LIGHT, DARK }

/**
 * Uygulama geneli tercihler: tema ve tanıtımın görülüp görülmediği.
 *
 * İki küçük değer için SharedPreferences yeterli; ilk karede doğru temayı ve
 * tanıtımı gösterebilmek için açılışta eşzamanlı okunur (tek seferlik, birkaç bayt).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("yazki_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        // Eski sürümden kalan "SYSTEM" değeri tanınmaz ve null'a, yani telefona göre davranışa düşer.
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) },
    )

    /** null: kullanıcı seçmedi, telefonun teması kullanılır. */
    val themeMode: StateFlow<ThemeMode?> = _themeMode.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit { putString(KEY_THEME, mode.name) }
    }

    fun completeOnboarding() {
        _onboardingDone.value = true
        prefs.edit { putBoolean(KEY_ONBOARDING, true) }
    }

    /** Ayarlar'dan "Tanıtımı yeniden göster". */
    fun replayOnboarding() {
        _onboardingDone.value = false
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_ONBOARDING = "onboarding_done"
    }
}
