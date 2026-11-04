@file:OptIn(ExperimentalFoundationApi::class)

package com.hamzabilici.yazki.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ChatType
import com.hamzabilici.yazki.ui.components.YazkiLogo
import com.hamzabilici.yazki.ui.emoji
import com.hamzabilici.yazki.ui.theme.YazkiColors
import com.hamzabilici.yazki.ui.theme.YazkiGradients
import com.hamzabilici.yazki.ui.title
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

/** İlk açılıştaki kısa tanıtım: 4 sayfa, kaydırarak veya "İleri" ile geçilir. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGE_COUNT - 1

    BackHandler(enabled = pager.currentPage > 0) {
        scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(YazkiGradients.brush(listOf(0, 5, 4, 2)[pager.currentPage])),
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(YazkiGradients.brush(listOf(0, 5, 4, 2)[page]))
                    .systemBarsPadding()
                    .padding(start = 28.dp, end = 28.dp, top = 56.dp, bottom = 140.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> WelcomePage()
                    1 -> ExportPage()
                    2 -> TypesPage()
                    else -> ExplorePage()
                }
            }
        }

        // Üstte "Atla"
        if (!last) {
            TextButton(
                onClick = onDone,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .systemBarsPadding()
                    .padding(8.dp),
            ) { Text(stringResource(R.string.onboarding_skip), color = Color.White) }
        }

        // Altta sayfa noktaları ve İleri / Başla
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(PAGE_COUNT) { i ->
                    val selected = i == pager.currentPage
                    val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot")
                    val color by animateColorAsState(
                        if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                        label = "dotColor",
                    )
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = YazkiColors.OffWhite, contentColor = YazkiColors.IndigoDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                Text(
                    stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun PageText(title: Int, body: Int) {
    Text(
        stringResource(title),
        color = Color.White,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(body),
        color = Color.White.copy(alpha = 0.9f),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        YazkiLogo(120.dp)
        Spacer(Modifier.height(32.dp))
        PageText(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body)
    }
}

@Composable
private fun ExportPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📤", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        PageText(R.string.onboarding_export_title, R.string.onboarding_export_body)
        Spacer(Modifier.height(20.dp))
        // WhatsApp'taki yol, adım adım
        listOf(
            R.string.onboarding_export_step1,
            R.string.onboarding_export_step2,
            R.string.onboarding_export_step3,
        ).forEachIndexed { i, step ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Text((i + 1).toString(), color = YazkiColors.IndigoDeep, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(step), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TypesPage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎯", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        PageText(R.string.onboarding_types_title, R.string.onboarding_types_body)
        Spacer(Modifier.height(20.dp))
        ChatType.entries.forEach { t ->
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Text(
                    "${t.emoji}  ${stringResource(t.title)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ExplorePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎓", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        PageText(R.string.onboarding_explore_title, R.string.onboarding_explore_body)
    }
}
