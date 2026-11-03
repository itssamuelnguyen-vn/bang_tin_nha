package com.samuelcruz.bangtinnha.ui

import com.samuelcruz.bangtinnha.core.db.AppDatabase
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * The data the screens show, kept as shared StateFlows that outlive a single screen (similar to a cache in front of
 * the database).
 *
 * Collecting a fresh Room flow inside each composable meant every card started empty and filled a frame later, on
 * every navigation back to the dashboard, which made the layout jump. These flows keep their last value for a minute
 * after the last screen stops watching, so going back and forth shows the data at once.
 */
class UiData(database: AppDatabase, settingsRepository: SettingsRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private fun <T> Flow<T>.hold(initial: T): StateFlow<T> =
        stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 60_000), initial)

    val settings: StateFlow<AppSettings> = settingsRepository.settings.hold(AppSettings())
    val statuses = database.sourceStatusDao().observeAll().hold(emptyList())

    val weatherCurrent = database.weatherDao().observeCurrent().hold(null)
    val weatherDaily = database.weatherDao().observeDaily().hold(emptyList())
    val weatherHourly = database.weatherDao().observeHourly().hold(emptyList())

    val fxLatest = database.fxDao().observeLatest().hold(emptyList())
    val goldLatest = database.goldDao().observeLatest().hold(emptyList())
    val fuelLatest = database.fuelDao().observeLatest().hold(emptyList())

    /** Outages not yet over; "now" is taken each time the flow (re)starts, so ended outages drop out. */
    val powerUpcoming = flow { emitAll(database.powerDao().observeUpcoming(System.currentTimeMillis())) }.hold(emptyList())
}
