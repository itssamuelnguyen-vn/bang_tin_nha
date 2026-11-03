package com.samuelcruz.bangtinnha.fuel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatDong
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.notify.chosen
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.ui.PriceHistoryChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.appContainer

/** When the price took effect (Petrolimex's time), or when we first saw it. */
private fun FuelPrice.shownTime() = if (effectiveAt > 0) effectiveAt else fetchedAt

/** Price with a coloured ▲/▼ and the difference from the previous price. */
@Composable
private fun PriceWithChange(p: FuelLatest) {
    val price = p.price ?: return Text("–")
    val diff = p.previousPrice?.let { price - it } ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("${formatDong(price)}đ", fontWeight = FontWeight.Bold)
        when {
            diff > 0 -> Text("▲${formatDong(diff)}", color = Color(0xFFC62828))
            diff < 0 -> Text("▼${formatDong(-diff)}", color = Color(0xFF2E7D32))
        }
    }
}

@Composable
fun FuelCardContent() {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val latest by c.database.fuelDao().observeLatest().collectAsStateWithLifecycle(emptyList())
    val shown = latest.chosen(settings)
    if (shown.isEmpty()) return Text("Chưa có dữ liệu giá xăng", style = MaterialTheme.typography.bodyMedium)
    shown.forEach { p ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(p.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            PriceWithChange(p)
        }
    }
    shown.mapNotNull { it.effectiveAt }.maxOrNull()?.let {
        Text("Giá vùng 1, áp dụng từ ${formatTime(it, "HH:mm dd/MM")}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun FuelDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val latest by c.database.fuelDao().observeLatest().collectAsStateWithLifecycle(emptyList())
    val shown = latest.chosen(settings)
    var selected by remember { mutableStateOf<Long?>(null) }
    val productId = selected ?: shown.firstOrNull()?.productId
    val history by remember(productId) { c.database.fuelDao().observeHistory(productId ?: -1) }
        .collectAsStateWithLifecycle(emptyList())
    SubScreen("Giá xăng dầu Petrolimex", onBack) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item { Text("Giá bán lẻ vùng 1 (đồng/lít, đồng/kg)", style = MaterialTheme.typography.bodySmall) }
            items(shown) { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(p.name, modifier = Modifier.weight(1f))
                    PriceWithChange(p)
                }
                HorizontalDivider()
            }
            item {
                SectionTitle("Lịch sử giá")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { p ->
                        FilterChip(selected = productId == p.productId, onClick = { selected = p.productId }, label = { Text(p.name) })
                    }
                }
                PriceHistoryChart(history.map { it.shownTime() to it.price }, toValue = { it.toDouble() })
                history.asReversed().take(10).forEach {
                    Text("${formatTime(it.shownTime(), "HH:mm dd/MM/yyyy")}: ${formatDong(it.price)}đ")
                }
            }
        }
    }
}
