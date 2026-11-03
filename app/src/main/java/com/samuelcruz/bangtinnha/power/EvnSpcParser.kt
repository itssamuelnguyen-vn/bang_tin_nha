package com.samuelcruz.bangtinnha.power

import com.samuelcruz.bangtinnha.core.settings.PowerUnit
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ParsedOutage(val area: String, val startAt: Long, val endAt: Long, val reason: String)

data class ParsedOutages(val unitName: String?, val outages: List<ParsedOutage>)

/**
 * EVNSPC (Tổng công ty Điện lực miền Nam) customer site, www.cskh.evnspc.vn:
 * - /TraCuu/LichNgungGiamCungCapDien: the page; its `#idCongTyDienLuc` select lists the provincial companies (PB05 = Tây Ninh)
 * - /TraCuu/GetDanhMucDienLuc?pMA_DVICTREN=PB05: `<option>` list of the company's local units (PB0501…)
 * - /TraCuu/GetThongTinLichNgungGiamCungCapDien?madvi=PB0501&tuNgay=dd-MM-yyyy&denNgay=…&ChucNang=MaDonVi:
 *   HTML with one `div.entry` per outage (`span.where`, `span.time`, `span.cause`), or a "không có lịch" notice.
 */
object EvnSpcParser {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val timeRegex = Regex(
        """Từ\s+(\d{1,2}:\d{2})(?::\d{2})?\s+ngày\s+(\d{1,2}/\d{1,2}/\d{4})\s+đến\s+(\d{1,2}:\d{2})(?::\d{2})?\s+ngày\s+(\d{1,2}/\d{1,2}/\d{4})""",
    )
    private val dateTime = DateTimeFormatter.ofPattern("H:mm d/M/yyyy")

    fun parseOutages(html: String): ParsedOutages {
        val doc = Jsoup.parse(html)
        val unitName = doc.select("p.ttl span").map { it.text() }.firstOrNull { it.startsWith("Đơn vị:") }
            ?.removePrefix("Đơn vị:")?.trim()?.let(::prettyUnitName)
        val entries = doc.select("div.entry")
        if (entries.isEmpty()) {
            if (doc.text().contains("không có lịch", ignoreCase = true)) return ParsedOutages(unitName, emptyList())
            error("Trang EVNSPC không có mục lịch nào và cũng không báo 'không có lịch': có thể đã đổi giao diện")
        }
        val outages = entries.map { entry ->
            val area = textWithLines(entry.selectFirst("span.where")).removePrefix("KHU VỰC:").trim()
            val time = entry.selectFirst("span.time")?.text().orEmpty()
            val m = timeRegex.find(time) ?: error("Không đọc được thời gian: \"$time\"")
            ParsedOutage(
                area = area,
                startAt = epoch(m.groupValues[1], m.groupValues[2]),
                endAt = epoch(m.groupValues[3], m.groupValues[4]),
                reason = entry.selectFirst("span.cause > span")?.text()?.trim().orEmpty(),
            )
        }
        return ParsedOutages(unitName, outages)
    }

    /** Provincial companies from the main page's select. */
    fun parseCompanies(html: String): List<PowerUnit> =
        Jsoup.parse(html).select("#idCongTyDienLuc option").mapNotNull { option ->
            val code = option.attr("value").trim()
            if (code.isEmpty()) null else PowerUnit(code, option.text().trim())
        }

    /** Local units of one company (the options returned by GetDanhMucDienLuc). */
    fun parseUnits(html: String): List<PowerUnit> =
        Jsoup.parse(html).select("option").mapNotNull { option ->
            val code = option.attr("value").trim()
            if (code.isEmpty()) null else PowerUnit(code, prettyUnitName(option.text()))
        }

    /** "ĐIỆN LỰC TÂN NINH" → "Điện lực Tân Ninh". */
    fun prettyUnitName(raw: String): String {
        val words = raw.trim().lowercase().split(Regex("\\s+")).map { w -> w.replaceFirstChar { it.uppercase() } }
        return words.joinToString(" ").replace("Điện Lực", "Điện lực")
    }

    private fun epoch(time: String, date: String): Long =
        LocalDateTime.parse("$time $date", dateTime).atZone(zone).toInstant().toEpochMilli()

    /** Text of [element] keeping its line breaks (Jsoup's text() would join the khu phố into one line). */
    private fun textWithLines(element: Element?): String {
        if (element == null) return ""
        element.select("b").remove()
        return element.wholeText().lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }
}
