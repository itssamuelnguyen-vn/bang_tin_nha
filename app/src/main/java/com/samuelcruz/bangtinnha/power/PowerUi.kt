package com.samuelcruz.bangtinnha.power

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
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

private val weekdays = listOf("thứ Hai", "thứ Ba", "thứ Tư", "thứ Năm", "thứ Sáu", "thứ Bảy", "Chủ nhật")

/** "08:00–17:00 · thứ Tư 04/11". */
private fun PowerOutage.timeText(): String {
    val day = Instant.ofEpochMilli(startAt).atZone(VN_ZONE).dayOfWeek.value
    return "${formatTime(startAt, "HH:mm")}–${formatTime(endAt, "HH:mm")} · ${weekdays[day - 1]} ${formatTime(startAt, "dd/MM")}"
}

@Composable
fun PowerCardContent() {
    val c = appContainer()
    val settings by c.settings.settings.collectAsStateWithLifecycle(AppSettings())
    val now = remember { System.currentTimeMillis() }
    val upcoming by c.database.powerDao().observeUpcoming(now).collectAsStateWithLifecycle(emptyList())
    val mine = upcoming.forMyArea(settings, System.currentTimeMillis())
    when {
        settings.powerUnits.isEmpty() -> Text("Chọn điện lực trong Cài đặt để xem lịch cúp điện.")
        mine.isEmpty() -> Text(
            if (settings.powerKeywords.isEmpty()) "Không có lịch cúp điện sắp tới" else "Khu vực của bạn không có lịch cúp điện sắp tới",
            style = MaterialTheme.typography.bodyMedium,
        )
        else -> {
            Text("${mine.size} lịch sắp tới", fontWeight = FontWeight.Bold)
            mine.take(2).forEach {
                Text("⚡ ${it.timeText()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                Text(it.area.lines().first(), style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
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
    SubScreen("Lịch cúp điện", onBack) { modifier ->
        LazyColumn(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Điện lực: " + settings.powerUnits.joinToString { it.name }.ifEmpty { "chưa chọn" },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (settings.powerKeywords.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = onlyMine, onClick = { onlyMine = true }, label = { Text("Khu vực của tôi") })
                        FilterChip(selected = !onlyMine, onClick = { onlyMine = false }, label = { Text("Tất cả") })
                    }
                }
                if (shown.isEmpty()) Text("Không có lịch ngừng cung cấp điện sắp tới.", modifier = Modifier.padding(vertical = 16.dp))
            }
            items(shown) { o ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚡ ${o.timeText()}", fontWeight = FontWeight.Bold)
                        Text(o.area, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        if (o.reason.isNotEmpty()) Text("Lý do: ${o.reason}", style = MaterialTheme.typography.bodySmall)
                        Text(o.unitName, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item { Spacer(Modifier.padding(bottom = 24.dp)) }
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

    SubScreen("Khu vực cúp điện", onBack) { modifier ->
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
