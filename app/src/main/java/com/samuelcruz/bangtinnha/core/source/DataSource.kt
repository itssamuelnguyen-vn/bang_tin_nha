package com.samuelcruz.bangtinnha.core.source

import kotlin.time.Duration

/**
 * One kind of data on the dashboard (weather, exchange rates, fuel, power cuts).
 * Like a self-contained Rails service object: it downloads, parses and saves its own data.
 * To add a source, write a class implementing this and add it to AppContainer.sources.
 */
interface DataSource {
    /** Stable key: "weather", "fx", "fuel", "power". */
    val id: String

    /** Vietnamese name shown in the status screen. */
    val title: String

    /** How often the data needs refreshing. */
    val refreshInterval: Duration

    /** Downloads, parses and stores the data. Throws on failure; the coordinator records the error. */
    suspend fun refresh(): RefreshResult
}

/** A short human-readable summary, e.g. "7 ngày dự báo, AQI 74". */
data class RefreshResult(val summary: String)
