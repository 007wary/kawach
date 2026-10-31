package dev.wary.kawach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.wary.kawach.ui.Incoming
import dev.wary.kawach.ui.KawachApp
import dev.wary.kawach.ui.theme.KawachTheme

class MainActivity : ComponentActivity() {

    private var incoming by mutableStateOf<Incoming?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
        Intent.ACTION_SEND -> listOfNotNull(
            intent.getStringExtra(Intent.EXTRA_SUBJECT),
            intent.getStringExtra(Intent.EXTRA_TEXT),
        ).joinToString("\n").takeIf { it.isNotBlank() }?.let(Incoming::Shared)
        ACTION_CHECK -> Incoming.OpenCheck
        else -> null
    }

    companion object {
        /** Sent by the home-screen widget to open the check screen directly. */
        const val ACTION_CHECK = "dev.wary.kawach.action.CHECK"
    }
}
