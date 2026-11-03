package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.VN_ZONE
import com.samuelcruz.bangtinnha.fuel.FuelCardContent
import com.samuelcruz.bangtinnha.fx.FxCardContent
import com.samuelcruz.bangtinnha.power.PowerCardContent
import com.samuelcruz.bangtinnha.weather.WeatherHero
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private val weekdays = listOf("Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật")

/** The dashboard: greeting, the weather hero, then one card per source. Pull down to refresh everything. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    val c = appContainer()
    val statuses by c.database.sourceStatusDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val status = statuses.associateBy { it.sourceId }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                c.coordinator.refreshAll()
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Greeting(onSettings = { onOpen(Routes.SETTINGS) }) }
            item { WeatherHero(status["weather"], c.weather.refreshInterval, onClick = { onOpen(Routes.WEATHER) }) }
            item {
                ModuleCard("💱", "Tỷ giá Vietcombank", Palette.fx, status["fx"], c.fx.refreshInterval, { onOpen(Routes.FX) }) {
                    FxCardContent()
                }
            }
            item {
                ModuleCard("⛽", "Giá xăng dầu", Palette.fuel, status["fuel"], c.fuel.refreshInterval, { onOpen(Routes.FUEL) }) {
                    FuelCardContent()
                }
            }
            item {
                ModuleCard("⚡", "Lịch cúp điện", Palette.power, status["power"], c.power.refreshInterval, { onOpen(Routes.POWER) }) {
                    PowerCardContent()
                }
            }
            item {
                Text(
                    "Kéo xuống để cập nhật tất cả",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp).fillParentMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Greeting(onSettings: () -> Unit) {
    val now = ZonedDateTime.now(VN_ZONE)
    val (hello, wave) = when (now.hour) {
        in 5..10 -> "Chào buổi sáng" to "☀️"
        in 11..12 -> "Chào buổi trưa" to "🌤️"
        in 13..17 -> "Chào buổi chiều" to "👋"
        else -> "Chào buổi tối" to "🌙"
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                "${weekdays[now.dayOfWeek.value - 1]}, %02d/%02d/%d".format(now.dayOfMonth, now.monthValue, now.year),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("$hello $wave", style = MaterialTheme.typography.headlineMedium)
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer),
        ) { Icon(Icons.Filled.Settings, contentDescription = "Cài đặt", tint = MaterialTheme.colorScheme.primary) }
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
