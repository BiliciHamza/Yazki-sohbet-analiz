package com.hamzabilici.yazki.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamzabilici.yazki.R

// ---------------------------------------------------------------- renkler
// Logo ile aynı mor-indigo ve mercan. WhatsApp yeşilinden bilerek uzak (Play taklit politikası).

object YazkiColors {
    val Indigo = Color(0xFF5B4BDB)
    val IndigoLight = Color(0xFF7A6AF5)
    val IndigoDeep = Color(0xFF4330C4)
    val Lavender = Color(0xFF8E80F7)
    val Coral = Color(0xFFFF6F61)

    /** Degrade kartlar üzerindeki düğmeler: saf beyaz yerine yumuşak kırık beyaz. */
    val OffWhite = Color(0xFFF6F6F7)

    /** Kişileri grafiklerde ayırt etmek için sıra sabit renkler. */
    val People = listOf(
        Color(0xFF5B4BDB), // indigo
        Color(0xFFFF6F61), // mercan
        Color(0xFF14B8A6), // turkuaz
        Color(0xFFF59E0B), // kehribar
        Color(0xFFEC4899), // pembe
        Color(0xFF0EA5E9), // gök mavisi
        Color(0xFF84CC16), // yeşil-sarı
        Color(0xFFA855F7), // mor
    )

    fun person(index: Int): Color = People[index.mod(People.size)]
}

/** Karne kartları ve hikâye sayfaları için degradeler. Üzerindeki yazı her zaman beyaz. */
object YazkiGradients {
    val Brand = listOf(Color(0xFF7A6AF5), Color(0xFF4330C4))
    private val all = listOf(
        Brand,
        listOf(Color(0xFFFF8A65), Color(0xFFE8456B)), // mercan → gül
        listOf(Color(0xFF2DD4BF), Color(0xFF0E7490)), // turkuaz → petrol
        listOf(Color(0xFFF59E0B), Color(0xFFDC4A0C)), // kehribar → turuncu
        listOf(Color(0xFFF472B6), Color(0xFF9D27B0)), // pembe → mor
        listOf(Color(0xFF38BDF8), Color(0xFF4F46E5)), // gök → çivit
        listOf(Color(0xFF34D399), Color(0xFF047857)), // nane → orman
    )

    fun colors(index: Int): List<Color> = all[index.mod(all.size)]

    fun brush(index: Int): Brush = brushes[index.mod(brushes.size)]

    // Fırçalar bir kez oluşturulur; her çizimde yeni nesne yaratılmaz.
    private val brushes: List<Brush> = all.map { Brush.linearGradient(it) }

    val brandBrush: Brush = Brush.linearGradient(Brand)
}

/*
 * Yüzey tonları bilerek saf beyaz ve saf siyah değil: tam beyaz ve tam siyah göz yorar.
 *
 * Açık tema: zemin #EEEEF1, kartlar #F6F6F7 (≈ #F5F5F5), iç kutular biraz daha koyu.
 * Koyu tema: zemin #121212, kartlar zeminden bir kademe açık (#1C1C1C), iç kutular bir kademe daha.
 * Gri tonlar nötr; renk yalnızca marka mor-indigosu ve mercandan gelir.
 */
private val LightColors = lightColorScheme(
    primary = YazkiColors.Indigo,
    onPrimary = Color(0xFFF6F6F7),
    primaryContainer = Color(0xFFE2DEF8),
    onPrimaryContainer = Color(0xFF23187A),
    secondary = Color(0xFFE5574A),
    onSecondary = Color(0xFFF6F6F7),
    secondaryContainer = Color(0xFFFBE3DF),
    onSecondaryContainer = Color(0xFF6A1B12),
    tertiary = Color(0xFF0E8C7E),
    background = Color(0xFFEEEEF1), // res/values/colors.xml window_background ile aynı
    onBackground = Color(0xFF1C1C21),
    surface = Color(0xFFEEEEF1),
    onSurface = Color(0xFF1C1C21),
    surfaceVariant = Color(0xFFE4E4E9),
    onSurfaceVariant = Color(0xFF5F5F6A),
    surfaceContainerLowest = Color(0xFFF6F6F7), // kartlar, alt çubuk
    surfaceContainerLow = Color(0xFFE8E8EC),
    surfaceContainer = Color(0xFFE2E2E7),
    surfaceContainerHigh = Color(0xFFDADAE0),
    surfaceContainerHighest = Color(0xFFD3D3DA),
    outline = Color(0xFF8E8E98),
    outlineVariant = Color(0xFFDDDDE2),
    error = Color(0xFFC62E2E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3A8FF),
    onPrimary = Color(0xFF1E1460),
    primaryContainer = Color(0xFF2E2860),
    onPrimaryContainer = Color(0xFFE2DCFF),
    secondary = Color(0xFFFF9C90),
    onSecondary = Color(0xFF4A1510),
    secondaryContainer = Color(0xFF4A2320),
    onSecondaryContainer = Color(0xFFFFD9D3),
    tertiary = Color(0xFF4FD1C0),
    background = Color(0xFF121212), // res/values-night window_background_dark ile aynı
    onBackground = Color(0xFFE8E8EA),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE8E8EA),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFA5A5AD),
    surfaceContainerLowest = Color(0xFF1C1C1C), // kartlar, alt çubuk
    surfaceContainerLow = Color(0xFF222222),
    surfaceContainer = Color(0xFF282828),
    surfaceContainerHigh = Color(0xFF303030),
    surfaceContainerHighest = Color(0xFF383838),
    outline = Color(0xFF6E6E76),
    outlineVariant = Color(0xFF2C2C2C),
    error = Color(0xFFFF8A80),
)

// ---------------------------------------------------------------- yazı tipi
// Plus Jakarta Sans (SIL OFL 1.1). Türkçe karakterlerin tamamı var. Lisans: assets/licenses.

val JakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val YazkiTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold),
        displayMedium = base.displayMedium.copy(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold),
        displaySmall = base.displaySmall.copy(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold),
        headlineMedium = base.headlineMedium.copy(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontFamily = JakartaSans, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontFamily = JakartaSans, fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontFamily = JakartaSans, fontWeight = FontWeight.Bold),
        titleSmall = base.titleSmall.copy(fontFamily = JakartaSans, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontFamily = JakartaSans),
        bodyMedium = base.bodyMedium.copy(fontFamily = JakartaSans),
        bodySmall = base.bodySmall.copy(fontFamily = JakartaSans),
        labelLarge = base.labelLarge.copy(fontFamily = JakartaSans, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = JakartaSans, fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontFamily = JakartaSans, fontWeight = FontWeight.Medium),
    )
}

private val YazkiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun YazkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = YazkiTypography,
        shapes = YazkiShapes,
        content = content,
    )
}
