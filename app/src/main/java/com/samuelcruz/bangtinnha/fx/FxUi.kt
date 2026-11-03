package com.samuelcruz.bangtinnha.fx

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatHundredths
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.ui.PriceHistoryChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.appContainer

private fun FxLatest.price(v: Long?) = v?.let(::formatHundredths) ?: "–"

@Composable
fun FxCardContent() {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val latest by c.database.fxDao().observeLatest().collectAsStateWithLifecycle(emptyList())
    val shown = latest.filter { it.code in settings.currencies }
    if (shown.isEmpty()) return Text("Chưa có dữ liệu tỷ giá", style = MaterialTheme.typography.bodyMedium)
    shown.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(r.code, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
            Text("Mua ${r.price(r.buyTransfer)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("Bán ${r.price(r.sell)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FxDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val latest by c.database.fxDao().observeLatest().collectAsStateWithLifecycle(emptyList())
    val shown = latest.filter { it.code in settings.currencies }
    var selected by remember { mutableStateOf("USD") }
    val history by remember(selected) { c.database.fxDao().observeHistory(selected) }.collectAsStateWithLifecycle(emptyList())
    SubScreen("Tỷ giá Vietcombank", onBack) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item {
                shown.firstOrNull()?.sourceTime?.let {
                    Text("Bảng giá lúc ${formatTime(it, "HH:mm dd/MM/yyyy")}", style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Text("Mã", Modifier.width(48.dp), fontWeight = FontWeight.Bold)
                    listOf("Mua TM", "Mua CK", "Bán").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider()
            }
            items(shown) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(r.code, Modifier.width(48.dp), fontWeight = FontWeight.Bold)
                    listOf(r.buyCash, r.buyTransfer, r.sell).forEach {
                        Text(r.price(it), Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider()
            }
            item {
                SectionTitle("Lịch sử giá bán")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { r -> FilterChip(selected = selected == r.code, onClick = { selected = r.code }, label = { Text(r.code) }) }
                }
                val points = history.mapNotNull { h -> h.sell?.let { h.fetchedAt to it } }
                if (points.size < 2) {
                    Text("Cần ít nhất 2 lần giá thay đổi để vẽ biểu đồ.", style = MaterialTheme.typography.bodySmall)
                }
                PriceHistoryChart(points, toValue = { it / 100.0 })
                Text("${history.size} lần thay đổi đã lưu", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
