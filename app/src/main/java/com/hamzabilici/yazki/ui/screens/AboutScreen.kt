package com.hamzabilici.yazki.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.ui.components.SectionCard
import com.hamzabilici.yazki.ui.components.YazkiLogo
import com.hamzabilici.yazki.ui.theme.YazkiTheme

/** Gizlilik sözü, ölçümlerin nasıl hesaplandığı ve yasal notlar. */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            YazkiLogo(88.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.about_version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        SectionCard(title = stringResource(R.string.about_privacy_title)) {
            listOf(
                R.string.about_privacy_1,
                R.string.about_privacy_2,
                R.string.about_privacy_3,
                R.string.about_privacy_4,
            ).forEach { CheckItem(it) }
        }

        SectionCard(title = stringResource(R.string.about_metrics_title)) {
            listOf(
                R.string.about_metric_started to R.string.about_metric_started_body,
                R.string.about_metric_day to R.string.about_metric_day_body,
                R.string.about_metric_reply to R.string.about_metric_reply_body,
                R.string.about_metric_double to R.string.about_metric_double_body,
                R.string.about_metric_ghost to R.string.about_metric_ghost_body,
                R.string.about_metric_laughter to R.string.about_metric_laughter_body,
                R.string.about_metric_address to R.string.about_metric_address_body,
            ).forEach { (title, body) ->
                Column {
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionCard(title = stringResource(R.string.about_legal_title)) {
            listOf(R.string.about_legal_1, R.string.about_legal_2, R.string.about_legal_3).forEach {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CheckItem(@StringRes text: Int) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun AboutScreenPreview() {
    YazkiTheme { AboutScreen() }
}
