package dev.wary.kawach.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.wary.kawach.MainActivity
import dev.wary.kawach.R

private val Surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF151F29))
private val Teal = ColorProvider(day = Color(0xFF0E6B67), night = Color(0xFF45BDB4))
private val OnTeal = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF00201E))
private val DangerSoft = ColorProvider(day = Color(0xFFFBE4E1), night = Color(0xFF3B1915))
private val Danger = ColorProvider(day = Color(0xFFC23628), night = Color(0xFFFF7A6B))

/** Home-screen widget: one big "Check a message" button and a 1930 helpline shortcut. */
class CheckWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val openCheck = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_CHECK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val dialHelpline = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1930"))
        val title = context.getString(R.string.app_name)
        val checkLabel = context.getString(R.string.widget_check)
        val helpLabel = context.getString(R.string.widget_help)

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(Surface)
                    .cornerRadius(20.dp)
                    .padding(12.dp),
            ) {
                Text(title, style = TextStyle(color = Teal, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Spacer(GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .background(Teal)
                        .cornerRadius(16.dp)
                        .clickable(actionStartActivity(openCheck)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(checkLabel, style = TextStyle(color = OnTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp))
                }
                Spacer(GlanceModifier.height(8.dp))
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(DangerSoft)
                        .cornerRadius(12.dp)
                        .clickable(actionStartActivity(dialHelpline)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(helpLabel, style = TextStyle(color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp))
                }
            }
        }
    }
}

class CheckWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CheckWidget()
}
