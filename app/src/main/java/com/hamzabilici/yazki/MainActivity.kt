package com.hamzabilici.yazki

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzabilici.yazki.data.SharedChatIntent
import com.hamzabilici.yazki.ui.AnalysisViewModel
import com.hamzabilici.yazki.ui.SettingsViewModel
import com.hamzabilici.yazki.ui.ThemeMode
import com.hamzabilici.yazki.ui.YazkiApp
import com.hamzabilici.yazki.ui.theme.YazkiTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AnalysisViewModel by viewModels()
    private val settings: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Ekran döndürmede aynı dosya tekrar analiz edilmesin.
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val mode by settings.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (mode) {
                null -> systemDark // ilk açılış: telefonun temasına uy
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Durum ve gezinme çubuğu simgeleri seçilen temaya uysun (telefon temasından farklı olsa bile).
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }

            YazkiTheme(darkTheme = dark) {
                YazkiApp(viewModel, settings, isDark = dark)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action !in SharedChatIntent.HANDLED_ACTIONS) return
        val uri = SharedChatIntent.chatUri(intent, contentResolver)
        if (uri != null) {
            viewModel.import(uri)
        } else {
            viewModel.showError(R.string.error_no_file)
        }
    }

    private companion object {
        // Eski Android sürümlerinde (3 tuşlu gezinme) simgelerin okunması için yarı saydam zemin.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
