package com.hamzabilici.yazki.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.ui.AppUiState
import com.hamzabilici.yazki.ui.components.PickFileButton
import com.hamzabilici.yazki.ui.components.SegmentedControl

/** Karne sekmesinin üstündeki alt sekmeler. */
enum class HubTab(@param:StringRes val label: Int) {
    REPORT(R.string.hub_report),
    STATS(R.string.hub_stats),
    SAVED(R.string.hub_saved),
}

/**
 * Alt çubuktaki "Karne" sekmesi. Üstte seçili sohbet ve "Sohbet seç" düğmesi,
 * altında Karne / İstatistik / Kayıtlı geçişi. Kayıtlılardan biri seçilince
 * Karne ve İstatistik o sohbeti gösterir.
 */
@Composable
fun ChatHubScreen(
    state: AppUiState,
    tab: HubTab,
    onTabChange: (HubTab) -> Unit,
    onPickFile: () -> Unit,
    onPlayStory: () -> Unit,
    onSelectSaved: (String) -> Unit,
    onDeleteSaved: (String) -> Unit,
    onEditOptions: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.hub_current_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.result?.let { it.title ?: stringResource(R.string.report_untitled) }
                            ?: stringResource(R.string.hub_no_chat),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                PickFileButton(onClick = onPickFile, compact = true)
            }
            Spacer(Modifier.height(12.dp))
            val labels = HubTab.entries.map { t ->
                if (t == HubTab.SAVED && state.saved.isNotEmpty()) {
                    stringResource(R.string.hub_saved_count, state.saved.size)
                } else {
                    stringResource(t.label)
                }
            }
            SegmentedControl(
                options = labels,
                selected = tab.ordinal,
                onSelect = { onTabChange(HubTab.entries[it]) },
            )
        }

        // Açılışta kayıtlar okunurken boş durumu bir anlığına göstermemek için.
        if (state.restoring && state.result == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            return@Column
        }

        Crossfade(targetState = tab, label = "hub", modifier = Modifier.weight(1f)) { current ->
            when (current) {
                HubTab.REPORT -> ReportScreen(
                    result = state.result,
                    hasSaved = state.saved.isNotEmpty(),
                    onOpenSaved = { onTabChange(HubTab.SAVED) },
                    onPlayStory = onPlayStory,
                    onEditOptions = onEditOptions,
                )
                HubTab.STATS -> StatsScreen(
                    result = state.result,
                    hasSaved = state.saved.isNotEmpty(),
                    onOpenSaved = { onTabChange(HubTab.SAVED) },
                )
                HubTab.SAVED -> SavedChatsScreen(
                    saved = state.saved,
                    selectedId = state.result?.id,
                    onSelect = { id ->
                        onSelectSaved(id)
                        onTabChange(HubTab.REPORT)
                    },
                    onDelete = onDeleteSaved,
                )
            }
        }
    }
}
