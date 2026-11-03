package com.samuelcruz.bangtinnha.weather

import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import java.net.URLEncoder
import kotlin.time.Duration.Companion.hours

/** Open-Meteo forecast + air quality (free, no API key) for the place chosen in settings. */
class WeatherSource(
    private val http: Http,
    private val settings: SettingsRepository,
    private val dao: WeatherDao,
) : DataSource {
    override val id = "weather"
    override val title = "Thời tiết & AQI"
    override val refreshInterval = 1.hours

    override suspend fun refresh(): RefreshResult {
        val s = settings.current()
        val at = "latitude=${s.latitude}&longitude=${s.longitude}&timezone=auto"
        val forecast = WeatherParser.parseForecast(
            http.get(
                "https://api.open-meteo.com/v1/forecast?$at&forecast_days=7" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                    "&hourly=temperature_2m,weather_code,precipitation_probability" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
                id,
            ),
            s.locationName,
        )
        val air = WeatherParser.parseAirQuality(
            http.get(
                "https://air-quality-api.open-meteo.com/v1/air-quality?$at&forecast_days=2" +
                    "&current=us_aqi,pm2_5,pm10&hourly=us_aqi",
                "aqi",
            ),
        )
        // Keep the next 48 hours only; earlier hours of today are no use on the dashboard.
        val currentHour = forecast.current.observedAt.take(13) + ":00" // "2026-11-03T12:30" -> "2026-11-03T12:00"
        val hourly = forecast.hourly.filter { it.time >= currentHour }.take(48)
            .map { it.copy(usAqi = air.hourly[it.time]) }
        dao.replaceAll(
            forecast.current.copy(usAqi = air.usAqi, pm25 = air.pm25, pm10 = air.pm10),
            forecast.daily,
            hourly,
        )
        return RefreshResult("${forecast.daily.size} ngày dự báo, AQI ${air.usAqi ?: "?"}")
    }

    /** Place search for the settings screen (Open-Meteo geocoding; no GPS). */
    suspend fun searchPlaces(query: String): List<Place> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        return WeatherParser.parsePlaces(
            http.get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=10&language=vi&format=json", "geocoding"),
        )
    }
}
