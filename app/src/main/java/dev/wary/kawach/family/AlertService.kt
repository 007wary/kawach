package dev.wary.kawach.family

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.wary.kawach.MainActivity
import dev.wary.kawach.R
import dev.wary.kawach.rules.Risk
import dev.wary.kawach.rules.ScamType
import dev.wary.kawach.ui.labelRes
import dev.wary.kawach.ui.titleRes

/**
 * Receives family alerts. The API sends data-only messages, so the text is built here
 * in the phone's own language and a tap opens the check in Kawach.
 */
class AlertService : FirebaseMessagingService() {

    override fun onNewToken(token: String) = Family.onNewToken(token)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val checkId = data["check_id"] ?: return
        val (title, text) = when (data["type"]) {
            "check" -> {
                val risk = Risk.entries.firstOrNull { it.name == data["verdict"] } ?: Risk.MEDIUM
                val scam = ScamType.entries.firstOrNull { it.name == data["scam_type"] }
                getString(R.string.notify_check_title, data["elder_name"].orEmpty()) to
                    getString(risk.labelRes()) + (scam?.let { " · " + getString(it.titleRes()) } ?: "")
            }
            "reply" -> getString(R.string.notify_reply_title, data["guardian_name"].orEmpty()) to
                getString(if (data["reply"] == "safe") R.string.reply_safe_elder else R.string.reply_scam_elder)
            else -> return
        }
        show(this, checkId, title, text)
    }

    companion object {
        private const val CHANNEL = "family_alerts"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL,
                context.getString(R.string.notify_channel),
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun show(context: Context, checkId: String, title: String, text: String) {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return
            createChannel(context)
            val open = Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_CHECK)
                .putExtra(MainActivity.EXTRA_CHECK_ID, checkId)
            val pending = PendingIntent.getActivity(
                context,
                checkId.hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try {
                manager.notify(checkId.hashCode(), notification)
            } catch (_: SecurityException) {
                // Notification permission was revoked between the check above and now.
            }
        }
    }
}
