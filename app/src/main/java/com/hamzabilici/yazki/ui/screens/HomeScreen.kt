package com.hamzabilici.yazki.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.hamzabilici.yazki.ui.components.YazkiLogo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ImportResult
import com.hamzabilici.yazki.ui.Format
import com.hamzabilici.yazki.ui.components.GradientCard
import com.hamzabilici.yazki.ui.components.PersonAvatar
import com.hamzabilici.yazki.ui.components.PickFileButton
import com.hamzabilici.yazki.ui.components.SectionCard
import com.hamzabilici.yazki.ui.theme.YazkiGradients
import com.hamzabilici.yazki.ui.theme.YazkiTheme

@Composable
fun HomeScreen(
    result: ImportResult?,
    @StringRes error: Int?,
    onPickFile: () -> Unit,
    onDismissError: () -> Unit,
    onOpenReport: () -> Unit,
    onPlayStory: () -> Unit,
    savedCount: Int = 0,
    onOpenSaved: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader()

        HeroCard(onPickFile)

        AnimatedVisibility(visible = error != null) {
            if (error != null) ErrorCard(error, onDismissError)
        }

        if (result != null) LastAnalysisCard(result, savedCount, onOpenReport, onPlayStory, onOpenSaved)

        FeatureRow()

        ExportGuide()
    }
}

@Composable
private fun HeroCard(onPickFile: () -> Unit) {
    GradientCard(YazkiGradients.brandBrush, Modifier.fillMaxWidth(), contentPadding = 24.dp) {
        Text(
            stringResource(R.string.home_hero_title),
            style = MaterialTheme.typography.headlineLarge,
            lineHeight = 38.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.88f),
        )
        Spacer(Modifier.height(20.dp))
        PickFileButton(onClick = onPickFile, onGradient = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.home_privacy),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ErrorCard(@StringRes error: Int, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Text(stringResource(R.string.error_title), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(error), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_dismiss))
            }
        }
    }
}

@Composable
private fun LastAnalysisCard(
    result: ImportResult,
    savedCount: Int,
    onOpenReport: () -> Unit,
    onPlayStory: () -> Unit,
    onOpenSaved: () -> Unit,
) {
    val stats = result.stats
    SectionCard(title = stringResource(R.string.home_last_analysis)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Katılımcı avatarları üst üste
            Box {
                stats.participants.take(3).forEachIndexed { i, p ->
                    Box(Modifier.padding(start = (i * 22).dp)) { PersonAvatar(p.name, i, 36.dp) }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.title ?: stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(
                        R.string.home_last_analysis_detail,
                        Format.number(stats.totals.messages),
                        Format.number(stats.participants.size),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onOpenReport, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_open_report))
            }
            FilledTonalButton(onClick = onPlayStory, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_play_story))
            }
        }
        if (savedCount > 1) {
            TextButton(onClick = onOpenSaved, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.home_saved_link, savedCount))
            }
        }
    }
}

/** Sayfanın üstünde ortalanmış logo ve uygulama adı. */
@Composable
private fun BrandHeader() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YazkiLogo(64.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.home_brand_subtitle),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureRow() {
    // Eşit yükseklikte üç kutu; alt yazısı olan ve olmayanlar aynı hizada dursun.
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FeatureTile("🇹🇷", R.string.feature_turkish_title, null, Modifier.weight(1f))
        FeatureTile("🔒", R.string.feature_private_title, R.string.feature_private_body, Modifier.weight(1f))
        FeatureTile("📊", R.string.feature_report_title, null, Modifier.weight(1f))
    }
}

@Composable
private fun FeatureTile(emoji: String, @StringRes title: Int, @StringRes body: Int?, modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Üstten hizalı: alt yazısı olan kutuda da ikonlar aynı seviyede kalır.
            verticalArrangement = Arrangement.Top,
        ) {
            // İkon, marka renginde yumuşak bir dairenin ortasında
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) { Text(emoji, fontSize = 26.sp) }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            if (body != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Android / iPhone sekmeli, numaralı dışa aktarma adımları. */
@Composable
private fun ExportGuide() {
    var platform by rememberSaveable { mutableIntStateOf(0) }
    val steps = stringArrayResource(if (platform == 0) R.array.guide_share_steps else R.array.guide_file_steps)
    SectionCard(
        title = stringResource(R.string.home_guide_title),
        modifier = Modifier.animateContentSize(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(4.dp),
        ) {
            listOf(R.string.guide_tab_share, R.string.guide_tab_file).forEachIndexed { i, label ->
                val selected = platform == i
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { platform = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(label),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (i + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun HomeScreenPreview() {
    YazkiTheme {
        HomeScreen(
            result = null,
            error = null,
            onPickFile = {},
            onDismissError = {},
            onOpenReport = {},
            onPlayStory = {},
        )
    }
}
