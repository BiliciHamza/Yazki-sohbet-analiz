package com.hamzabilici.yazki.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ChatOptions
import com.hamzabilici.yazki.data.ChatType
import com.hamzabilici.yazki.ui.SetupRequest
import com.hamzabilici.yazki.ui.emoji
import com.hamzabilici.yazki.ui.subtitle
import com.hamzabilici.yazki.ui.title

/**
 * Sohbet aktarıldıktan sonra açılan iki adımlı kurulum:
 * 1. Sohbet türü (Arkadaş, Arkadaş grubu, Sevgili & Flört, Aile, İş & Okul)
 * 2. Analiz ayarları (küfür sayacı, küfür sansürü)
 *
 * Karne'deki "Analiz ayarları" düğmesiyle aynı ekran düzenleme için de açılır.
 */
@Composable
fun ChatSetupScreen(
    request: SetupRequest,
    onConfirm: (ChatOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by rememberSaveable(request.resultId) { mutableIntStateOf(0) }
    var type by rememberSaveable(request.resultId) { mutableStateOf(request.initial.type) }
    var countProfanity by rememberSaveable(request.resultId) { mutableStateOf(request.initial.countProfanity) }
    var censor by rememberSaveable(request.resultId) { mutableStateOf(request.initial.censor) }
    val suggested = ChatType.suggestFor(request.participants)

    BackHandler { if (step == 1) step = 0 else onDismiss() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            StepIndicator(step)
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it * dir / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it * dir / 4 } + fadeOut())
                },
                label = "setup",
                modifier = Modifier.weight(1f),
            ) { current ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (current == 0) {
                        Header(R.string.setup_type_title, R.string.setup_type_body)
                        ChatType.entries.forEach { t ->
                            TypeCard(
                                type = t,
                                selected = t == type,
                                suggested = t == suggested,
                                onClick = { type = t },
                            )
                        }
                    } else {
                        Header(R.string.setup_options_title, R.string.setup_options_body)
                        ToggleCard(
                            emoji = "🤬",
                            title = stringResource(R.string.setup_count_profanity),
                            body = stringResource(R.string.setup_count_profanity_body),
                            checked = countProfanity,
                            onChange = { countProfanity = it },
                        )
                        ToggleCard(
                            emoji = "🙈",
                            title = stringResource(R.string.setup_censor),
                            body = stringResource(
                                if (censor) R.string.setup_censor_on_body else R.string.setup_censor_off_body,
                            ),
                            checked = censor,
                            onChange = { censor = it },
                        )
                        Text(
                            stringResource(R.string.setup_change_later),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step == 1) {
                    OutlinedButton(onClick = { step = 0 }, shape = CircleShape, modifier = Modifier.height(52.dp)) {
                        Text(stringResource(R.string.action_back))
                    }
                }
                Button(
                    onClick = {
                        if (step == 0) {
                            step = 1
                        } else {
                            onConfirm(ChatOptions(type, countProfanity, censor))
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                ) {
                    Text(
                        stringResource(
                            when {
                                step == 0 -> R.string.action_continue
                                request.isNew -> R.string.setup_show_report
                                else -> R.string.action_save
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(2) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )
        }
    }
}

@Composable
private fun Header(title: Int, body: Int) {
    Column(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TypeCard(type: ChatType, selected: Boolean, suggested: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center,
            ) { Text(type.emoji, fontSize = 28.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(type.title), style = MaterialTheme.typography.titleMedium)
                    if (suggested) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                stringResource(R.string.setup_suggested),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    stringResource(type.subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioDot(selected)
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (selected) 8.dp else 16.dp)
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
        )
    }
}

@Composable
private fun ToggleCard(emoji: String, title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Switch) { onChange(!checked) },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Start)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
