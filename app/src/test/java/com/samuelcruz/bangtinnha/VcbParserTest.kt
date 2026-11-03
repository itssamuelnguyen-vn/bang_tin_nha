package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.fx.FxRate
import com.samuelcruz.bangtinnha.fx.ParsedRate
import com.samuelcruz.bangtinnha.fx.VcbParser
import com.samuelcruz.bangtinnha.fx.differsFrom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VcbParserTest {
    @Test
    fun parsesRatesInHundredthsOfDong() {
        val parsed = VcbParser.parse(fixture("vcb_exrates.xml"))
        assertNotNull(parsed.time)
        val aud = parsed.rates.first { it.code == "AUD" }
        assertEquals("AUSTRALIAN DOLLAR", aud.name)
        assertEquals(1753081L, aud.buyCash)
        assertEquals(1770789L, aud.buyTransfer)
        assertEquals(1827558L, aud.sell)
        // No cash buying for DKK: "-" becomes null.
        assertNull(parsed.rates.first { it.code == "DKK" }.buyCash)
        assertTrue(parsed.rates.any { it.code == "USD" })
    }

    @Test
    fun hundredths() {
        assertEquals(2634550L, VcbParser.hundredths("26,345.50"))
        assertEquals(17L, VcbParser.hundredths("0.17"))
        assertNull(VcbParser.hundredths("-"))
        assertNull(VcbParser.hundredths(""))
    }

    @Test
    fun onlyChangedRatesAreNew() {
        val stored = FxRate(1, 1, 0, null, 100, 200, 300)
        assertFalse(stored.differsFrom(ParsedRate("USD", "US DOLLAR", 100, 200, 300)))
        assertTrue(stored.differsFrom(ParsedRate("USD", "US DOLLAR", 100, 200, 301)))
        assertTrue((null as FxRate?).differsFrom(ParsedRate("USD", "US DOLLAR", 100, 200, 300)))
    }
}
