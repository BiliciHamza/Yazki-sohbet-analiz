package com.hamzabilici.yazki.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.turkish.TurkishText
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Türkçe sayı, yüzde ve tarih biçimleri: 12.345 · %42 · 2 Kasım 2026. */
object Format {
    private val locale = TurkishText.LOCALE
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", locale)

    // NumberFormat iş parçacığı güvenli değil ve oluşturması pahalı; her iş parçacığına bir tane.
    // (ThreadLocal.withInitial API 26+ olduğu için minSdk 24'te alt sınıf kullanılır.)
    private fun cached(create: () -> NumberFormat) = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat = create()
    }

    private val integer = cached { NumberFormat.getIntegerInstance(locale) }
    private val oneDecimal = cached {
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 }
    }
    private val percent = cached { NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 } }

    fun number(value: Int): String = integer.get()!!.format(value)

    fun number(value: Long): String = integer.get()!!.format(value)

    fun decimal(value: Double): String = oneDecimal.get()!!.format(value)

    fun percent(ratio: Double): String = percent.get()!!.format(ratio)

    fun date(value: LocalDate): String = value.format(dateFormatter)

    fun shortDate(value: LocalDate): String = value.format(shortDateFormatter)

    fun shortDate(epochMillis: Long): String =
        shortDate(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
}

/**
 * Süreyi okunur hale getirir.
 *
 * @param minutePrecision Zaman damgaları yalnızca dakika içeriyorsa (Android dışa aktarımı)
 *   aynı dakikadaki cevaplar 0 sn ölçülür. Bu durumda 1 dakikanın altı "1 dk içinde" yazılır;
 *   saniye gösterilmez çünkü dosyada saniye bilgisi yoktur.
 */
@Composable
fun formatDuration(duration: Duration, minutePrecision: Boolean = false): String {
    val seconds = duration.seconds
    return when {
        seconds < 60 && minutePrecision -> stringResource(R.string.duration_within_minute)
        seconds < 60 -> stringResource(R.string.duration_seconds, seconds.toInt())
        seconds < 3_600 -> stringResource(R.string.duration_minutes, (seconds / 60).toInt())
        seconds < 86_400 -> stringResource(
            R.string.duration_hours,
            (seconds / 3_600).toInt(),
            ((seconds % 3_600) / 60).toInt(),
        )
        else -> stringResource(
            R.string.duration_days,
            (seconds / 86_400).toInt(),
            ((seconds % 86_400) / 3_600).toInt(),
        )
    }
}
