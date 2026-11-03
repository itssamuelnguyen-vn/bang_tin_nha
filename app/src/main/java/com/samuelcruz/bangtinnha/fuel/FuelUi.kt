package com.samuelcruz.bangtinnha.fuel

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatDong
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.notify.chosen
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.ui.ChangeBadge
import com.samuelcruz.bangtinnha.ui.Palette
import com.samuelcruz.bangtinnha.ui.PriceHistoryChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.Tile
import com.samuelcruz.bangtinnha.ui.appContainer

/** When the price took effect (Petrolimex's time), or when we first saw it. */
private fun FuelPrice.shownTime() = if (effectiveAt > 0) effectiveAt else fetchedAt

/** A colour per kind of fuel, for the dot before its name. */
private fun dotColor(name: String): Color = when {
    name.startsWith("Xăng") -> Color(0xFFFF9F43)
    name.startsWith("DO") -> Color(0xFF4E8FF5)
    name.startsWith("Dầu") -> Color(0xFF8E9EAB)
    else -> Color(0xFFB07D5B)
}

@Composable
private fun FuelRow(p: FuelLatest) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor(p.name)))
        Spacer(Modifier.width(10.dp))
        Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(p.price?.let { "${formatDong(it)}đ" } ?: "–", style = MaterialTheme.typography.titleMedium)
            ChangeBadge(p.price, p.previousPrice, ::formatDong)
        }
    }
}

@Composable
fun FuelCardContent() {
    val c = appContainer()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val latest by c.ui.fuelLatest.collectAsStateWithLifecycle()
    val shown = latest.chosen(settings)
    if (shown.isEmpty()) return Text("Chưa có dữ liệu giá xăng", style = MaterialTheme.typography.bodyMedium)
    shown.forEach { FuelRow(it) }
    val anyChange = shown.any { it.previousPrice != null && it.previousPrice != it.price }
    shown.mapNotNull { it.effectiveAt }.maxOrNull()?.let {
        Text(
            (if (anyChange) "" else "Giữ giá so với kỳ trước · ") + "Giá vùng 1 · áp dụng từ ${formatTime(it, "HH:mm dd/MM")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
fun FuelDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val latest by c.ui.fuelLatest.collectAsStateWithLifecycle()
    val shown = latest.chosen(settings)
    var selected by remember { mutableStateOf<Long?>(null) }
    val productId = selected ?: shown.firstOrNull()?.productId
    val history by remember(productId) { c.database.fuelDao().observeHistory(productId ?: -1) }
        .collectAsStateWithLifecycle(emptyList())
    SubScreen("Giá xăng dầu", onBack, Palette.fuel, subtitle = "Petrolimex · giá bán lẻ vùng 1 (đồng/lít, đồng/kg)") { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Tile { shown.forEach { FuelRow(it) } } }
            item {
                SectionTitle("Lịch sử giá")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { p ->
                        FilterChip(selected = productId == p.productId, onClick = { selected = p.productId }, label = { Text(p.name) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Tile {
                    if (history.size < 2) Text("Biểu đồ sẽ hiện sau lần đổi giá tiếp theo.", style = MaterialTheme.typography.bodySmall)
                    PriceHistoryChart(history.map { it.shownTime() to it.price }, toValue = { it.toDouble() })
                }
            }
            items(history.asReversed().take(10)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Text(formatTime(it.shownTime(), "HH:mm dd/MM/yyyy"), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text("${formatDong(it.price)}đ", style = MaterialTheme.typography.titleSmall)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
