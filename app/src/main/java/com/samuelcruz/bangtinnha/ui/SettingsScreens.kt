package com.samuelcruz.bangtinnha.ui

import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samuelcruz.bangtinnha.core.formatAge
import com.samuelcruz.bangtinnha.core.formatTime
import com.samuelcruz.bangtinnha.core.settings.AppSettings
import com.samuelcruz.bangtinnha.core.work.WorkScheduler
import com.samuelcruz.bangtinnha.weather.Place
import kotlinx.coroutines.launch

private data class SettingsEntry(val emoji: String, val colors: ModuleColors, val title: String, val subtitle: String, val route: String)

@Composable
fun SettingsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val settings by appContainer().ui.settings.collectAsStateWithLifecycle()
    val violet = Palette.settings
    val entries = listOf(
        SettingsEntry("📍", Palette.weather(1, 12), "Địa điểm thời tiết", settings.locationName, Routes.LOCATION),
        SettingsEntry("💱", Palette.fx, "Ngoại tệ hiển thị", settings.currencies.sorted().joinToString(), Routes.CURRENCIES),
        SettingsEntry("🥇", Palette.gold, "Vàng hiển thị", "${settings.goldProducts.size} loại", Routes.GOLD_PRODUCTS),
        SettingsEntry(
            "⛽", Palette.fuel, "Xăng dầu hiển thị",
            if (settings.fuelProducts.isEmpty()) "Tất cả mặt hàng" else "${settings.fuelProducts.size} mặt hàng", Routes.FUEL_PRODUCTS,
        ),
        SettingsEntry(
            "⚡", Palette.power, "Khu vực cúp điện",
            "${settings.powerUnits.size} điện lực · ${settings.powerKeywords.size} từ khoá", Routes.POWER_AREA,
        ),
        SettingsEntry("🔔", violet, "Thông báo", "Bản tin sáng lúc %02d:%02d".format(settings.morningHour, settings.morningMinute), Routes.NOTIFICATIONS),
        SettingsEntry("🔋", ModuleColors(Palette.ok, Palette.fx.end, Palette.ok), "Chạy nền & tối ưu pin", "Để app tự cập nhật và báo đúng giờ", Routes.BATTERY),
        SettingsEntry("🩺", ModuleColors(Palette.warn, Palette.fuel.end, Palette.warn), "Trạng thái nguồn", "Lần tải gần nhất, lỗi chi tiết", Routes.STATUS),
        SettingsEntry("💾", violet, "Sao lưu & khôi phục", "Xuất / nhập file database", Routes.BACKUP),
    )
    SubScreen("Cài đặt", onBack, subtitle = "Bảng Tin Nhà 1.2") { modifier ->
        LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(entries) { e ->
                Tile(Modifier.clip(RoundedCornerShape(20.dp)).clickable { onOpen(e.route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(e.emoji, e.colors, size = 40)
                        Spacer(Modifier.padding(start = 12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, style = MaterialTheme.typography.titleSmall)
                            Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.padding(bottom = 32.dp)) }
        }
    }
}

/** Search a place by name with Open-Meteo geocoding (no GPS, no location permission). */
@Composable
fun LocationScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    fun search() = scope.launch {
        message = "Đang tìm…"
        runCatching { c.weather.searchPlaces(query) }
            .onSuccess { results = it; message = if (it.isEmpty()) "Không tìm thấy địa điểm nào" else null }
            .onFailure { message = "Lỗi: ${it.message}" }
    }
    SubScreen("Địa điểm thời tiết", onBack) { modifier ->
        Column(modifier.padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Tên tỉnh, thành phố, phường…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { search() }, enabled = query.isNotBlank(), modifier = Modifier.padding(vertical = 8.dp)) { Text("Tìm") }
            message?.let { Text(it) }
            LazyColumn {
                items(results) { place ->
                    ListItem(
                        headlineContent = { Text(place.name) },
                        supportingContent = { Text("${place.description} (${"%.3f".format(place.latitude)}, ${"%.3f".format(place.longitude)})") },
                        modifier = Modifier.clickable {
                            scope.launch {
                                c.settings.setLocation(place.name, place.latitude, place.longitude)
                                c.coordinator.refreshOne("weather")
                                onBack()
                            }
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

/** A checklist of [options] backed by [selected]; [onChange] saves the new set. */
@Composable
private fun CheckList(options: List<Pair<String, String>>, selected: Set<String>, onChange: (Set<String>) -> Unit) {
    LazyColumn {
        items(options) { (key, label) ->
            val checked = key in selected
            Row(
                Modifier.fillMaxWidth().clickable { onChange(if (checked) selected - key else selected + key) }.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Text(label)
            }
        }
    }
}

@Composable
fun CurrenciesScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val codes by remember { c.database.fxDao().observeCodes() }.collectAsStateWithLifecycle(emptyList())
    val options = (codes.ifEmpty { listOf("USD", "EUR", "JPY", "CNY", "KRW") }).map { it to it }
    SubScreen("Ngoại tệ hiển thị", onBack) { modifier ->
        Column(modifier) {
            if (codes.isEmpty()) Text("Danh sách đầy đủ sẽ có sau lần tải tỷ giá đầu tiên.", Modifier.padding(16.dp))
            CheckList(options, settings.currencies) { scope.launch { c.settings.setCurrencies(it) } }
        }
    }
}

@Composable
fun FuelProductsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val products by remember { c.database.fuelDao().observeProducts() }.collectAsStateWithLifecycle(emptyList())
    SubScreen("Xăng dầu hiển thị", onBack) { modifier ->
        Column(modifier) {
            Text(
                if (products.isEmpty()) "Danh sách mặt hàng sẽ có sau lần tải giá đầu tiên." else "Không chọn mặt hàng nào = hiện tất cả.",
                Modifier.padding(16.dp),
            )
            CheckList(products.map { it.sourceKey to it.name }, settings.fuelProducts) { scope.launch { c.settings.setFuelProducts(it) } }
        }
    }
}

@Composable
fun GoldProductsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val settings by c.ui.settings.collectAsStateWithLifecycle()
    val products by remember { c.database.goldDao().observeProducts() }.collectAsStateWithLifecycle(emptyList())
    SubScreen("Vàng hiển thị", onBack, Palette.gold) { modifier ->
        Column(modifier) {
            if (products.isEmpty()) Text("Danh sách loại vàng sẽ có sau lần tải giá đầu tiên.", Modifier.padding(16.dp))
            CheckList(products.map { it.code to it.name }, settings.goldProducts) { scope.launch { c.settings.setGoldProducts(it) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by c.ui.settings.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf(false) }

    @Composable
    fun Toggle(title: String, subtitle: String, on: Boolean, set: suspend (Boolean) -> Unit) = ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = on, onCheckedChange = { scope.launch { set(it) } }) },
    )

    SubScreen("Thông báo", onBack) { modifier ->
        Column(modifier) {
            if (!c.notifications.canPost()) {
                Text(
                    "Thông báo đang bị tắt cho app. Bật lại trong Cài đặt của điện thoại.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Toggle("Bản tin buổi sáng", "Một thông báo tóm tắt mỗi sáng", s.notifyMorning) { c.settings.setNotifyMorning(it) }
            ListItem(
                headlineContent = { Text("Giờ gửi bản tin") },
                supportingContent = { Text("Android có thể gửi trễ vài phút để tiết kiệm pin") },
                trailingContent = { Text("%02d:%02d".format(s.morningHour, s.morningMinute), fontWeight = FontWeight.Bold) },
                modifier = Modifier.clickable { picking = true },
            )
            Toggle("Cúp điện", "Báo ngay khi có lịch cúp điện ở khu vực của bạn trong 24 giờ tới", s.notifyPower) {
                c.settings.setNotifyPower(it)
            }
            Toggle("Giá xăng", "Báo khi giá xăng dầu thay đổi", s.notifyFuel) { c.settings.setNotifyFuel(it) }
            Toggle("Thời tiết xấu", "Báo khi sắp có dông hoặc mưa to trong 6 giờ tới", s.notifyWeather) { c.settings.setNotifyWeather(it) }
        }
    }
    if (picking) {
        val state = rememberTimePickerState(initialHour = s.morningHour, initialMinute = s.morningMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    scope.launch {
                        c.settings.setMorningTime(state.hour, state.minute)
                        WorkScheduler.scheduleMorning(context, state.hour, state.minute, replace = true)
                    }
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Huỷ") } },
            text = { TimePicker(state = state) },
        )
    }
}

/** Why background work may stop, and buttons to the right system pages. */
@Composable
fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val power = context.getSystemService(PowerManager::class.java)
    var ignoring by remember { mutableStateOf(power.isIgnoringBatteryOptimizations(context.packageName)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { ignoring = power.isIgnoringBatteryOptimizations(context.packageName) }
    val maker = Build.MANUFACTURER.lowercase()
    val tip = when {
        "xiaomi" in maker || "redmi" in maker || "poco" in maker ->
            "Xiaomi/Redmi: Cài đặt → Ứng dụng → Bảng Tin Nhà → bật \"Tự khởi động\" và chọn Tiết kiệm pin = \"Không giới hạn\"."
        "oppo" in maker || "realme" in maker || "oneplus" in maker ->
            "Oppo/Realme: Cài đặt → Pin → Bảng Tin Nhà → cho phép \"Chạy ở chế độ nền\" và \"Tự khởi chạy\"."
        "vivo" in maker ->
            "Vivo: i Manager → Quản lý ứng dụng → Tự khởi động: bật cho Bảng Tin Nhà; Pin → Tiêu thụ điện nền cao: cho phép."
        "samsung" in maker ->
            "Samsung: Cài đặt → Pin → Giới hạn sử dụng nền: bỏ Bảng Tin Nhà khỏi \"Ứng dụng ngủ\" và \"Ứng dụng ngủ sâu\"."
        else -> "Tìm trong Cài đặt → Pin mục cho phép ứng dụng chạy nền, và bật cho Bảng Tin Nhà."
    }
    SubScreen("Chạy nền & tối ưu pin", onBack) { modifier ->
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "App tự cập nhật mỗi giờ và gửi bản tin sáng bằng WorkManager. Android (nhất là Xiaomi, Oppo, Vivo, Samsung) " +
                    "hay dừng các app chạy nền để tiết kiệm pin, làm thông báo đến trễ hoặc không đến.",
            )
            Tile {
                Text(
                    if (ignoring) "✅ App đã được bỏ qua tối ưu pin." else "⚠ App đang bị tối ưu pin: cập nhật nền có thể bị hoãn.",
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!ignoring) {
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri()),
                    )
                }) { Text("Tắt tối ưu pin cho app") }
            }
            OutlinedButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            }) { Text("Mở trang thông tin ứng dụng") }
            Text("Máy của bạn (${Build.MANUFACTURER}):", fontWeight = FontWeight.Bold)
            Text(tip)
        }
    }
}

