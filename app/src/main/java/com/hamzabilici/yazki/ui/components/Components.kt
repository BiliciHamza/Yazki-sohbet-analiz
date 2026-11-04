package com.hamzabilici.yazki.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.core.turkish.TurkishText
import com.hamzabilici.yazki.ui.theme.YazkiColors

// ---------------------------------------------------------------- marka

@Composable
fun YazkiLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, logoSize: Dp = 36.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        YazkiLogo(logoSize)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

// ---------------------------------------------------------------- kartlar

/** Degrade zeminli kart. İçindeki yazı ve ikonlar beyazdır. */
@Composable
fun GradientCard(
    brush: Brush,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.large)
            .background(brush),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Sade, kenarlıklı içerik kartı. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (title != null) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

/** Büyük sayı + küçük etiket; üstte renkli bir çizgi vurgusu. */
@Composable
fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                Modifier
                    .size(width = 28.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------- kişiler

/** İsmin ilk harfiyle renkli yuvarlak. Emoji veya numarayla başlayan adlarda da çalışır. */
@Composable
fun PersonAvatar(name: String, index: Int, size: Dp = 40.dp) {
    val initial = name.trim().removePrefix("+").firstOrNull { it.isLetterOrDigit() }
        ?.toString()?.uppercase(TurkishText.LOCALE) ?: "?"
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(YazkiColors.person(index)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initial,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** Yatay paylaşım çubuğu: her kişi kendi renginde, payı oranında. */
@Composable
fun ShareBar(parts: List<Pair<Color, Float>>, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val total = parts.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        parts.filter { it.second > 0f }.forEach { (color, value) ->
            Box(
                Modifier
                    .weight(value / total)
                    .fillMaxHeight()
                    .background(color),
            )
        }
    }
}

@Composable
fun ColorDot(color: Color, size: Dp = 10.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

// ---------------------------------------------------------------- küçük parçalar

/** Yuvarlak zeminli ikon. */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/**
 * Boş durum: logo, başlık ve açıklama.
 * [onPickFile] verilirse "Sohbet dosyası seç" düğmesi çıkar; Karne sekmesinde verilmez çünkü
 * aynı düğme sağ üstte zaten var. [secondaryLabel] verilirse altında bir yazı düğmesi çıkar.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    onPickFile: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YazkiLogo(88.dp)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onPickFile != null) {
            Spacer(Modifier.height(24.dp))
            PickFileButton(onClick = onPickFile)
        }
        if (secondaryLabel != null) {
            Spacer(Modifier.height(if (onPickFile != null) 8.dp else 20.dp))
            TextButton(onClick = onSecondary) { Text(secondaryLabel, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** Hap şeklinde sekme seçici: seçili parça ana renkte dolu. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PickFileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGradient: Boolean = false,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (compact) 40.dp else 54.dp),
        shape = CircleShape,
        contentPadding = if (compact) PaddingValues(horizontal = 14.dp) else ButtonDefaults.ContentPadding,
        colors = if (onGradient) {
            ButtonDefaults.buttonColors(containerColor = YazkiColors.OffWhite, contentColor = YazkiColors.IndigoDeep)
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Icon(
            painterResource(R.drawable.ic_upload),
            contentDescription = null,
            modifier = Modifier.size(if (compact) 18.dp else 20.dp),
        )
        Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
        Text(
            stringResource(if (compact) R.string.action_pick_short else R.string.home_pick_file),
            style = MaterialTheme.typography.labelLarge,
            fontSize = if (compact) 14.sp else 16.sp,
        )
    }
}
