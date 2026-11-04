package com.hamzabilici.yazki.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.ui.components.YazkiLogo
import com.hamzabilici.yazki.ui.screens.AboutScreen
import com.hamzabilici.yazki.ui.screens.HomeScreen
import com.hamzabilici.yazki.ui.screens.ChatHubScreen
import com.hamzabilici.yazki.ui.screens.HubTab
import com.hamzabilici.yazki.ui.screens.SettingsScreen
import com.hamzabilici.yazki.ui.onboarding.OnboardingScreen
import com.hamzabilici.yazki.ui.setup.ChatSetupScreen
import com.hamzabilici.yazki.ui.story.StoryScreen

/** Dosya seçicide gösterilecek türler: WhatsApp .txt ve .zip dışa aktarımları. */
private val CHAT_MIME_TYPES = arrayOf("text/plain", "application/zip", "application/x-zip-compressed")

/** Alt gezinme çubuğundaki sekmeler. */
enum class Tab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    HOME(R.string.tab_home, R.drawable.ic_nav_home),
    REPORT(R.string.tab_report, R.drawable.ic_nav_report),
    SETTINGS(R.string.tab_settings, R.drawable.ic_nav_settings),
    ABOUT(R.string.tab_about, R.drawable.ic_nav_about),
}

@Composable
fun YazkiApp(viewModel: AnalysisViewModel, settings: SettingsViewModel, isDark: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val onboardingDone by settings.onboardingDone.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var hubTab by rememberSaveable { mutableStateOf(HubTab.REPORT) }
    var storyOpen by rememberSaveable { mutableStateOf(false) }
    // Ekran dönünce aynı sonuç için tekrar Karne'ye atlamamak için işlenen sürüm saklanır.
    var handledVersion by rememberSaveable { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val pickFile: () -> Unit = { picker.launch(CHAT_MIME_TYPES) }
    val openHub: (HubTab) -> Unit = { t ->
        hubTab = t
        tab = Tab.REPORT
    }

    // Yeni analiz bitince Karne'yi aç; hata olursa Ana Sayfa'ya dön.
    LaunchedEffect(state.resultVersion) {
        if (state.resultVersion > handledVersion) {
            handledVersion = state.resultVersion
            openHub(HubTab.REPORT)
        }
    }
    LaunchedEffect(state.error) {
        if (state.error != null) tab = Tab.HOME
    }

    BackHandler(enabled = tab != Tab.HOME && !storyOpen && state.setup == null && onboardingDone) { tab = Tab.HOME }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = { YazkiBottomBar(selected = tab, onSelect = { tab = it }) },
        ) { padding ->
            Crossfade(targetState = tab, label = "tab", modifier = Modifier.padding(padding)) { current ->
                when (current) {
                    Tab.HOME -> HomeScreen(
                        result = state.result,
                        error = state.error,
                        onPickFile = pickFile,
                        onDismissError = viewModel::dismissError,
                        onOpenReport = { openHub(HubTab.REPORT) },
                        onPlayStory = { storyOpen = true },
                        savedCount = state.saved.size,
                        onOpenSaved = { openHub(HubTab.SAVED) },
                    )
                    Tab.REPORT -> ChatHubScreen(
                        state = state,
                        tab = hubTab,
                        onTabChange = { hubTab = it },
                        onPickFile = pickFile,
                        onPlayStory = { storyOpen = true },
                        onSelectSaved = viewModel::select,
                        onDeleteSaved = viewModel::delete,
                        onEditOptions = viewModel::editOptions,
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        // Seçim yapılmadıysa o an görünen tema işaretli görünür.
                        themeMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                        onThemeChange = settings::setThemeMode,
                        onReplayOnboarding = settings::replayOnboarding,
                    )
                    Tab.ABOUT -> AboutScreen()
                }
            }
        }

        AnimatedVisibility(visible = state.loading, enter = fadeIn(), exit = fadeOut()) {
            LoadingOverlay()
        }

        val result = state.result
        AnimatedVisibility(
            visible = storyOpen && result != null,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            if (result != null) {
                StoryScreen(
                    result = result,
                    onClose = {
                        storyOpen = false
                        openHub(HubTab.REPORT)
                    },
                )
            }
        }

        // Sohbet aktarıldıktan sonra: tür ve analiz ayarları
        val setup = state.setup
        AnimatedVisibility(
            visible = setup != null,
            enter = fadeIn() + slideInVertically { it / 8 },
            exit = fadeOut() + slideOutVertically { it / 8 },
        ) {
            // Kapanış animasyonu sırasında son isteği göstermeye devam et (durum değil, düz tutucu).
            val last = remember { arrayOfNulls<SetupRequest>(1) }
            if (setup != null) last[0] = setup
            last[0]?.let { request ->
                ChatSetupScreen(
                    request = request,
                    onConfirm = viewModel::confirmSetup,
                    onDismiss = viewModel::dismissSetup,
                )
            }
        }

        // İlk açılışta tanıtım; her şeyin üstünde.
        AnimatedVisibility(visible = !onboardingDone, enter = fadeIn(), exit = fadeOut()) {
            OnboardingScreen(onDone = settings::completeOnboarding)
        }
    }
}

@Composable
private fun YazkiBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = t == selected,
                onClick = { onSelect(t) },
                icon = { Icon(painterResource(t.icon), contentDescription = null) },
                label = { Text(stringResource(t.label), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

/** Dosya okunurken tüm ekranı kaplayan, nefes alan logolu bekleme ekranı. */
@Composable
private fun LoadingOverlay() {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            // Arkadaki ekrana dokunulmasın
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            YazkiLogo(96.dp, Modifier.scale(scale))
            Spacer(Modifier.height(28.dp))
            CircularProgressIndicator(strokeWidth = 3.dp)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.loading), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.loading_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
