package com.samuelcruz.bangtinnha.core.notify

import com.samuelcruz.bangtinnha.core.VN_ZONE
import com.samuelcruz.bangtinnha.core.db.NotificationLog
import com.samuelcruz.bangtinnha.core.db.NotificationLogDao
import com.samuelcruz.bangtinnha.core.formatDong
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import com.samuelcruz.bangtinnha.core.text.matchesAnyKeyword
import com.samuelcruz.bangtinnha.fuel.FuelDao
import com.samuelcruz.bangtinnha.fuel.FuelLatest
import com.samuelcruz.bangtinnha.power.PowerDao
import com.samuelcruz.bangtinnha.power.PowerOutage
import com.samuelcruz.bangtinnha.weather.WeatherDao
import com.samuelcruz.bangtinnha.weather.aqiLevel
import com.samuelcruz.bangtinnha.weather.weatherOf
import java.time.LocalDate

private const val HOUR = 3600_000L
private const val DAY = 24 * HOUR

/** Outages of the chosen areas (keywords), still to come or under way. */
fun List<PowerOutage>.forMyArea(settings: AppSettings, now: Long): List<PowerOutage> =
    filter { it.endAt >= now && matchesAnyKeyword(it.area, settings.powerKeywords) }

/** Fuel products the user chose (all when none chosen). */
fun List<FuelLatest>.chosen(settings: AppSettings): List<FuelLatest> =
    if (settings.fuelProducts.isEmpty()) this else filter { it.sourceKey in settings.fuelProducts }

/** "RON 95-V: 24.950đ (▲300)". */
fun FuelLatest.changeLine(): String {
    val p = price ?: return name
    val diff = previousPrice?.let { p - it } ?: 0
    val arrow = when {
        diff > 0 -> " (▲${formatDong(diff)})"
        diff < 0 -> " (▼${formatDong(-diff)})"
        else -> ""
    }
    return "$name: ${formatDong(p)}đ$arrow"
}

/**
 * Urgent alerts and the morning brief. Every alert is recorded in notification_log under a stable key,
 * so nothing is announced twice (like an idempotency key).
 */
class Alerts(
    private val settings: SettingsRepository,
    private val log: NotificationLogDao,
    private val weather: WeatherDao,
    private val fuel: FuelDao,
    private val power: PowerDao,
    private val notifications: Notifications,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** Called after every refresh run: power cuts in the next 24 hours, and fuel price changes. */
    suspend fun checkUrgent() {
        val s = settings.current()
        val t = now()
        if (s.notifyPower) {
            val soon = power.upcoming(t).forMyArea(s, t).filter { it.startAt <= t + DAY }
            val fresh = soon.filterNot { log.wasSent(powerKey(it)) }
            if (fresh.isNotEmpty()) {
                notifications.post(
                    Channel.POWER,
                    NOTIFY_POWER,
                    "Cúp điện trong 24 giờ tới",
                    fresh.map { "${formatTime(it.startAt, "HH:mm")}–${formatTime(it.endAt, "HH:mm dd/MM")}: ${it.area.lines().first()}" },
                )
                fresh.forEach { log.markSent(NotificationLog(powerKey(it), t)) }
            }
        }
        if (s.notifyFuel) {
            // A change = the newest row has a previous row to compare with, and arrived in the last 2 days.
            val changed = fuel.latest().chosen(s).filter {
                it.priceId != null && it.previousPrice != null && it.previousPrice != it.price &&
                    (it.fetchedAt ?: 0) > t - 2 * DAY && !log.wasSent("fuel:${it.priceId}")
            }
            if (changed.isNotEmpty()) {
                notifications.post(Channel.FUEL, NOTIFY_FUEL, "Giá xăng dầu vừa thay đổi", changed.map { it.changeLine() })
                changed.forEach { log.markSent(NotificationLog("fuel:${it.priceId}", t)) }
            }
        }
        log.deleteOlderThan(t - 90 * DAY)
    }

    /** One morning summary per day; returns false when today's was already sent. */
    suspend fun sendMorningBrief(): Boolean {
        val s = settings.current()
        val t = now()
        val key = "brief:${LocalDate.now(VN_ZONE)}"
        if (!s.notifyMorning || log.wasSent(key)) return false
        val lines = morningLines(s, t)
        notifications.post(Channel.MORNING, NOTIFY_MORNING, "Bản tin sáng · ${s.locationName}", lines)
        log.markSent(NotificationLog(key, t))
        return true
    }

    suspend fun morningLines(s: AppSettings, t: Long): List<String> = buildList {
        val today = LocalDate.now(VN_ZONE).toString()
        weather.daily().firstOrNull { it.date == today }?.let { d ->
            val (emoji, text) = weatherOf(d.weatherCode)
            add("$emoji $text, ${d.tempMin.toInt()}–${d.tempMax.toInt()}°C" + (d.rainProbability?.let { ", mưa $it%" } ?: ""))
        }
        weather.current()?.usAqi?.let { add("Không khí: AQI $it (${aqiLevel(it).label})") }
        power.upcoming(t).forMyArea(s, t).filter { it.startAt <= t + 2 * DAY }.take(3).forEach {
            add("⚡ Cúp điện ${formatTime(it.startAt, "HH:mm dd/MM")}–${formatTime(it.endAt, "HH:mm")}: ${it.area.lines().first()}")
        }
        fuel.latest().chosen(s)
            .filter { it.previousPrice != null && it.previousPrice != it.price && (it.fetchedAt ?: 0) > t - DAY }
            .forEach { add("⛽ ${it.changeLine()}") }
        if (isEmpty()) add("Chưa có dữ liệu mới. Mở app để cập nhật.")
    }

    private fun powerKey(o: PowerOutage) = "power:${o.unitCode}:${o.startAt}:${o.area.hashCode()}"

    companion object {
        const val NOTIFY_MORNING = 1
        const val NOTIFY_POWER = 2
        const val NOTIFY_FUEL = 3
    }
}
