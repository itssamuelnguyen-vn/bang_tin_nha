package com.samuelcruz.bangtinnha.fx

import com.samuelcruz.bangtinnha.core.net.Http
import com.samuelcruz.bangtinnha.core.source.DataSource
import com.samuelcruz.bangtinnha.core.source.RefreshResult
import kotlin.time.Duration.Companion.hours

/** Vietcombank exchange rates (their XML asks for at most one request every 5 minutes; we ask every 6 hours). */
class FxSource(private val http: Http, private val dao: FxDao, private val now: () -> Long = System::currentTimeMillis) :
    DataSource {
    override val id = "fx"
    override val title = "Tỷ giá Vietcombank"
    override val refreshInterval = 6.hours

    override suspend fun refresh(): RefreshResult {
        val parsed = VcbParser.parse(http.get(URL, id))
        val fetchedAt = now()
        var changed = 0
        for (rate in parsed.rates) {
            val currencyId = dao.currency(rate.code)?.id ?: dao.insertCurrency(Currency(code = rate.code, name = rate.name))
            if (dao.latest(currencyId).differsFrom(rate)) {
                dao.insertRate(FxRate(0, currencyId, fetchedAt, parsed.time, rate.buyCash, rate.buyTransfer, rate.sell))
                changed++
            }
        }
        return RefreshResult("${parsed.rates.size} ngoại tệ, $changed thay đổi")
    }

    companion object {
        const val URL = "https://portal.vietcombank.com.vn/Usercontrols/TVPortal.TyGia/pXML.aspx"
    }
}
