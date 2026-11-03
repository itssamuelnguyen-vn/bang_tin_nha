package com.samuelcruz.bangtinnha.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Open-Meteo answers, mapped with kotlinx.serialization (unknown fields ignored). */
private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class ForecastResponse(val current: Current, val hourly: Hourly, val daily: Daily) {
    @Serializable
    data class Current(
        val time: String,
        @SerialName("temperature_2m") val temperature: Double,
        @SerialName("relative_humidity_2m") val humidity: Int,
        @SerialName("apparent_temperature") val apparentTemperature: Double,
        @SerialName("weather_code") val weatherCode: Int,
        @SerialName("wind_speed_10m") val windSpeed: Double,
    )

    @Serializable
    data class Hourly(
        val time: List<String>,
        @SerialName("temperature_2m") val temperature: List<Double?>,
        @SerialName("weather_code") val weatherCode: List<Int?>,
        @SerialName("precipitation_probability") val rainProbability: List<Int?>,
    )

    @Serializable
    data class Daily(
        val time: List<String>,
        @SerialName("weather_code") val weatherCode: List<Int?>,
        @SerialName("temperature_2m_max") val tempMax: List<Double?>,
        @SerialName("temperature_2m_min") val tempMin: List<Double?>,
        @SerialName("precipitation_probability_max") val rainProbability: List<Int?>,
    )
}

@Serializable
private data class AirQualityResponse(val current: Current, val hourly: Hourly) {
    @Serializable
    data class Current(
        val time: String,
        @SerialName("us_aqi") val usAqi: Int? = null,
        @SerialName("pm2_5") val pm25: Double? = null,
        @SerialName("pm10") val pm10: Double? = null,
    )

    @Serializable
    data class Hourly(val time: List<String>, @SerialName("us_aqi") val usAqi: List<Int?>)
}

@Serializable
private data class GeocodingResponse(val results: List<Result> = emptyList()) {
    @Serializable
    data class Result(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val country: String? = null,
        val admin1: String? = null,
        val admin2: String? = null,
    )
}

data class Forecast(val current: WeatherCurrent, val daily: List<WeatherDaily>, val hourly: List<WeatherHourly>)

data class AirQuality(val usAqi: Int?, val pm25: Double?, val pm10: Double?, val hourly: Map<String, Int>)

data class Place(val name: String, val latitude: Double, val longitude: Double, val description: String)

object WeatherParser {
    fun parseForecast(body: String, locationName: String): Forecast {
        val r = json.decodeFromString<ForecastResponse>(body)
        val c = r.current
        return Forecast(
            current = WeatherCurrent(
                locationName = locationName,
                observedAt = c.time,
                temperature = c.temperature,
                apparentTemperature = c.apparentTemperature,
                humidity = c.humidity,
                weatherCode = c.weatherCode,
                windSpeed = c.windSpeed,
            ),
            daily = r.daily.time.indices.mapNotNull { i ->
                val code = r.daily.weatherCode[i] ?: return@mapNotNull null
                WeatherDaily(
                    date = r.daily.time[i],
                    weatherCode = code,
                    tempMax = r.daily.tempMax[i] ?: return@mapNotNull null,
                    tempMin = r.daily.tempMin[i] ?: return@mapNotNull null,
                    rainProbability = r.daily.rainProbability[i],
                )
            },
            hourly = r.hourly.time.indices.mapNotNull { i ->
                WeatherHourly(
                    time = r.hourly.time[i],
                    temperature = r.hourly.temperature[i] ?: return@mapNotNull null,
                    weatherCode = r.hourly.weatherCode[i] ?: return@mapNotNull null,
                    rainProbability = r.hourly.rainProbability[i],
                )
            },
        )
    }

    fun parseAirQuality(body: String): AirQuality {
        val r = json.decodeFromString<AirQualityResponse>(body)
        val hourly = buildMap {
            r.hourly.time.forEachIndexed { i, t -> r.hourly.usAqi[i]?.let { put(t, it) } }
        }
        return AirQuality(r.current.usAqi, r.current.pm25, r.current.pm10, hourly)
    }

    fun parsePlaces(body: String): List<Place> = json.decodeFromString<GeocodingResponse>(body).results.map {
        Place(it.name, it.latitude, it.longitude, listOfNotNull(it.admin2, it.admin1, it.country).distinct().joinToString(", "))
    }
}
