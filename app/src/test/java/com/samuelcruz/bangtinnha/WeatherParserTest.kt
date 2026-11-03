package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.weather.WeatherParser
import com.samuelcruz.bangtinnha.weather.aqiLevel
import com.samuelcruz.bangtinnha.weather.weatherOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParserTest {
    @Test
    fun parsesForecast() {
        val f = WeatherParser.parseForecast(fixture("weather_forecast.json"), "Tây Ninh")
        assertEquals("Tây Ninh", f.current.locationName)
        assertEquals("2026-11-03T12:30", f.current.observedAt)
        assertEquals(29.3, f.current.temperature, 0.001)
        assertEquals(95, f.current.weatherCode)
        assertEquals(7, f.daily.size)
        assertEquals("2026-11-03", f.daily.first().date)
        assertEquals(30.8, f.daily.first().tempMax, 0.001)
        assertEquals(168, f.hourly.size)
    }

    @Test
    fun parsesAirQuality() {
        val a = WeatherParser.parseAirQuality(fixture("air_quality.json"))
        assertNotNull(a.usAqi)
        assertTrue(a.hourly.isNotEmpty())
        assertTrue(a.hourly.keys.first().startsWith("2026-11-03T"))
    }

    @Test
    fun parsesPlaces() {
        val places = WeatherParser.parsePlaces(fixture("geocoding.json"))
        assertEquals("Tây Ninh", places.first().name)
        assertEquals(11.31004, places.first().latitude, 0.0001)
        assertTrue(places.first().description.contains("Việt Nam"))
    }

    @Test
    fun aqiBandsAndWeatherNames() {
        assertEquals("Tốt", aqiLevel(50).label)
        assertEquals("Trung bình", aqiLevel(74).label)
        assertEquals("Kém cho nhóm nhạy cảm", aqiLevel(101).label)
        assertEquals("Nguy hại", aqiLevel(400).label)
        assertEquals("Dông", weatherOf(95).second)
    }
}
