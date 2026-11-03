package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.gold.PnjParser
import com.samuelcruz.bangtinnha.gold.millions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldParserTest {
    @Test
    fun parsesPnjBoardInDongPerChi() {
        val table = PnjParser.parse(fixture("pnj_gold.json"))
        assertNotNull(table.time)
        // 20 rows, 2 of them buy-only raw gold ("giaban": "") which are skipped.
        assertEquals(18, table.items.size)
        assertTrue(table.items.none { it.code.startsWith("RAW_") })
        val sjc = table.items.first()
        assertEquals("SJC", sjc.code)
        assertEquals("Vàng miếng SJC 999.9", sjc.name)
        assertEquals(14_060_000L, sjc.sell)
        assertEquals(13_760_000L, sjc.buy)
        assertEquals("N24K", table.items[1].code)
    }

    @Test
    fun millionsLabel() {
        assertEquals("14,06 tr", millions(14_060_000))
    }
}
