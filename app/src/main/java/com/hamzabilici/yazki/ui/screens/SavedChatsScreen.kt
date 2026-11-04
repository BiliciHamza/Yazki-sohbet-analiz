package com.hamzabilici.yazki.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.SavedChatInfo
import com.hamzabilici.yazki.ui.Format
import com.hamzabilici.yazki.ui.components.EmptyState
import com.hamzabilici.yazki.ui.components.PersonAvatar

/**
 * Daha önce aktarılan sohbetler. Birine dokununca Karne ve İstatistik o sohbeti gösterir.
 * Listede yalnızca hesaplanmış özetler vardır; sohbet metni saklanmaz.
 */
@Composable
fun SavedChatsScreen(
    saved: List<SavedChatInfo>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (saved.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.saved_empty_title),
            message = stringResource(R.string.saved_empty_body),
        )
        return
    }

    // Silme onayı bekleyen kaydın kimliği; ekran dönünce de korunur.
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                stringResource(R.string.saved_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(saved, key = { it.id }) { info ->
            SavedChatRow(
                info = info,
                selected = info.id == selectedId,
                onClick = { onSelect(info.id) },
                onDelete = { pendingDelete = info.id },
            )
        }
    }

    val target = pendingDelete?.let { id -> saved.firstOrNull { it.id == id } }
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.saved_delete_title)) },
            text = {
                Text(stringResource(R.string.saved_delete_body, target.title ?: stringResource(R.string.report_untitled)))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SavedChatRow(
    info: SavedChatInfo,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            // Katılımcı avatarları üst üste
            Box(Modifier.width((36 + 18 * (info.participants.size.coerceAtMost(3) - 1).coerceAtLeast(0)).dp)) {
                info.participants.take(3).forEachIndexed { i, name ->
                    Box(Modifier.padding(start = (i * 18).dp)) { PersonAvatar(name, i, 36.dp) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        info.title ?: stringResource(R.string.report_untitled),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (selected) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                            Text(
                                stringResource(R.string.saved_selected),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    stringResource(
                        R.string.home_last_analysis_detail,
                        Format.number(info.messages),
                        Format.number(info.participants.size),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.firstDay != null && info.lastDay != null) {
                    Text(
                        stringResource(
                            R.string.result_date_range,
                            Format.shortDate(info.firstDay),
                            Format.shortDate(info.lastDay),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.saved_imported_at, Format.shortDate(info.importedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
