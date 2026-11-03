package com.samuelcruz.bangtinnha.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.ui.LineChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.appContainer
import java.time.LocalDate

/** "AQI 74 · Trung bình" in the band's colour. */
@Composable
fun AqiBadge(aqi: Int) {
    val level = aqiLevel(aqi)
    Text(
        "AQI $aqi · ${level.label}",
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.background(level.color, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun WeatherCardContent() {
    val dao = appContainer().database.weatherDao()
    val current by dao.observeCurrent().collectAsStateWithLifecycle(null)
    val daily by dao.observeDaily().collectAsStateWithLifecycle(emptyList())
    val w = current ?: return Text("Chưa có dữ liệu thời tiết", style = MaterialTheme.typography.bodyMedium)
    val (emoji, text) = weatherOf(w.weatherCode)
    val today = daily.firstOrNull { it.date == LocalDate.now().toString() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 40.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${w.temperature.toInt()}°C · $text", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "${w.locationName} · cảm giác ${w.apparentTemperature.toInt()}° · ẩm ${w.humidity}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            today?.let {
                Text(
                    "Hôm nay ${it.tempMin.toInt()}–${it.tempMax.toInt()}°C" + (it.rainProbability?.let { p -> " · mưa $p%" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
    w.usAqi?.let {
        Spacer(Modifier.padding(top = 8.dp))
        AqiBadge(it)
    }
}

@Composable
fun WeatherDetailScreen(onBack: () -> Unit) {
    val dao = appContainer().database.weatherDao()
    val current by dao.observeCurrent().collectAsStateWithLifecycle(null)
    val daily by dao.observeDaily().collectAsStateWithLifecycle(emptyList())
    val hourly by dao.observeHourly().collectAsStateWithLifecycle(emptyList())
    SubScreen("Thời tiết · ${current?.locationName.orEmpty()}", onBack) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            current?.let { w ->
                item {
                    SectionTitle("Hiện tại (${w.observedAt.replace('T', ' ')})")
                    val (emoji, text) = weatherOf(w.weatherCode)
                    Text("$emoji ${w.temperature}°C · $text", style = MaterialTheme.typography.titleLarge)
                    Text("Cảm giác ${w.apparentTemperature}° · độ ẩm ${w.humidity}% · gió ${w.windSpeed} km/h")
                    w.usAqi?.let {
                        Spacer(Modifier.padding(top = 6.dp))
                        AqiBadge(it)
                        Text("PM2.5 ${w.pm25 ?: "?"} µg/m³ · PM10 ${w.pm10 ?: "?"} µg/m³", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (hourly.size >= 2) {
                item {
                    SectionTitle("Nhiệt độ 48 giờ tới")
                    val shown = hourly.take(48)
                    LineChart(
                        xs = shown.indices.map { it.toDouble() },
                        ys = shown.map { it.temperature },
                        xLabel = { i -> shown.getOrNull(i.toInt())?.time?.substring(11, 13)?.plus("h") ?: "" },
                    )
                    SectionTitle("24 giờ tới")
                }
                items(hourly.take(24)) { h ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(h.time.substring(11, 16), modifier = Modifier.width(52.dp))
                        Text(weatherOf(h.weatherCode).first, modifier = Modifier.width(32.dp))
                        Text("${h.temperature.toInt()}°", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold)
                        Text(h.rainProbability?.let { "💧$it%" } ?: "", modifier = Modifier.width(56.dp))
                        h.usAqi?.let { Text("AQI $it", color = aqiLevel(it).color) }
                    }
                    HorizontalDivider()
                }
            }
            if (daily.isNotEmpty()) {
                item { SectionTitle("7 ngày") }
                items(daily) { d ->
                    val (emoji, text) = weatherOf(d.weatherCode)
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(d.date.substring(8, 10) + "/" + d.date.substring(5, 7), modifier = Modifier.width(52.dp))
                        Text("$emoji $text", modifier = Modifier.weight(1f))
                        Text("${d.tempMin.toInt()}–${d.tempMax.toInt()}°")
                        Text(d.rainProbability?.let { "💧$it%" } ?: "", modifier = Modifier.width(56.dp))
                    }
                    HorizontalDivider()
                }
            }
            item { Spacer(Modifier.padding(bottom = 24.dp)) }
        }
    }
}
