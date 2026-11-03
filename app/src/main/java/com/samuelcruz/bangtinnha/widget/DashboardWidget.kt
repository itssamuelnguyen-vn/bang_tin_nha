package com.samuelcruz.bangtinnha.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.samuelcruz.bangtinnha.BangTinNhaApp
import com.samuelcruz.bangtinnha.MainActivity
import com.samuelcruz.bangtinnha.R
import com.samuelcruz.bangtinnha.core.formatDong
import com.samuelcruz.bangtinnha.core.formatHundredths
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.notify.chosen
import com.samuelcruz.bangtinnha.core.notify.forMyArea
import com.samuelcruz.bangtinnha.gold.millions
import com.samuelcruz.bangtinnha.weather.aqiLevel
import com.samuelcruz.bangtinnha.weather.weatherOf
import kotlinx.coroutines.flow.first

/** Redraws every dashboard widget on the home screen (called after each refresh run). */
suspend fun updateDashboardWidgets(context: Context) = DashboardWidget().updateAll(context)

private data class WidgetData(
    val place: String,
    val temperature: String,
    val sky: String,
    val aqi: String?,
    val lines: List<String>,
)

/** Compact dashboard for the home screen (Glance: Compose-like code that becomes a classic widget). */
class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        val white = ColorProvider(Color.White)
        val soft = ColorProvider(Color.White.copy(alpha = 0.85f))
        provideContent {
            Row(
                modifier = GlanceModifier.fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("📍 ${data.place}", maxLines = 1, style = TextStyle(color = soft, fontSize = 12.sp))
                    Text(data.temperature, style = TextStyle(color = white, fontSize = 36.sp, fontWeight = FontWeight.Bold))
                    Text(data.sky, maxLines = 1, style = TextStyle(color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                    data.aqi?.let { Text(it, maxLines = 1, style = TextStyle(color = soft, fontSize = 12.sp)) }
                }
                Spacer(GlanceModifier.width(10.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    data.lines.forEach { line ->
                        Text(line, maxLines = 1, style = TextStyle(color = white, fontSize = 12.sp), modifier = GlanceModifier.padding(bottom = 3.dp))
                    }
                }
            }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val c = (context.applicationContext as BangTinNhaApp).container
        val s = c.settings.current()
        val now = System.currentTimeMillis()
        val db = c.database
        val w = db.weatherDao().current()
        val lines = buildList {
            db.fxDao().observeLatest().first().firstOrNull { it.code == "USD" }?.sell?.let { add("💵 USD ${formatHundredths(it)}") }
            db.goldDao().latest().firstOrNull { it.code == "SJC" }?.sell?.let { add("🥇 SJC ${millions(it)}") }
            db.fuelDao().latest().chosen(s).firstOrNull()?.let { f -> f.price?.let { add("⛽ ${f.name.removePrefix("Xăng ")} ${formatDong(it)}") } }
            val outage = db.powerDao().upcoming(now).forMyArea(s, now).firstOrNull()
            add(if (outage == null) "⚡ Không cúp điện" else "⚡ Cúp ${formatTime(outage.startAt, "HH:mm dd/MM")}")
        }
        return if (w == null) {
            WidgetData(s.locationName, "--°", "Mở app để tải", null, lines)
        } else {
            val (emoji, text) = weatherOf(w.weatherCode)
            WidgetData(w.locationName, "${w.temperature.toInt()}°", "$emoji $text", w.usAqi?.let { "AQI $it · ${aqiLevel(it).label}" }, lines)
        }
    }
}

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardWidget()
}
