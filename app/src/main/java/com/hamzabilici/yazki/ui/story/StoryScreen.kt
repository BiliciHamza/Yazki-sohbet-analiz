@file:OptIn(ExperimentalFoundationApi::class)

package com.hamzabilici.yazki.ui.story

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ImportResult
import com.hamzabilici.yazki.ui.Format
import com.hamzabilici.yazki.ui.Highlight
import com.hamzabilici.yazki.ui.components.YazkiLogo
import com.hamzabilici.yazki.ui.highlightsFor
import com.hamzabilici.yazki.ui.theme.YazkiColors
import com.hamzabilici.yazki.ui.theme.YazkiGradients
import kotlinx.coroutines.launch

private const val PAGE_MILLIS = 5_000

/**
 * Instagram hikâyesi gibi tam ekran sohbet özeti.
 * Ekranın sağına dokun: sonraki, soluna dokun: önceki. Kaydırarak da geçilir.
 */
@Composable
fun StoryScreen(result: ImportResult, onClose: () -> Unit) {
    val highlights = highlightsFor(result.stats, result.options)
    // Sayfa 0: giriş, 1..n: öne çıkanlar, son: kapanış
    val pageCount = highlights.size + 2
    val pager = rememberPagerState(pageCount = { pageCount })
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onClose)

    // Otomatik ilerleme: her sayfa 5 sn; son sayfada durur. Sayfa yerleşince sayaç baştan başlar.
    LaunchedEffect(pager.settledPage) {
        val page = pager.settledPage
        progress.snapTo(0f)
        if (page < pageCount - 1) {
            progress.animateTo(1f, tween(PAGE_MILLIS, easing = LinearEasing))
            // Kaydırma ayrı başlatılır: sayfa değişince bu efekt iptal olur, kaydırma yarıda kalmasın.
            scope.launch { pager.animateScrollToPage(page + 1) }
        } else {
            progress.snapTo(1f)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(YazkiGradients.brush(pager.currentPage)),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(YazkiGradients.brush(page))
                    .pointerInput(page) {
                        detectTapGestures { offset ->
                            scope.launch {
                                if (offset.x < size.width / 3f) {
                                    if (page > 0) pager.animateScrollToPage(page - 1)
                                } else if (page < pageCount - 1) {
                                    pager.animateScrollToPage(page + 1)
                                }
                            }
                        }
                    }
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> IntroPage(result)
                    pageCount - 1 -> OutroPage(
                        result = result,
                        onReplay = { scope.launch { pager.scrollToPage(0) } },
                        onClose = onClose,
                    )
                    else -> HighlightPage(highlights[page - 1])
                }
            }
        }

        // Üstte ilerleme çubukları ve kapat düğmesi
        Column(
            Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(pageCount) { i ->
                    val fill = when {
                        i < pager.settledPage -> 1f
                        i == pager.settledPage -> progress.value
                        else -> 0f
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(Color.White),
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                YazkiLogo(28.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.action_close),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroPage(result: ImportResult) {
    val t = result.stats.time
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        YazkiLogo(96.dp)
        Spacer(Modifier.height(28.dp))
        Text(
            stringResource(R.string.story_intro_label),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            result.title ?: stringResource(R.string.report_untitled),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        if (t.first != null && t.last != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(
                    R.string.result_date_range,
                    Format.date(t.first!!.toLocalDate()),
                    Format.date(t.last!!.toLocalDate()),
                ),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.story_intro_hint),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HighlightPage(h: Highlight) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(h.emoji, fontSize = 88.sp)
        Spacer(Modifier.height(28.dp))
        Text(
            h.title,
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            h.value,
            color = Color.White,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            h.detail,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OutroPage(result: ImportResult, onReplay: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", fontSize = 88.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.story_outro_title),
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(
                R.string.story_outro_body,
                Format.number(result.stats.totals.messages),
                Format.number(result.stats.participants.size),
            ),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onClose,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = YazkiColors.OffWhite, contentColor = YazkiColors.IndigoDeep),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.story_outro_details), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onReplay,
            shape = CircleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(painterResource(R.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.story_replay), style = MaterialTheme.typography.labelLarge)
        }
    }
}
