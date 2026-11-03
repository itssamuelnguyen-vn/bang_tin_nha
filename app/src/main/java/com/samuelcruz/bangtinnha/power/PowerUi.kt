package com.samuelcruz.bangtinnha.power

import androidx.compose.foundation.clickable
import com.samuelcruz.bangtinnha.ui.Tile
import com.samuelcruz.bangtinnha.ui.Palette
import com.samuelcruz.bangtinnha.ui.Pill
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.VN_ZONE
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.notify.forMyArea
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.core.settings.PowerUnit
import com.samuelcruz.bangtinnha.ui.SectionTitle
import com.samuelcruz.bangtinnha.ui.SubScreen
import com.samuelcruz.bangtinnha.ui.appContainer
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val weekdays = listOf("thứ Hai", "thứ Ba", "thứ Tư", "thứ Năm", "thứ Sáu", "thứ Bảy", "Chủ nhật")

/** "08:00–17:00 · thứ Tư 04/11". */
private fun PowerOutage.timeText(): String = "${hoursText()} · ${dayText()}"

/** "08:00–17:00". */
private fun PowerOutage.hoursText(): String = "${formatTime(startAt, "HH:mm")}–${formatTime(endAt, "HH:mm")}"

/** "thứ Tư 04/11". */
private fun PowerOutage.dayText(): String {
    val day = Instant.ofEpochMilli(startAt).atZone(VN_ZONE).dayOfWeek.value
    return "${weekdays[day - 1]} ${formatTime(startAt, "dd/MM")}"
}

/** "Hôm nay", "Ngày mai", "Còn 3 ngày". */
private fun PowerOutage.countdown(): String {
    val today = LocalDate.now(VN_ZONE)
    val day = Instant.ofEpochMilli(startAt).atZone(VN_ZONE).toLocalDate()
    return when (val n = ChronoUnit.DAYS.between(today, day)) {
        0L -> "Hôm nay"
        1L -> "Ngày mai"
        else -> "Còn $n ngày"
    }
}

