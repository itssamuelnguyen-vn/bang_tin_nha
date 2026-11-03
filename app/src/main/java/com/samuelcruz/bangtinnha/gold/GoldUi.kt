package com.samuelcruz.bangtinnha.gold

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatDong
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.ui.ChangeBadge
import com.samuelcruz.bangtinnha.ui.Palette
import com.samuelcruz.bangtinnha.ui.PriceHistoryChart
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.Tile
import com.samuelcruz.bangtinnha.ui.appContainer

/** The products the user chose, in PNJ's order (SJC first when none chosen). */
fun List<GoldLatest>.chosen(settings: AppSettings): List<GoldLatest> =
    filter { it.code in settings.goldProducts }.ifEmpty { take(2) }

/** "14,06 triệu" for 14,060,000 đồng. */
fun millions(dong: Long): String = "%.2f".format(dong / 1_000_000.0).replace('.', ',') + " tr"

@Composable
fun GoldCardContent() {
    val c = appContainer()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val latest by c.ui.goldLatest.collectAsStateWithLifecycle()
    val shown = latest.chosen(settings)
    if (shown.isEmpty()) return Text("Chưa có dữ liệu giá vàng", style = MaterialTheme.typography.bodyMedium)
    shown.forEachIndexed { i, g ->
        Row(
            Modifier.fillMaxWidth().padding(top = if (i == 0) 0.dp else 8.dp).clip(RoundedCornerShape(20.dp))
                .background(Palette.gold.accent.copy(alpha = if (i == 0) 0.14f else 0.06f)).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (i == 0) "🥇" else "💍", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text("Mua ${g.buy?.let(::millions) ?: "–"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(g.sell?.let(::millions) ?: "–", style = MaterialTheme.typography.titleLarge, color = Palette.gold.accent)
                ChangeBadge(g.sell, g.previousSell, ::formatDong)
            }
        }
    }
    shown.firstNotNullOfOrNull { it.sourceTime }?.let {
        Text(
            "Giá bán ra / chỉ · PNJ cập nhật ${formatTime(it, "HH:mm dd/MM")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun GoldDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val latest by c.ui.goldLatest.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<Long?>(null) }
    val productId = selected ?: latest.firstOrNull()?.productId
    val history by remember(productId) { c.database.goldDao().observeHistory(productId ?: -1) }.collectAsStateWithLifecycle(emptyList())
    SubScreen(
        "Giá vàng PNJ",
        onBack,
        Palette.gold,
        subtitle = latest.firstNotNullOfOrNull { it.sourceTime }?.let { "Bảng giá TP.HCM · cập nhật ${formatTime(it, "HH:mm dd/MM")} · đồng/chỉ" },
    ) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(latest) { g ->
                Tile {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.name, style = MaterialTheme.typography.titleSmall)
                            Text("Mua ${g.buy?.let(::formatDong) ?: "–"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(g.sell?.let(::formatDong) ?: "–", style = MaterialTheme.typography.titleMedium, color = Palette.gold.accent)
                            ChangeBadge(g.sell, g.previousSell, ::formatDong)
                        }
                    }
                }
            }
            item {
                SectionTitle("Lịch sử giá bán")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    latest.forEach { g -> FilterChip(selected = productId == g.productId, onClick = { selected = g.productId }, label = { Text(g.code) }) }
                }
                Spacer(Modifier.height(8.dp))
                Tile {
                    if (history.size < 2) Text("Biểu đồ sẽ hiện khi giá thay đổi ít nhất một lần.", style = MaterialTheme.typography.bodySmall)
                    PriceHistoryChart(history.map { it.fetchedAt to it.sell }, toValue = { it / 1_000_000.0 })
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
