package dev.wary.kawach

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import dev.wary.kawach.family.AlertService
import dev.wary.kawach.family.Family
import dev.wary.kawach.ui.Incoming
import dev.wary.kawach.ui.KawachApp
import dev.wary.kawach.ui.theme.KawachTheme

class MainActivity : ComponentActivity() {

    private var incoming by mutableStateOf<Incoming?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AlertService.createChannel(this)
        Family.syncToken()
        if (savedInstanceState == null) incoming = parse(intent)
        setContent {
            KawachTheme {
                KawachApp(incoming = incoming, onIncomingHandled = { incoming = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incoming = parse(intent)
    }

    private fun parse(intent: Intent?): Incoming? = when (intent?.action) {
        Intent.ACTION_SEND -> {
            val text = listOfNotNull(
                intent.getStringExtra(Intent.EXTRA_SUBJECT),
                intent.getStringExtra(Intent.EXTRA_TEXT),
            ).joinToString("\n").takeIf { it.isNotBlank() }
            val image = if (intent.type?.startsWith("image/") == true) {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                null
            }
            // A WhatsApp picture can come with a caption: read both.
            image?.let { Incoming.SharedImage(it, text) } ?: text?.let(Incoming::Shared)
        }
        ACTION_CHECK -> Incoming.OpenCheck
        ACTION_OPEN_CHECK -> intent.getStringExtra(EXTRA_CHECK_ID)?.let(Incoming::OpenAlert)
        else -> null
    }

    companion object {
        /** Sent by the home-screen widget to open the check screen directly. */
        const val ACTION_CHECK = "dev.wary.kawach.action.CHECK"

        /** Sent by a family-alert notification to open that check. */
        const val ACTION_OPEN_CHECK = "dev.wary.kawach.action.OPEN_CHECK"
        const val EXTRA_CHECK_ID = "check_id"
    }
}
