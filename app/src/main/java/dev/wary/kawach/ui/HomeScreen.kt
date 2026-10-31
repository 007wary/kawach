package dev.wary.kawach.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.wary.kawach.R
import dev.wary.kawach.ui.theme.LocalStatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCheck: () -> Unit,
    onCheckText: (String) -> Unit,
    onHelp: () -> Unit,
    onLearn: () -> Unit,
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION") // LocalClipboard's suspend API needs Compose 1.8 ClipEntry plumbing; this is enough for plain text.
    val clipboard = LocalClipboardManager.current
    var clipboardEmpty by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    ScreenColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.primary,
            )
            Spacer(Modifier.weight(1f))
            AssistChip(
                onClick = { openLanguageSettings(context) },
                label = { Text(stringResource(R.string.language_name) + " ▾") },
            )
        }

        StatusCard()

        Surface(
            onClick = onCheck,
            shape = RoundedCornerShape(28.dp),
            color = scheme.primary,
            contentColor = scheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(3.dp, LocalContentColor.current, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("?", style = MaterialTheme.typography.headlineMedium)
                }
                Text(
                    text = stringResource(R.string.home_check_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.home_check_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }

        OutlinedButton(
            onClick = {
                val text = clipboard.getText()?.text
                if (text.isNullOrBlank()) clipboardEmpty = true else onCheckText(text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.home_paste))
        }
        if (clipboardEmpty) {
            Text(
                text = stringResource(R.string.home_clipboard_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onHelp,
            colors = ButtonDefaults.buttonColors(containerColor = scheme.error, contentColor = scheme.onError),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.home_paid))
        }

        Surface(
            onClick = onLearn,
            shape = RoundedCornerShape(16.dp),
            color = scheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.home_learn_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.home_learn_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusCard() {
    val status = LocalStatusColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(status.ok, CircleShape),
                )
                Text(
                    text = stringResource(R.string.home_status_ok),
                    style = MaterialTheme.typography.titleMedium,
                    color = status.ok,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = stringResource(R.string.home_status_family),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
