package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.fuel.FuelCardContent
import com.samuelcruz.bangtinnha.fx.FxCardContent
import com.samuelcruz.bangtinnha.power.PowerCardContent
import com.samuelcruz.bangtinnha.weather.WeatherCardContent
import kotlinx.coroutines.launch

/** The dashboard: one card per source; pull down to refresh everything. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val c = appContainer()
    val statuses by c.database.sourceStatusDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val status = statuses.associateBy { it.sourceId }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bảng Tin Nhà") },
                actions = {
                    IconButton(onClick = { onOpen(Routes.SETTINGS) }) { Icon(Icons.Filled.Settings, contentDescription = "Cài đặt") }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    c.coordinator.refreshAll()
                    refreshing = false
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    ModuleCard("🌤️", "Thời tiết & không khí", status["weather"], c.weather.refreshInterval, { onOpen(Routes.WEATHER) }) {
                        WeatherCardContent()
                    }
                }
                item {
                    ModuleCard("💱", "Tỷ giá Vietcombank", status["fx"], c.fx.refreshInterval, { onOpen(Routes.FX) }) { FxCardContent() }
                }
                item {
                    ModuleCard("⛽", "Giá xăng dầu", status["fuel"], c.fuel.refreshInterval, { onOpen(Routes.FUEL) }) { FuelCardContent() }
                }
                item {
                    ModuleCard("⚡", "Cúp điện sắp tới", status["power"], c.power.refreshInterval, { onOpen(Routes.POWER) }) {
                        PowerCardContent()
                    }
                }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val WEATHER = "weather"
    const val FX = "fx"
    const val FUEL = "fuel"
    const val POWER = "power"
    const val SETTINGS = "settings"
    const val LOCATION = "settings/location"
    const val CURRENCIES = "settings/currencies"
    const val FUEL_PRODUCTS = "settings/fuel"
    const val POWER_AREA = "settings/power"
    const val NOTIFICATIONS = "settings/notifications"
    const val BATTERY = "settings/battery"
    const val STATUS = "settings/status"
    const val BACKUP = "settings/backup"
}
