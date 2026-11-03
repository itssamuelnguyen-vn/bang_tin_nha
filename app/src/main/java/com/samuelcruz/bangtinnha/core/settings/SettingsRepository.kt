package com.samuelcruz.bangtinnha.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** A power company unit chosen for outage lookups, e.g. PB0501 "Điện lực Tân Ninh". */
data class PowerUnit(val code: String, val name: String)

/** Everything the user can configure. Defaults suit a family in Tây Ninh. */
data class AppSettings(
    val locationName: String = "Tây Ninh",
    val latitude: Double = 11.31004,
    val longitude: Double = 106.09828,
    val currencies: Set<String> = setOf("USD", "EUR", "JPY", "CNY", "KRW"),
    /** Petrolimex product ids to show; empty means all. */
    val fuelProducts: Set<String> = emptySet(),
    val powerCompany: String = "PB05",
    val powerUnits: List<PowerUnit> = listOf(PowerUnit("PB0501", "Điện lực Tân Ninh")),
    val powerKeywords: List<String> = emptyList(),
    val notifyMorning: Boolean = true,
    val notifyPower: Boolean = true,
    val notifyFuel: Boolean = true,
    val morningHour: Int = 6,
    val morningMinute: Int = 30,
    val lastVacuumAt: Long = 0,
)

/** Typed access to DataStore (think of it as a small key/value settings table). */
class SettingsRepository(private val context: Context) {
    private object Keys {
        val locationName = stringPreferencesKey("location_name")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val currencies = stringSetPreferencesKey("currencies")
        val fuelProducts = stringSetPreferencesKey("fuel_products")
        val powerCompany = stringPreferencesKey("power_company")
        val powerUnits = stringSetPreferencesKey("power_units") // "code|name"
        val powerKeywords = stringPreferencesKey("power_keywords") // one per line
        val notifyMorning = booleanPreferencesKey("notify_morning")
        val notifyPower = booleanPreferencesKey("notify_power")
        val notifyFuel = booleanPreferencesKey("notify_fuel")
        val morningHour = intPreferencesKey("morning_hour")
        val morningMinute = intPreferencesKey("morning_minute")
        val lastVacuumAt = longPreferencesKey("last_vacuum_at")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            locationName = p[Keys.locationName] ?: d.locationName,
            latitude = p[Keys.latitude] ?: d.latitude,
            longitude = p[Keys.longitude] ?: d.longitude,
            currencies = p[Keys.currencies] ?: d.currencies,
            fuelProducts = p[Keys.fuelProducts] ?: d.fuelProducts,
            powerCompany = p[Keys.powerCompany] ?: d.powerCompany,
            powerUnits = p[Keys.powerUnits]
                ?.map { PowerUnit(it.substringBefore('|'), it.substringAfter('|')) }
                ?.sortedBy { it.code }
                ?: d.powerUnits,
            powerKeywords = p[Keys.powerKeywords]?.lines()?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: d.powerKeywords,
            notifyMorning = p[Keys.notifyMorning] ?: d.notifyMorning,
            notifyPower = p[Keys.notifyPower] ?: d.notifyPower,
            notifyFuel = p[Keys.notifyFuel] ?: d.notifyFuel,
            morningHour = p[Keys.morningHour] ?: d.morningHour,
            morningMinute = p[Keys.morningMinute] ?: d.morningMinute,
            lastVacuumAt = p[Keys.lastVacuumAt] ?: d.lastVacuumAt,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLocation(name: String, latitude: Double, longitude: Double) = context.dataStore.edit {
        it[Keys.locationName] = name
        it[Keys.latitude] = latitude
        it[Keys.longitude] = longitude
    }

    suspend fun setCurrencies(codes: Set<String>) = context.dataStore.edit { it[Keys.currencies] = codes }

    suspend fun setFuelProducts(ids: Set<String>) = context.dataStore.edit { it[Keys.fuelProducts] = ids }

    suspend fun setPowerCompany(code: String) = context.dataStore.edit {
        it[Keys.powerCompany] = code
        it[Keys.powerUnits] = emptySet()
    }

    suspend fun setPowerUnits(units: List<PowerUnit>) =
        context.dataStore.edit { p -> p[Keys.powerUnits] = units.map { "${it.code}|${it.name}" }.toSet() }

    suspend fun setPowerKeywords(keywords: List<String>) =
        context.dataStore.edit { it[Keys.powerKeywords] = keywords.joinToString("\n") }

    suspend fun setNotifyMorning(on: Boolean) = context.dataStore.edit { it[Keys.notifyMorning] = on }

    suspend fun setNotifyPower(on: Boolean) = context.dataStore.edit { it[Keys.notifyPower] = on }

    suspend fun setNotifyFuel(on: Boolean) = context.dataStore.edit { it[Keys.notifyFuel] = on }

    suspend fun setMorningTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.morningHour] = hour
        it[Keys.morningMinute] = minute
    }

    suspend fun setLastVacuumAt(time: Long) = context.dataStore.edit { it[Keys.lastVacuumAt] = time }
}
