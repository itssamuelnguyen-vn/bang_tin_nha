package com.samuelcruz.bangtinnha.gold

import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import kotlin.time.Duration.Companion.hours

/** PNJ gold prices (TP.HCM board, the nearest to Tây Ninh). */
class GoldSource(private val http: Http, private val dao: GoldDao, private val now: () -> Long = System::currentTimeMillis) :
    DataSource {
    override val id = "gold"
    override val title = "Giá vàng PNJ"
    override val refreshInterval = 4.hours

    override suspend fun refresh(): RefreshResult {
        val table = PnjParser.parse(http.get(PnjParser.URL, id))
        val fetchedAt = now()
        var changed = 0
        for (item in table.items) {
            val existing = dao.product(item.code)
            val productId = when {
                existing == null -> dao.insertProduct(GoldProduct(code = item.code, name = item.name, displayOrder = item.order))
                existing.name != item.name || existing.displayOrder != item.order -> {
                    dao.updateProduct(existing.copy(name = item.name, displayOrder = item.order))
                    existing.id
                }
                else -> existing.id
            }
            val last = dao.latestPrice(productId)
            if (last == null || last.buy != item.buy || last.sell != item.sell) {
                dao.insertPrice(GoldPrice(productId = productId, buy = item.buy, sell = item.sell, sourceTime = table.time, fetchedAt = fetchedAt))
                changed++
            }
        }
        return RefreshResult("${table.items.size} loại vàng, $changed đổi giá")
    }
}
