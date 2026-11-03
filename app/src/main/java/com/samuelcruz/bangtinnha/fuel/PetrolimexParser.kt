package com.samuelcruz.bangtinnha.fuel

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull
import java.time.Instant
import java.util.Base64

data class ParsedFuel(val key: String, val name: String, val zone1Price: Long, val effectiveAt: Long, val order: Int)

/**
 * Petrolimex's retail price list. The website draws its "Giá bán lẻ xăng dầu" box with JavaScript from this JSON API
 * (found in portals.petrolimex.com.vn/_themes/sunrise/js/all.js, `__vieapps.prices.fetch`); the filter below is the
 * one the page sends.
 */
object PetrolimexParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun url(): String {
        val filter = """{"FilterBy":{"And":[{"SystemID":{"Equals":"6783dc1271ff449e95b74a9520964169"}},""" +
            """{"RepositoryID":{"Equals":"a95451e23b474fe5886bfb7cf843f53c"}},""" +
            """{"RepositoryEntityID":{"Equals":"3801378fe1e045b1afa10de7c5776124"}},""" +
            """{"Status":{"Equals":"Published"}}]},"SortBy":{"LastModified":"Descending"},""" +
            """"Pagination":{"TotalRecords":-1,"TotalPages":0,"PageSize":0,"PageNumber":0}}"""
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(filter.toByteArray())
        return "https://portals.petrolimex.com.vn/~apis/portals/cms.item/search?x-request=$encoded"
    }

    fun parse(body: String): List<ParsedFuel> {
        val objects = json.parseToJsonElement(body).jsonObject["Objects"]?.jsonArray
            ?: error("Không có danh sách giá trong phản hồi Petrolimex")
        val products = objects.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            fun text(name: String) = o[name]?.jsonPrimitive?.content
            val price = o["Zone1Price"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            ParsedFuel(
                key = text("ID") ?: return@mapNotNull null,
                name = text("Title")?.trim() ?: return@mapNotNull null,
                zone1Price = price,
                effectiveAt = text("LastModified")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0,
                // The field really is spelt "DIsplayOrder" in their data.
                order = (o["DIsplayOrder"] ?: o["DisplayOrder"] ?: o["OrderIndex"])?.jsonPrimitive?.intOrNull ?: 99,
            )
        }
        require(products.isNotEmpty()) { "Không đọc được giá xăng dầu nào" }
        return products.sortedBy { it.order }
    }
}
