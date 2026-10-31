package dev.wary.kawach.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import dev.wary.kawach.MainActivity

/** Opens the phone's dialler with the number filled in. The person still taps Call. */
fun dial(context: Context, number: String) {
    context.startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}

fun openUrl(context: Context, url: String) {
    context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

/** Free family alert for now: the share sheet, so the elder can pick WhatsApp and a family member. */
fun shareWithFamily(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, null).apply {
        // Kawach accepts shared text too; keep it out of its own share sheet.
        putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, MainActivity::class.java)))
    }
    context.startSafely(chooser)
}

/** Android 13+ has a per-app language screen; older phones get the system language screen. */
fun openLanguageSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
    } else {
        Intent(Settings.ACTION_LOCALE_SETTINGS)
    }
    context.startSafely(intent)
}

private fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent) }
}
