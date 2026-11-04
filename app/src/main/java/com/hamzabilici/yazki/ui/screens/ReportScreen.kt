package com.hamzabilici.yazki.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.analysis.AnalysisWarning
import com.hamzabilici.yazki.core.analysis.ChatStats
import com.hamzabilici.yazki.data.ImportResult
import com.hamzabilici.yazki.ui.Format
import com.hamzabilici.yazki.ui.Highlight
import com.hamzabilici.yazki.ui.components.ColorDot
import com.hamzabilici.yazki.ui.components.EmptyState
import com.hamzabilici.yazki.ui.components.GradientCard
import com.hamzabilici.yazki.ui.components.SectionCard
import com.hamzabilici.yazki.ui.components.ShareBar
import com.hamzabilici.yazki.ui.components.YazkiLogo
import com.hamzabilici.yazki.ui.emoji
import com.hamzabilici.yazki.ui.highlightsFor
import com.hamzabilici.yazki.ui.title
import com.hamzabilici.yazki.ui.theme.YazkiColors
import com.hamzabilici.yazki.ui.theme.YazkiGradients

/** Sohbet Karnesi: öne çıkanlar, mesaj payı ve hikâye modunun girişi. */
@Composable
fun ReportScreen(
    result: ImportResult?,
    hasSaved: Boolean,
    onOpenSaved: () -> Unit,
    onPlayStory: () -> Unit,
    onEditOptions: () -> Unit,
) {
    if (result == null) {
        EmptyState(
            title = stringResource(R.string.empty_report_title),
            message = stringResource(R.string.empty_report_body),
            secondaryLabel = if (hasSaved) stringResource(R.string.action_open_saved) else null,
            onSecondary = onOpenSaved,
        )
        return
    }
    val stats = result.stats
    val highlights = highlightsFor(stats, result.options)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ReportHeader(result, onPlayStory, onEditOptions) }
        item { WarningsCard(stats) }
        if (stats.participants.size >= 2) item { MessageShareCard(stats) }
        itemsIndexed(highlights) { i, h -> HighlightCard(h, gradient = i) }
        item {
            Text(
                stringResource(R.string.result_entertainment_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ReportHeader(result: ImportResult, onPlayStory: () -> Unit, onEditOptions: () -> Unit) {
    val t = result.stats.time
    GradientCard(YazkiGradients.brandBrush, Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YazkiLogo(28.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.report_label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 1.5.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            result.title ?: stringResource(R.string.report_untitled),
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (t.first != null && t.last != null) {
            Text(
                stringResource(
                    R.string.result_date_range,
                    Format.date(t.first!!.toLocalDate()),
                    Format.date(t.last!!.toLocalDate()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(12.dp))
        // Sohbet türü + analiz ayarları: dokununca tür ve küfür ayarları değiştirilebilir.
        Surface(
            onClick = onEditOptions,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            contentColor = Color.White,
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${result.options.type.emoji}  ${stringResource(result.options.type.title)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    painterResource(R.drawable.ic_tune),
                    contentDescription = stringResource(R.string.action_edit_options),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPlayStory,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = YazkiColors.OffWhite, contentColor = YazkiColors.IndigoDeep),
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_play_story), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun WarningsCard(stats: ChatStats) {
    val messages = stats.warnings.mapNotNull { w ->
        when (w) {
            is AnalysisWarning.NearExportLimit -> stringResource(R.string.warning_export_limit, Format.number(w.messageCount))
            is AnalysisWarning.UnparsedLines ->
                stringResource(R.string.warning_unparsed, Format.number(w.count), Format.percent(w.ratio))
            AnalysisWarning.NoMessages -> null
        }
    }
    if (messages.isEmpty()) return
    SectionCard {
        messages.forEach {
            Row {
                Text("⚠️", modifier = Modifier.padding(end = 10.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MessageShareCard(stats: ChatStats) {
    // Kalabalık gruplarda ilk 6 kişi, gerisi "Diğerleri"
    val sorted = stats.participants.withIndex().sortedByDescending { it.value.messages }
    val top = sorted.take(6)
    val rest = sorted.drop(6).sumOf { it.value.messages }
    val total = stats.totals.messages.coerceAtLeast(1)
    val otherColor = MaterialTheme.colorScheme.outlineVariant

    SectionCard(title = stringResource(R.string.report_share_title)) {
        ShareBar(
            top.map { YazkiColors.person(it.index) to it.value.messages.toFloat() } +
                listOfNotNull(if (rest > 0) otherColor to rest.toFloat() else null),
        )
        Spacer(Modifier.height(2.dp))
        top.forEach { (index, p) ->
            ShareLegendRow(YazkiColors.person(index), p.name, p.messages, total)
        }
        if (rest > 0) ShareLegendRow(otherColor, stringResource(R.string.report_others), rest, total)
    }
}

@Composable
private fun ShareLegendRow(color: Color, name: String, count: Int, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(color)
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            Format.number(count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            Format.percent(count.toDouble() / total),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp),
        )
    }
}

@Composable
fun HighlightCard(h: Highlight, gradient: Int, modifier: Modifier = Modifier) {
    GradientCard(YazkiGradients.brush(gradient), modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    h.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    h.value,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    h.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(h.emoji, fontSize = 44.sp)
        }
    }
}