@Composable
fun PowerCardContent() {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val now = remember { System.currentTimeMillis() }
    val upcoming by c.database.powerDao().observeUpcoming(now).collectAsStateWithLifecycle(emptyList())
    val mine = upcoming.forMyArea(settings, System.currentTimeMillis())
    when {
        settings.powerUnits.isEmpty() ->
            Notice("🛠️", "Chọn điện lực trong Cài đặt để xem lịch cúp điện", MaterialTheme.colorScheme.surfaceContainerHigh)
        mine.isEmpty() -> Notice(
            "✅",
            if (settings.powerKeywords.isEmpty()) "Không có lịch cúp điện sắp tới" else "Khu vực của bạn không có lịch cúp điện sắp tới",
            Palette.ok.copy(alpha = 0.14f),
        )
        else -> {
            val next = mine.first()
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.power.gradient).padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SẮP CÚP ĐIỆN", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                    Pill(next.countdown(), Color.White.copy(alpha = 0.22f), Color.White)
                }
                Text(next.hoursText(), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text(next.dayText().replaceFirstChar { it.uppercase() }, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text("📍 " + next.area.lines().first(), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium, maxLines = 2, modifier = Modifier.padding(top = 6.dp))
            }
            if (mine.size > 1) {
                Pill(
                    "Xem thêm ${mine.size - 1} lịch ›",
                    Palette.power.accent.copy(alpha = 0.15f),
                    Palette.power.accent,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun Notice(icon: String, text: String, background: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(background).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PowerDetailScreen(onBack: () -> Unit) {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val now = remember { System.currentTimeMillis() }
    val upcoming by c.database.powerDao().observeUpcoming(now).collectAsStateWithLifecycle(emptyList())
    var onlyMine by remember { mutableStateOf(true) }
    val shown = if (onlyMine) upcoming.forMyArea(settings, now) else upcoming
    SubScreen(
        "Lịch cúp điện",
        onBack,
        Palette.power,
        subtitle = settings.powerUnits.joinToString { it.name }.ifEmpty { "Chưa chọn điện lực" },
    ) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                if (settings.powerKeywords.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = onlyMine, onClick = { onlyMine = true }, label = { Text("Khu vực của tôi") })
                        FilterChip(selected = !onlyMine, onClick = { onlyMine = false }, label = { Text("Tất cả (${upcoming.size})") })
                    }
                }
                if (shown.isEmpty()) Notice("✅", "Không có lịch ngừng cung cấp điện sắp tới.", Palette.ok.copy(alpha = 0.14f))
            }
            items(shown) { o ->
                Tile {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Palette.power.accent))
                        Spacer(Modifier.width(8.dp))
                        Text(o.timeText(), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(o.area, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                    if (o.reason.isNotEmpty()) {
                        Text("Lý do: ${o.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(o.unitName, style = MaterialTheme.typography.labelSmall, color = Palette.power.accent)
                }
            }
            item { Spacer(Modifier.padding(bottom = 32.dp)) }
        }
    }
}

/** Company, local units and area keywords for the outage module. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PowerSettingsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    var companies by remember { mutableStateOf<List<PowerUnit>>(emptyList()) }
    var units by remember { mutableStateOf<List<PowerUnit>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var keywords by remember(settings.powerKeywords) { mutableStateOf(settings.powerKeywords.joinToString("\n")) }

    LaunchedEffect(Unit) {
        runCatching { c.power.fetchCompanies() }.onSuccess { companies = it }.onFailure { error = it.message }
    }
    LaunchedEffect(settings.powerCompany) {
        runCatching { c.power.fetchUnits(settings.powerCompany) }.onSuccess { units = it }.onFailure { error = it.message }
    }

    SubScreen("Khu vực cúp điện", onBack, Palette.power) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item {
                error?.let { Text("Không tải được danh sách từ EVNSPC: $it", color = MaterialTheme.colorScheme.error) }
                SectionTitle("Công ty điện lực (EVNSPC)")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(companies.firstOrNull { it.code == settings.powerCompany }?.name ?: settings.powerCompany)
                }
                if (companies.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        companies.forEach { co ->
                            FilterChip(
                                selected = co.code == settings.powerCompany,
                                onClick = { scope.launch { c.settings.setPowerCompany(co.code) } },
                                label = { Text(co.name.removePrefix("Công ty Điện lực ")) },
                            )
                        }
                    }
                }
                SectionTitle("Điện lực khu vực (chọn nơi bạn ở)")
                Text(
                    "Tây Ninh mới gồm cả Long An cũ: các mã PB05xx là Tây Ninh cũ, PB06xx là Long An cũ. Mỗi điện lực chọn thêm là một lần tải mỗi ngày.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            items(units) { unit ->
                val checked = settings.powerUnits.any { it.code == unit.code }
                Row(
                    Modifier.fillMaxWidth().clickable {
                        val next = if (checked) settings.powerUnits.filterNot { it.code == unit.code } else settings.powerUnits + unit
                        scope.launch { c.settings.setPowerUnits(next) }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Text("${unit.name} (${unit.code})")
                }
            }
            item {
                SectionTitle("Từ khoá khu vực")
                Text(
                    "Mỗi dòng một từ khoá: tên phường/xã, tên đường, khu phố… Ghi cả tên cũ lẫn tên mới (sau sắp xếp đơn vị hành chính 2025). " +
                        "So khớp không phân biệt hoa thường và dấu. Để trống = hiện tất cả lịch của các điện lực đã chọn.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    minLines = 4,
                    placeholder = { Text("Bình Minh\nNinh Thành\nphường 3") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(onClick = { scope.launch { c.settings.setPowerKeywords(keywords.lines()) } }) { Text("Lưu từ khoá") }
                    TextButton(onClick = { scope.launch { c.coordinator.refreshOne("power") } }) { Text("Tải lịch ngay") }
                }
                Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}
