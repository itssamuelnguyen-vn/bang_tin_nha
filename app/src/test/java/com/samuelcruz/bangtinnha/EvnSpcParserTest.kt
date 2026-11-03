package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.core.text.matchesAnyKeyword
import com.samuelcruz.bangtinnha.core.text.normalizeVietnamese
import com.samuelcruz.bangtinnha.power.EvnSpcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class EvnSpcParserTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun parsesOutages() {
        val parsed = EvnSpcParser.parseOutages(fixture("evnspc_outages.html"))
        assertEquals("Điện lực Tân Ninh", parsed.unitName)
        assertEquals(9, parsed.outages.size)
        val first = parsed.outages.first()
        assertEquals("Khu phố Ninh Thành phường Bình Minh", first.area)
        assertEquals(ZonedDateTime.of(2026, 11, 3, 8, 0, 0, 0, zone).toInstant().toEpochMilli(), first.startAt)
        assertEquals(ZonedDateTime.of(2026, 11, 3, 17, 0, 0, 0, zone).toInstant().toEpochMilli(), first.endAt)
        assertEquals("Bảo trì, sửa chữa lưới điện", first.reason)
        // Several khu phố stay on separate lines.
        assertTrue(parsed.outages[1].area.lines().size > 1)
        assertTrue(parsed.outages.all { it.endAt > it.startAt })
    }

    @Test
    fun noScheduleIsAnEmptyListNotAnError() {
        assertTrue(EvnSpcParser.parseOutages(fixture("evnspc_outages_empty.html")).outages.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun unknownPageFailsLoudly() {
        EvnSpcParser.parseOutages("<html><body>Trang bảo trì</body></html>")
    }

    @Test
    fun parsesUnits() {
        val units = EvnSpcParser.parseUnits(fixture("evnspc_units.html"))
        assertEquals("PB0501", units.first().code)
        assertEquals("Điện lực Tân Ninh", units.first().name)
        assertTrue(units.any { it.code == "PB0614" }) // Long An cũ, now part of Tây Ninh
    }

    @Test
    fun keywordsIgnoreCaseAndAccents() {
        assertEquals("phuong binh minh", normalizeVietnamese("  Phường  Bình Minh "))
        assertEquals("duong 30/4", normalizeVietnamese("Đường 30/4"))
        assertTrue(matchesAnyKeyword("Khu phố Ninh Thành phường Bình Minh", listOf("binh minh")))
        assertTrue(matchesAnyKeyword("Khu phố Ninh Thành phường Bình Minh", listOf("xyz", "NINH THANH")))
        assertFalse(matchesAnyKeyword("Khu phố Ninh Thành phường Bình Minh", listOf("Hòa Thành")))
        assertTrue(matchesAnyKeyword("bất kỳ", emptyList()))
        assertTrue(matchesAnyKeyword("bất kỳ", listOf("  ", "")))
    }
}
