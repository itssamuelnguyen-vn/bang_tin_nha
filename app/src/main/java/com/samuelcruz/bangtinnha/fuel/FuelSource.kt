package com.samuelcruz.bangtinnha.fuel

import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import kotlin.time.Duration.Companion.hours

/** Petrolimex retail fuel prices, zone 1. */
class FuelSource(private val http: Http, private val dao: FuelDao, private val now: () -> Long = System::currentTimeMillis) :
    DataSource {
    override val id = "fuel"
    override val title = "Giá xăng dầu Petrolimex"
    override val refreshInterval = 12.hours

    override suspend fun refresh(): RefreshResult {
        val parsed = PetrolimexParser.parse(
            http.get(PetrolimexParser.url(), id, headers = mapOf("Referer" to "https://www.petrolimex.com.vn/")),
        )
        val fetchedAt = now()
        var changed = 0
        for (item in parsed) {
            val existing = dao.product(item.key)
            val productId = when {
                existing == null -> dao.insertProduct(FuelProduct(sourceKey = item.key, name = item.name, displayOrder = item.order))
                existing.name != item.name || existing.displayOrder != item.order -> {
                    dao.updateProduct(existing.copy(name = item.name, displayOrder = item.order))
                    existing.id
                }
                else -> existing.id
            }
            if (dao.latestPrice(productId)?.price != item.zone1Price) {
                dao.insertPrice(FuelPrice(productId = productId, price = item.zone1Price, effectiveAt = item.effectiveAt, fetchedAt = fetchedAt))
                changed++
            }
        }
        return RefreshResult("${parsed.size} mặt hàng, $changed đổi giá")
    }
}
