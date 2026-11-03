package com.samuelcruz.bangtinnha.core

import android.content.Context
import com.samuelcruz.bangtinnha.core.backup.Maintenance
import com.samuelcruz.bangtinnha.core.db.AppDatabase
import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.notify.Alerts
import com.samuelcruz.bangtinnha.core.notify.Notifications
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import com.samuelcruz.bangtinnha.core.source.RefreshCoordinator
import com.samuelcruz.bangtinnha.fuel.FuelSource
import com.samuelcruz.bangtinnha.fx.FxSource
import com.samuelcruz.bangtinnha.gold.GoldSource
import com.samuelcruz.bangtinnha.power.PowerSource
import com.samuelcruz.bangtinnha.weather.WeatherSource
import com.samuelcruz.bangtinnha.widget.updateDashboardWidgets

/**
 * Hand-written dependency container (instead of Hilt): every long-lived object is built once here,
 * like a small service registry. Screens and workers read it from BangTinNhaApp.container.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val settings = SettingsRepository(app)
    val database = AppDatabase.open(app)
    val http = Http(app.cacheDir)
    val notifications = Notifications(app)

    val weather = WeatherSource(http, settings, database.weatherDao())
    val fx = FxSource(http, database.fxDao())
    val fuel = FuelSource(http, database.fuelDao())
    val power = PowerSource(http, settings, database.powerDao())
    val gold = GoldSource(http, database.goldDao())

    val alerts = Alerts(
        settings,
        database.notificationLogDao(),
        database.weatherDao(),
        database.fuelDao(),
        database.powerDao(),
        notifications,
    )

    /** Register new data sources here. */
    val coordinator = RefreshCoordinator(
        sources = listOf(weather, fx, gold, fuel, power),
        statusDao = database.sourceStatusDao(),
        afterRefresh = {
            alerts.checkUrgent()
            updateDashboardWidgets(app)
        },
    )

    val maintenance = Maintenance(app, database, settings)
}
