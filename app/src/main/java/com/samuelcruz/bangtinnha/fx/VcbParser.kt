package com.samuelcruz.bangtinnha.fx

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedRate(val code: String, val name: String, val buyCash: Long?, val buyTransfer: Long?, val sell: Long?)

data class ParsedRates(val time: Long?, val rates: List<ParsedRate>)

/**
 * Vietcombank's XML table:
 * `<Exrate CurrencyCode="USD" CurrencyName="US DOLLAR" Buy="26,105.00" Transfer="26,135.00" Sell="26,345.00" />`,
 * "-" where the bank does not trade (e.g. no cash buying).
 */
object VcbParser {
    private val timeFormat = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.US)

    fun parse(xml: String): ParsedRates {
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val time = doc.selectFirst("DateTime")?.text()?.let { text ->
            runCatching {
                LocalDateTime.parse(text.trim(), timeFormat).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli()
            }.getOrNull()
        }
        val rates = doc.select("Exrate").map {
            ParsedRate(
                code = it.attr("CurrencyCode").trim(),
                name = it.attr("CurrencyName").trim(),
                buyCash = hundredths(it.attr("Buy")),
                buyTransfer = hundredths(it.attr("Transfer")),
                sell = hundredths(it.attr("Sell")),
            )
        }.filter { it.code.isNotEmpty() }
        require(rates.isNotEmpty()) { "Không tìm thấy tỷ giá trong XML Vietcombank" }
        return ParsedRates(time, rates)
    }

    /** "26,345.50" → 2634550; "-" or blank → null. */
    fun hundredths(text: String): Long? {
        val clean = text.replace(",", "").trim()
        if (clean.isEmpty() || clean == "-") return null
        return BigDecimal(clean).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }
}

/** Whether [new] differs from the last stored row (so a new history row is worth adding). */
fun FxRate?.differsFrom(new: ParsedRate): Boolean =
    this == null || buyCash != new.buyCash || buyTransfer != new.buyTransfer || sell != new.sell
