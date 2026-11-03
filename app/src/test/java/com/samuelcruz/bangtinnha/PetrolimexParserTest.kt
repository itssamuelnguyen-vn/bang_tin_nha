package com.samuelcruz.bangtinnha

import com.samuelcruz.bangtinnha.fuel.PetrolimexParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Base64

class PetrolimexParserTest {
    @Test
    fun parsesProductsFromTheSource() {
        val products = PetrolimexParser.parse(fixture("petrolimex_prices.json"))
        assertEquals(6, products.size)
        val ron95 = products.first { it.name == "Xăng E10 RON 95-V" }
        assertEquals(24950L, ron95.zone1Price)
        assertEquals("b85a9189d7174ee0a4be489f34962ab7", ron95.key)
        assertEquals(Instant.parse("2026-10-31T16:53:44.611Z").toEpochMilli(), ron95.effectiveAt)
        // Sorted by Petrolimex's display order.
        assertTrue(products.zipWithNext().all { (a, b) -> a.order <= b.order })
    }

    @Test
    fun requestFilterIsTheWebsitesOne() {
        val encoded = PetrolimexParser.url().substringAfter("x-request=")
        val filter = String(Base64.getUrlDecoder().decode(encoded))
        assertTrue(filter.contains("3801378fe1e045b1afa10de7c5776124"))
        assertTrue(filter.contains("\"Status\":{\"Equals\":\"Published\"}"))
    }
}
