package com.hamzabilici.yazki.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.analysis.MonthCount
import com.hamzabilici.yazki.ui.theme.YazkiColors
import java.time.format.TextStyle

/** 7 × 24 gün-saat ısı haritası. Satır 0 = Pazartesi. */
@Composable
fun Heatmap(data: List<List<Int>>, modifier: Modifier = Modifier) {
    val max = data.maxOf { row -> row.maxOrNull() ?: 0 }.coerceAtLeast(1)
    val days = stringArrayResource(R.array.weekdays_short)
    val color = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        data.forEachIndexed { day, row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    days[day],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(30.dp),
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { value ->
                        val alpha = if (value == 0) 0.07f else 0.18f + 0.82f * value / max
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(color.copy(alpha = alpha), RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
        }
        Row {
            Box(Modifier.width(30.dp))
            listOf("00", "06", "12", "18").forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Aylık mesaj sayısı çubuk grafiği. En yoğun ay mercan renkle vurgulanır. */
@Composable
fun MonthlyChart(months: List<MonthCount>, modifier: Modifier = Modifier) {
    if (months.isEmpty()) return
    // Çok uzun sohbetlerde son 24 ay gösterilir.
    val shown = months.takeLast(24)
    val max = shown.maxOf { it.total }.coerceAtLeast(1)
    val peakIndex = shown.indexOfFirst { it.total == max }
    val base = YazkiColors.Lavender
    val peak = YazkiColors.Coral
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            val gap = 4.dp.toPx()
            val barWidth = ((size.width - gap * (shown.size - 1)) / shown.size).coerceAtLeast(1f)
            shown.forEachIndexed { i, m ->
                val h = (m.total.toFloat() / max) * size.height
                val left = i * (barWidth + gap)
                drawRoundRect(
                    brush = if (i == peakIndex) {
                        Brush.verticalGradient(listOf(peak, peak.copy(alpha = 0.75f)))
                    } else {
                        Brush.verticalGradient(listOf(base, base.copy(alpha = 0.55f)))
                    },
                    topLeft = Offset(left, size.height - h),
                    size = Size(barWidth, h.coerceAtLeast(2f)),
                    cornerRadius = CornerRadius(barWidth.coerceAtMost(12f) / 2),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            val locale = com.hamzabilici.yazki.core.turkish.TurkishText.LOCALE
            fun label(m: MonthCount) =
                m.month.month.getDisplayName(TextStyle.SHORT, locale) + " " + (m.month.year % 100).toString().padStart(2, '0')
            Text(
                label(shown.first()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (shown.size > 1) {
                Text(
                    label(shown.last()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
