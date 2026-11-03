package com.samuelcruz.bangtinnha.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.db.SourceStatus
import com.samuelcruz.bangtinnha.ui.Freshness
import com.samuelcruz.bangtinnha.ui.LineChart
import com.samuelcruz.bangtinnha.ui.Palette
import com.samuelcruz.bangtinnha.ui.Pill
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.Tile
import com.samuelcruz.bangtinnha.ui.appContainer
import com.samuelcruz.bangtinnha.ui.fadeEnd
import com.samuelcruz.bangtinnha.ui.freshness
import java.time.LocalDate
import kotlin.time.Duration

private val white = Color.White
private val glass = Color.White.copy(alpha = 0.18f)

/** "AQI 74 · Trung bình" in the band's colour. */
@Composable
fun AqiBadge(aqi: Int) {
    val level = aqiLevel(aqi)
    Pill("AQI $aqi · ${level.label}", level.color, Color.White)
}

private fun hourOf(isoTime: String) = isoTime.substring(11, 13).toIntOrNull() ?: 12

/** The big weather card at the top of the dashboard; its gradient follows the sky. */
@Composable
fun WeatherHero(status: SourceStatus?, interval: Duration, onClick: () -> Unit) {
    val dao = appContainer().database.weatherDao()
    val current by dao.observeCurrent().collectAsStateWithLifecycle(null)
    val daily by dao.observeDaily().collectAsStateWithLifecycle(emptyList())
    val hourly by dao.observeHourly().collectAsStateWithLifecycle(emptyList())
    val w = current
    val colors = Palette.weather(w?.weatherCode ?: 1, w?.observedAt?.let(::hourOf) ?: 12)
    val f = freshness(status, interval)

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.gradient).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📍 ${w?.locationName ?: "Thời tiết"}", color = white, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                when (f) {
                    is Freshness.Fresh -> Pill(f.text, glass, white)
                    is Freshness.Stale -> Pill(f.text, Palette.warn, white)
                    is Freshness.Failed -> Pill("Lỗi tải", Color(0xFFE84357), white)
                    Freshness.Empty -> Pill("Kéo để tải", glass, white)
                }
            }
            if (f is Freshness.Failed) {
                Text("⚠ ${f.message}", color = white, style = MaterialTheme.typography.bodySmall, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
            }
            if (w == null) {
                Text("Chưa có dữ liệu thời tiết", color = white, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 24.dp))
                return@Column
            }
            val (emoji, text) = weatherOf(w.weatherCode)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("${w.temperature.toInt()}°", color = white, fontSize = 76.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 80.sp)
                    Text(text, color = white, style = MaterialTheme.typography.titleLarge)
                    Text("Cảm giác như ${w.apparentTemperature.toInt()}°", color = white.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                Text(emoji, fontSize = 84.sp)
            }
            val today = daily.firstOrNull { it.date == LocalDate.now().toString() }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                today?.let { Stat("🌡️", "${it.tempMin.toInt()}–${it.tempMax.toInt()}°", Modifier.weight(1f)) }
                Stat("💧", today?.rainProbability?.let { "$it%" } ?: "${w.humidity}%", Modifier.weight(1f))
                w.usAqi?.let { Stat("🍃", "AQI $it", Modifier.weight(1f), dot = aqiLevel(it).color) }
            }
            if (hourly.isNotEmpty()) {
                Text(rainSummary(hourly), color = white, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp))
            }
            if (hourly.size > 1) {
                Row(
                    Modifier.padding(top = 10.dp).fadeEnd().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    hourly.drop(1).take(8).forEach { h ->
                        Column(
                            Modifier.clip(RoundedCornerShape(18.dp)).background(glass).padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${hourOf(h.time)}h", color = white.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
                            Text(weatherOf(h.weatherCode).first, fontSize = 22.sp)
                            Text("${h.temperature.toInt()}°", color = white, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: String, value: String, modifier: Modifier, dot: Color? = null) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(glass).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 15.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = white, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        dot?.let {
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(it))
        }
    }
}

@Composable
fun WeatherDetailScreen(onBack: () -> Unit) {
    val dao = appContainer().database.weatherDao()
    val current by dao.observeCurrent().collectAsStateWithLifecycle(null)
    val daily by dao.observeDaily().collectAsStateWithLifecycle(emptyList())
    val hourly by dao.observeHourly().collectAsStateWithLifecycle(emptyList())
    val colors = Palette.weather(current?.weatherCode ?: 1, current?.observedAt?.let(::hourOf) ?: 12)
    SubScreen(
        title = current?.let { "${it.temperature.toInt()}° · ${weatherOf(it.weatherCode).second}" } ?: "Thời tiết",
        subtitle = current?.let { "📍 ${it.locationName} · cảm giác ${it.apparentTemperature.toInt()}° · ẩm ${it.humidity}% · gió ${it.windSpeed.toInt()} km/h" },
        colors = colors,
        onBack = onBack,
    ) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current?.usAqi?.let { aqi ->
                item {
                    SectionTitle("Chất lượng không khí")
                    Tile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$aqi", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = aqiLevel(aqi).color)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(aqiLevel(aqi).label, style = MaterialTheme.typography.titleMedium)
                                Text("US AQI · PM2.5 ${current?.pm25 ?: "?"} · PM10 ${current?.pm10 ?: "?"} µg/m³", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            if (hourly.size >= 2) {
                item {
                    SectionTitle("Nhiệt độ 48 giờ tới")
                    Tile {
                        LineChart(
                            xs = hourly.indices.map { it.toDouble() },
                            ys = hourly.map { it.temperature },
                            xLabel = { i -> hourly.getOrNull(i.toInt())?.let { "${hourOf(it.time)}h" } ?: "" },
                        )
                    }
                    SectionTitle("Theo giờ")
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hourly.take(24).forEach { h ->
                            Column(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("${hourOf(h.time)}h", style = MaterialTheme.typography.labelMedium)
                                Text(weatherOf(h.weatherCode).first, fontSize = 24.sp)
                                Text("${h.temperature.toInt()}°", style = MaterialTheme.typography.titleSmall)
                                Text(h.rainProbability?.let { "💧$it%" } ?: " ", style = MaterialTheme.typography.labelSmall)
                                h.usAqi?.let { Text("AQI $it", style = MaterialTheme.typography.labelSmall, color = aqiLevel(it).color) }
                            }
                        }
                    }
                }
            }
            if (daily.isNotEmpty()) {
                item { SectionTitle("7 ngày tới") }
                items(daily) { d ->
                    val (emoji, text) = weatherOf(d.weatherCode)
                    val date = LocalDate.parse(d.date)
                    val label = if (date == LocalDate.now()) "Hôm nay" else listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")[date.dayOfWeek.value - 1] + " " + d.date.substring(8, 10)
                    Tile {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(76.dp))
                            Text(emoji, fontSize = 24.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(text, style = MaterialTheme.typography.bodyMedium)
                                d.rainProbability?.let { Text("💧 $it%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Text("${d.tempMin.toInt()}°", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            Text("${d.tempMax.toInt()}°", style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
