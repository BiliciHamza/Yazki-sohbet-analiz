package com.hamzabilici.yazki.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.ui.ThemeMode
import com.hamzabilici.yazki.ui.components.SectionCard

/** Ayarlar: tema ve tanıtımı yeniden izleme. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        SectionCard(
            title = stringResource(R.string.settings_theme),
            subtitle = stringResource(R.string.settings_theme_hint),
        ) {
            ThemeOption("☀️", R.string.theme_light, ThemeMode.LIGHT, themeMode, onThemeChange)
            ThemeOption("🌙", R.string.theme_dark, ThemeMode.DARK, themeMode, onThemeChange)
        }

        SectionCard(
            title = stringResource(R.string.settings_intro),
            subtitle = stringResource(R.string.settings_intro_hint),
        ) {
            OutlinedButton(onClick = onReplayOnboarding, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_intro_replay))
            }
        }
    }
}

@Composable
private fun ThemeOption(
    emoji: String,
    label: Int,
    mode: ThemeMode,
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val selected = mode == current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton) { onSelect(mode) },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            RadioButton(selected = selected, onClick = { onSelect(mode) })
        }
    }
}
