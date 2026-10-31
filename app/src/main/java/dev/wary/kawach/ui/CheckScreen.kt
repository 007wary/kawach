package dev.wary.kawach.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.wary.kawach.R

@Composable
fun CheckScreen(onBack: () -> Unit, onSubmit: (String) -> Unit, onPicture: (Uri) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    // Android's photo picker: no storage permission, and Kawach only sees the one picture chosen.
    val pickPicture = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicture)
    }
    // Some phones say they have the photo picker but can't open it; the older file chooser works everywhere.
    val choosePicture = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(onPicture)
    }

    ScreenColumn(Modifier.imePadding()) {
        ScreenHeader(stringResource(R.string.check_title), onBack)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.check_hint)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { clipboard.getText()?.text?.let { text = it } },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.check_paste))
            }
            Button(
                onClick = { onSubmit(text.trim()) },
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.check_button))
            }
        }

        OutlinedButton(
            onClick = {
                try {
                    pickPicture.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } catch (_: ActivityNotFoundException) {
                    choosePicture.launch("image/*")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TouchHeight),
        ) {
            Text(stringResource(R.string.check_picture))
        }

        Text(
            text = stringResource(R.string.check_tip),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
