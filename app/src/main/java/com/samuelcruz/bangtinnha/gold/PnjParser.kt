package com.samuelcruz.bangtinnha.gold

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ParsedGold(val code: String, val name: String, val buy: Long, val sell: Long, val order: Int)

data class ParsedGoldTable(val time: Long?, val items: List<ParsedGold>)

/**
 * PNJ's gold price API, the one www.pnj.com.vn/site/gia-vang calls (`/ecom-frontend/v1/get-gold-price?zone=00`,
 * zone 00 = TP.HCM): `{"data":[{"masp":"SJC","tensp":"Vàng miếng SJC 999.9","giaban":14060,"giamua":13760}],
 * "updateDate":"03/11/2026 14:12:37"}`, prices in thousand đồng per chỉ. (SJC's own site is behind a Cloudflare check.)
 */
object PnjParser {
    const val URL = "https://edge-api.pnj.io/ecom-frontend/v1/get-gold-price?zone=00"
    private val json = Json { ignoreUnknownKeys = true }
    private val timeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun parse(body: String): ParsedGoldTable {
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray ?: error("PNJ không trả về bảng giá vàng")
        val time = root["updateDate"]?.jsonPrimitive?.contentOrNull?.let {
            runCatching { LocalDateTime.parse(it.trim(), timeFormat).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli() }
                .getOrNull()
        }
        // Some rows are buy-only and have "giaban": "" (an empty string, not a number): those are skipped.
        val items = data.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val code = o["masp"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@mapNotNull null
            val name = o["tensp"]?.jsonPrimitive?.contentOrNull?.trim() ?: return@mapNotNull null
            val sell = o["giaban"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val buy = o["giamua"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            Triple(code, name, buy to sell)
        }
        require(items.isNotEmpty()) { "Không đọc được giá vàng nào" }
        return ParsedGoldTable(
            time,
            items.mapIndexed { i, (code, name, prices) -> ParsedGold(code, name, prices.first * 1000, prices.second * 1000, i) },
        )
    }
}
