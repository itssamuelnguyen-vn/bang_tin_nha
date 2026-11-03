package com.samuelcruz.bangtinnha.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samuelcruz.bangtinnha.BangTinNhaApp
import com.samuelcruz.bangtinnha.core.VN_ZONE
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * The single periodic job (WorkManager ~ a recurring background job). Every hour, with a network, it refreshes the
 * sources that are due, checks urgent alerts (also time-based ones like "outage starts within 24h"), and runs
 * monthly maintenance.
 */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as BangTinNhaApp).container
        c.coordinator.refreshDue()
        c.alerts.checkUrgent()
        c.maintenance.vacuumIfDue()
        return Result.success()
    }
}

/** Sends the morning brief, then queues itself for the next morning. */
class MorningBriefWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as BangTinNhaApp).container
        runCatching { c.coordinator.refreshDue() } // best effort: without a network, yesterday's data still helps
        c.alerts.sendMorningBrief()
        val s = c.settings.current()
        WorkScheduler.scheduleMorning(applicationContext, s.morningHour, s.morningMinute, replace = false, chain = true)
        return Result.success()
    }
}

object WorkScheduler {
    private const val REFRESH = "refresh"
    private const val MORNING = "morning-brief"

    fun scheduleRefresh(context: Context) {
        val request = PeriodicWorkRequestBuilder<RefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(REFRESH, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * Queues the morning brief for the next [hour]:[minute]. Not exact (no SCHEDULE_EXACT_ALARM): Android may run it
     * some minutes later, more when the phone is in deep sleep. [replace] is for a changed time; [chain] appends after
     * the brief that is running now instead of cancelling it.
     */
    fun scheduleMorning(context: Context, hour: Int, minute: Int, replace: Boolean, chain: Boolean = false) {
        val now = ZonedDateTime.now(VN_ZONE)
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now.plusMinutes(1))) next = next.plusDays(1)
        val request = OneTimeWorkRequestBuilder<MorningBriefWorker>()
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        val policy = when {
            chain -> ExistingWorkPolicy.APPEND_OR_REPLACE
            replace -> ExistingWorkPolicy.REPLACE
            else -> ExistingWorkPolicy.KEEP
        }
        WorkManager.getInstance(context).enqueueUniqueWork(MORNING, policy, request)
    }
}
