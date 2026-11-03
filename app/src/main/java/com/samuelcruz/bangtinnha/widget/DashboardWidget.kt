package com.samuelcruz.bangtinnha.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.samuelcruz.bangtinnha.BangTinNhaApp
import com.samuelcruz.bangtinnha.MainActivity
import com.samuelcruz.bangtinnha.core.formatHundredths
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.notify.changeLine
import com.samuelcruz.bangtinnha.core.notify.chosen
import com.samuelcruz.bangtinnha.core.notify.forMyArea
import com.samuelcruz.bangtinnha.weather.aqiLevel
import com.samuelcruz.bangtinnha.weather.weatherOf
import kotlinx.coroutines.flow.first

/** Redraws every dashboard widget on the home screen (called after each refresh run). */
suspend fun updateDashboardWidgets(context: Context) = DashboardWidget().updateAll(context)

/** Compact dashboard for the home screen (Glance: Compose-like code that becomes a classic widget). */
class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val lines = loadLines(context)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(20.dp)
                        .padding(12.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Text(
                        "Bảng Tin Nhà",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlanceTheme.colors.primary),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    lines.forEach { line ->
                        Text(line, maxLines = 1, style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurface))
                    }
                }
            }
        }
    }

    private suspend fun loadLines(context: Context): List<String> {
        val c = (context.applicationContext as BangTinNhaApp).container
        val s = c.settings.current()
        val now = System.currentTimeMillis()
        val db = c.database
        return buildList {
            db.weatherDao().current()?.let { w ->
                val (emoji, text) = weatherOf(w.weatherCode)
                add("$emoji ${w.temperature.toInt()}° $text" + (w.usAqi?.let { " · AQI $it ${aqiLevel(it).label}" } ?: ""))
            }
            db.fxDao().observeLatest().first().firstOrNull { it.code == "USD" }?.sell?.let { add("💵 USD bán ${formatHundredths(it)}") }
            db.fuelDao().latest().chosen(s).firstOrNull()?.let { add("⛽ ${it.changeLine()}") }
            val outage = db.powerDao().upcoming(now).forMyArea(s, now).firstOrNull()
            add(if (outage == null) "⚡ Không có lịch cúp điện" else "⚡ ${formatTime(outage.startAt)}: ${outage.area.lines().first()}")
            if (isEmpty()) add("Chưa có dữ liệu: mở app để tải")
        }
    }
}

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DashboardWidget()
}
