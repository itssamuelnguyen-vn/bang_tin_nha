package com.samuelcruz.bangtinnha.fx

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatHundredths
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.ui.ChangeBadge
import com.samuelcruz.bangtinnha.ui.Palette
import com.samuelcruz.bangtinnha.ui.PriceHistoryChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.Tile
import com.samuelcruz.bangtinnha.ui.appContainer

/** Flag emoji from the first two letters of an ISO currency code (USD → 🇺🇸, EUR → 🇪🇺). */
fun flagOf(code: String): String {
    if (code.length < 2) return "💱"
    return code.take(2).uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

private fun price(v: Long?) = v?.let(::formatHundredths) ?: "–"

@Composable
fun FxCardContent() {
    val c = appContainer()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val latest by c.ui.fxLatest.collectAsStateWithLifecycle()
    val shown = latest.filter { it.code in settings.currencies }
    if (shown.isEmpty()) return Text("Chưa có dữ liệu tỷ giá", style = MaterialTheme.typography.bodyMedium)
    val main = shown.firstOrNull { it.code == "USD" } ?: shown.first()

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.fx.accent.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(flagOf(main.code), fontSize = 34.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(main.code, style = MaterialTheme.typography.titleMedium)
            Text("Mua CK ${price(main.buyTransfer)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Bán ra", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(price(main.sell), style = MaterialTheme.typography.headlineSmall, color = Palette.fx.accent)
            ChangeBadge(main.sell, main.previousSell, ::formatHundredths)
        }
    }
    shown.filter { it != main }.forEach { r ->
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(flagOf(r.code), fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text(r.code, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            ChangeBadge(r.sell, r.previousSell, ::formatHundredths)
            Spacer(Modifier.width(8.dp))
            Text(price(r.sell), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
fun FxDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val latest by c.ui.fxLatest.collectAsStateWithLifecycle()
    val shown = latest.filter { it.code in settings.currencies }
    var selected by remember { mutableStateOf("USD") }
    val history by remember(selected) { c.database.fxDao().observeHistory(selected) }.collectAsStateWithLifecycle(emptyList())
    SubScreen(
        title = "Tỷ giá Vietcombank",
        subtitle = shown.firstOrNull()?.sourceTime?.let { "Bảng giá lúc ${formatTime(it, "HH:mm dd/MM/yyyy")}" },
        colors = Palette.fx,
        onBack = onBack,
    ) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shown) { r ->
                Tile {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(flagOf(r.code), fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.code, style = MaterialTheme.typography.titleMedium)
                            Text(r.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(price(r.sell), style = MaterialTheme.typography.titleLarge, color = Palette.fx.accent)
                            ChangeBadge(r.sell, r.previousSell, ::formatHundredths)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text("Mua TM ${price(r.buyCash)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("Mua CK ${price(r.buyTransfer)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
            item {
                SectionTitle("Lịch sử giá bán")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { r ->
                        FilterChip(selected = selected == r.code, onClick = { selected = r.code }, label = { Text("${flagOf(r.code)} ${r.code}") })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Tile {
                    val points = history.mapNotNull { h -> h.sell?.let { h.fetchedAt to it } }
                    if (points.size < 2) {
                        Text("Biểu đồ sẽ hiện khi giá thay đổi ít nhất một lần (đã lưu ${history.size} mức giá).", style = MaterialTheme.typography.bodySmall)
                    }
                    PriceHistoryChart(points, toValue = { it / 100.0 })
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