/** Per-source status for debugging: last success, last attempt, last error and summary. */
@Composable
fun SourceStatusScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val statuses by c.ui.statuses.collectAsStateWithLifecycle()
    val byId = statuses.associateBy { it.sourceId }
    SubScreen("Trạng thái nguồn", onBack) { modifier ->
        LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(c.coordinator.sources) { source ->
                val st = byId[source.id]
                Tile {
                    Column {
                        Text(source.title, fontWeight = FontWeight.Bold)
                        Text("Chu kỳ: ${source.refreshInterval.inWholeHours} giờ", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Thành công: " + (st?.lastSuccessAt?.let { "${formatTime(it, "HH:mm dd/MM")} (${formatAge(it)} trước)" } ?: "chưa"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Lần thử: " + (st?.lastAttemptAt?.let { formatTime(it, "HH:mm:ss dd/MM") } ?: "chưa"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        st?.lastSummary?.let { Text("Kết quả: $it", style = MaterialTheme.typography.bodySmall) }
                        st?.lastError?.let { Text("Lỗi: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { scope.launch { c.coordinator.refreshOne(source.id) } }) { Text("Tải lại nguồn này") }
                    }
                }
            }
            item {
                Text("Dung lượng database: ${c.maintenance.databaseSize() / 1024} KB (giới hạn sao lưu tự động 25 MB)")
                Spacer(Modifier.padding(bottom = 24.dp))
            }
        }
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.sqlite3")) { uri ->
        if (uri != null) scope.launch {
            message = runCatching { c.maintenance.export(uri) }.fold({ "Đã xuất database." }, { "Lỗi: ${it.message}" })
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    SubScreen("Sao lưu & khôi phục", onBack) { modifier ->
        Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Android tự sao lưu database và cài đặt lên Google Drive của bạn (Auto Backup, cần bật sao lưu trong Cài đặt " +
                    "điện thoại). Bạn cũng có thể tự xuất file để cất giữ hoặc chuyển sang máy khác.",
            )
            Text("Dung lượng hiện tại: ${c.maintenance.databaseSize() / 1024} KB")
            Button(onClick = { export.launch("bang_tin_nha_${formatTime(System.currentTimeMillis(), "yyyyMMdd")}.db") }) {
                Text("Xuất file database")
            }
            OutlinedButton(onClick = { import.launch(arrayOf("*/*")) }) { Text("Nhập file database") }
            message?.let { Text(it) }
        }
    }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Thay toàn bộ dữ liệu?") },
            text = { Text("Dữ liệu hiện tại sẽ bị thay bằng file đã chọn, rồi app tự khởi động lại.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { runCatching { c.maintenance.import(uri) }.onFailure { message = "Lỗi: ${it.message}" } }
                }) { Text("Thay thế") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Huỷ") } },
        )
    }
}
