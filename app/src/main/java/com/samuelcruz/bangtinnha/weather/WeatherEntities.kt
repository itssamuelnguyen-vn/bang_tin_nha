package com.samuelcruz.bangtinnha.weather

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The latest observation (a single row, id 0) plus the air quality measured with it. */
@Entity(tableName = "weather_current")
data class WeatherCurrent(
    @PrimaryKey val id: Int = 0,
    val locationName: String,
    /** Local time at the location, ISO "2026-11-03T12:30". */
    val observedAt: String,
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val weatherCode: Int,
    val windSpeed: Double,
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
)

@Entity(tableName = "weather_daily")
data class WeatherDaily(
    @PrimaryKey val date: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val rainProbability: Int?,
)

@Entity(tableName = "weather_hourly")
data class WeatherHourly(
    @PrimaryKey val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val rainProbability: Int?,
    val usAqi: Int? = null,
)

/** Weather keeps only the current forecast: every refresh replaces all rows (no long-term history). */
@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_current WHERE id = 0")
    fun observeCurrent(): Flow<WeatherCurrent?>

    @Query("SELECT * FROM weather_current WHERE id = 0")
    suspend fun current(): WeatherCurrent?

    @Query("SELECT * FROM weather_daily ORDER BY date")
    fun observeDaily(): Flow<List<WeatherDaily>>

    @Query("SELECT * FROM weather_daily ORDER BY date")
    suspend fun daily(): List<WeatherDaily>

    @Query("SELECT * FROM weather_hourly ORDER BY time")
    fun observeHourly(): Flow<List<WeatherHourly>>

    @Query("DELETE FROM weather_daily")
    suspend fun clearDaily()

    @Query("DELETE FROM weather_hourly")
    suspend fun clearHourly()

    @Upsert
    suspend fun upsertCurrent(current: WeatherCurrent)

    @Upsert
    suspend fun upsertDaily(rows: List<WeatherDaily>)

    @Upsert
    suspend fun upsertHourly(rows: List<WeatherHourly>)

    @Transaction
    suspend fun replaceAll(current: WeatherCurrent, daily: List<WeatherDaily>, hourly: List<WeatherHourly>) {
        upsertCurrent(current)
        clearDaily()
        upsertDaily(daily)
        clearHourly()
        upsertHourly(hourly)
    }
}
