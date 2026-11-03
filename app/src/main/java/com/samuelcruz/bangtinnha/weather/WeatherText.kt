package com.samuelcruz.bangtinnha.weather

import androidx.compose.ui.graphics.Color

/** Emoji and Vietnamese description of a WMO weather code. */
fun weatherOf(code: Int): Pair<String, String> = when (code) {
    0 -> "☀️" to "Trời quang"
    1 -> "🌤️" to "Ít mây"
    2 -> "⛅" to "Có mây"
    3 -> "☁️" to "Nhiều mây"
    45, 48 -> "🌫️" to "Sương mù"
    51, 53, 55, 56, 57 -> "🌦️" to "Mưa phùn"
    61, 66 -> "🌧️" to "Mưa nhỏ"
    63 -> "🌧️" to "Mưa vừa"
    65, 67 -> "🌧️" to "Mưa to"
    71, 73, 75, 77, 85, 86 -> "❄️" to "Tuyết"
    80, 81 -> "🌦️" to "Mưa rào"
    82 -> "⛈️" to "Mưa rào rất to"
    95 -> "⛈️" to "Dông"
    96, 99 -> "⛈️" to "Dông, mưa đá"
    else -> "🌡️" to "Không rõ"
}

/** US AQI band: Vietnamese label and colour. */
data class AqiLevel(val label: String, val color: Color)

fun aqiLevel(aqi: Int): AqiLevel = when {
    aqi <= 50 -> AqiLevel("Tốt", Color(0xFF2E7D32))
    aqi <= 100 -> AqiLevel("Trung bình", Color(0xFFF9A825))
    aqi <= 150 -> AqiLevel("Kém cho nhóm nhạy cảm", Color(0xFFEF6C00))
    aqi <= 200 -> AqiLevel("Xấu", Color(0xFFC62828))
    aqi <= 300 -> AqiLevel("Rất xấu", Color(0xFF6A1B9A))
    else -> AqiLevel("Nguy hại", Color(0xFF4E0D0D))
}

/** WMO codes worth a warning: heavy rain, violent showers, thunderstorms. */
val SEVERE_CODES = setOf(65, 67, 82, 95, 96, 99)

private fun WeatherHourly.isWet() = weatherCode >= 51 || (rainProbability ?: 0) >= 60

private fun WeatherHourly.hour() = time.substring(11, 13).toInt()

/**
 * One line about rain in the next 12 hours ([next] starts at the current hour): "🌧️ Có thể mưa khoảng 16h–19h",
 * "⛈️ Có dông khoảng …", "🌧️ Đang mưa, đến khoảng 18h" or "🌤️ 12 giờ tới ít khả năng mưa".
 */
fun rainSummary(next: List<WeatherHourly>): String {
    val window = next.take(12)
    val first = window.indexOfFirst { it.isWet() }
    if (first < 0) return "🌤️ 12 giờ tới ít khả năng mưa"
    var last = first
    while (last + 1 < window.size && window[last + 1].isWet()) last++
    val storm = window.subList(first, last + 1).any { it.weatherCode >= 95 }
    val end = (window[last].hour() + 1) % 24
    return when {
        first == 0 -> (if (storm) "⛈️ Đang có dông" else "🌧️ Đang mưa") + ", đến khoảng ${end}h"
        else -> (if (storm) "⛈️ Có dông" else "🌧️ Có thể mưa") + " khoảng ${window[first].hour()}h–${end}h"
    }
}
