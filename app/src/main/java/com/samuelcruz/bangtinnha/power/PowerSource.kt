package com.samuelcruz.bangtinnha.power

import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.settings.PowerUnit
import com.samuelcruz.bangtinnha.core.settings.SettingsRepository
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours

/** EVNSPC planned outages for the power units chosen in settings, today and the next 4 days (as the site shows). */
class PowerSource(
    private val http: Http,
    private val settings: SettingsRepository,
    private val dao: PowerDao,
    private val now: () -> Long = System::currentTimeMillis,
) : DataSource {
    override val id = "power"
    override val title = "Lịch cúp điện EVNSPC"
    override val refreshInterval = 24.hours

    override suspend fun refresh(): RefreshResult {
        val units = settings.current().powerUnits
        if (units.isEmpty()) return RefreshResult("Chưa chọn điện lực nào trong Cài đặt")
        val today = LocalDate.now(ZONE)
        val format = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        val from = today.format(format)
        val to = today.plusDays(4).format(format)
        val fetchedAt = now()
        val windowStart = today.atStartOfDay(ZONE).toInstant().toEpochMilli()
        var total = 0
        units.forEachIndexed { i, unit ->
            if (i > 0) delay(1_000) // one request at a time, gently
            val parsed = EvnSpcParser.parseOutages(
                http.get("$BASE/TraCuu/GetThongTinLichNgungGiamCungCapDien?madvi=${unit.code}&tuNgay=$from&denNgay=$to&ChucNang=MaDonVi", id),
            )
            val fresh = parsed.outages.map {
                PowerOutage(
                    unitCode = unit.code,
                    unitName = parsed.unitName ?: unit.name,
                    area = it.area,
                    startAt = it.startAt,
                    endAt = it.endAt,
                    reason = it.reason,
                    firstSeenAt = fetchedAt,
                )
            }
            // Outages from today on that the site no longer lists were cancelled.
            val keys = fresh.map { Triple(it.startAt, it.endAt, it.area) }.toSet()
            val cancelled = dao.futureOf(unit.code, windowStart).filter { Triple(it.startAt, it.endAt, it.area) !in keys }
            if (cancelled.isNotEmpty()) dao.delete(cancelled.map { it.id })
            dao.insertAll(fresh)
            total += fresh.size
        }
        dao.deleteOtherUnits(units.map { it.code })
        dao.deleteEndedBefore(fetchedAt - 3L * 24 * 3600 * 1000)
        return RefreshResult("${units.size} điện lực, $total lịch ngừng điện")
    }

    suspend fun fetchCompanies(): List<PowerUnit> =
        EvnSpcParser.parseCompanies(http.get("$BASE/TraCuu/LichNgungGiamCungCapDien", "power-companies"))

    suspend fun fetchUnits(company: String): List<PowerUnit> =
        EvnSpcParser.parseUnits(http.get("$BASE/TraCuu/GetDanhMucDienLuc?pMA_DVICTREN=$company", "power-units"))

    companion object {
        const val BASE = "https://www.cskh.evnspc.vn"
        val ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
    }
}
