package com.samuelcruz.bangtinnha

import android.app.Application
import com.samuelcruz.bangtinnha.core.AppContainer
import com.samuelcruz.bangtinnha.core.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BangTinNhaApp : Application() {
    lateinit var container: AppContainer
        private set

    /** For work that should outlive a screen (e.g. a refresh started from the UI). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifications.createChannels()
        WorkScheduler.scheduleRefresh(this)
        appScope.launch {
            val s = container.settings.current()
            WorkScheduler.scheduleMorning(this@BangTinNhaApp, s.morningHour, s.morningMinute, replace = false)
        }
    }
}
