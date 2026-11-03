package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.weather.WeatherHourly
import com.samuelcruz.bangtinnha.weather.rainSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class RainSummaryTest {
    private fun hours(vararg codes: Int, from: Int = 14) =
        codes.mapIndexed { i, code -> WeatherHourly("2026-11-03T%02d:00".format(from + i), 30.0, code, if (code >= 51) 80 else 10) }

    @Test
    fun dryDay() = assertEquals("🌤️ 12 giờ tới ít khả năng mưa", rainSummary(hours(1, 2, 3, 3)))

    @Test
    fun rainLater() = assertEquals("🌧️ Có thể mưa khoảng 16h–19h", rainSummary(hours(2, 3, 61, 63, 61, 3)))

    @Test
    fun stormLater() = assertEquals("⛈️ Có dông khoảng 15h–17h", rainSummary(hours(3, 95, 81, 2)))

    @Test
    fun rainingNow() = assertEquals("🌧️ Đang mưa, đến khoảng 16h", rainSummary(hours(61, 63, 2)))
}
