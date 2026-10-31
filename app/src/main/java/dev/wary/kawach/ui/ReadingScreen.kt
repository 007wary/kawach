package dev.wary.kawach.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.wary.kawach.R
import dev.wary.kawach.ocr.PictureText
import kotlinx.coroutines.CancellationException

/** Reads the words in a picture, then hands them to the scam check. [caption] is any text shared with it. */
@Composable
fun ReadingScreen(uri: Uri, caption: String?, onBack: () -> Unit, onRead: (String) -> Unit) {
    val context = LocalContext.current
    var error by remember(uri) { mutableStateOf<Int?>(null) }

    LaunchedEffect(uri) {
        try {
            val words = PictureText.read(context, uri)
            onRead(listOfNotNull(caption?.takeIf { it.isNotBlank() }, words).joinToString("\n"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = when (e) {
                is PictureText.NoWords -> R.string.reading_no_words
                is PictureText.NotReady -> R.string.reading_not_ready
                else -> R.string.reading_failed
            }
        }
    }

    ScreenColumn {
        ScreenHeader(stringResource(R.string.reading_title), onBack)
        val message = error
        if (message == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            Text(stringResource(R.string.reading_wait), style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TouchHeight),
            ) {
                Text(stringResource(R.string.reading_back))
            }
        }
    }
}
