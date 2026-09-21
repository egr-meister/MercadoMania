package com.mercadomania.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignParserTest {

    @Test
    fun `parses positional subs and keeps encoded sequences intact`() {
        val result = CampaignParser.parse("android1_AR_sub3%26_sub4_WBZ_sub6")

        assertEquals("android1", result["sub1"])
        assertEquals("AR", result["sub2"])
        // %26 must survive verbatim - decoding it would inject a raw & into the query.
        assertEquals("sub3%26", result["sub3"])
        assertEquals("sub4", result["sub4"])
        assertEquals("WBZ", result["sub5"])
        assertEquals("sub6", result["sub6"])
        assertEquals(6, result.size)
    }

    @Test
    fun `preserves order`() {
        val result = CampaignParser.parse("a_b_c_d")
        assertEquals(listOf("sub1", "sub2", "sub3", "sub4"), result.keys.toList())
    }

    @Test
    fun `keeps empty segments as positional placeholders`() {
        val result = CampaignParser.parse("a__c")
        assertEquals("a", result["sub1"])
        assertEquals("", result["sub2"])
        assertEquals("c", result["sub3"])
        assertEquals(3, result.size)
    }

    @Test
    fun `empty campaign yields no subs`() {
        assertTrue(CampaignParser.parse("").isEmpty())
    }

    @Test
    fun `toQuery does not double-encode already-encoded values`() {
        val query = CampaignParser.toQuery(linkedMapOf("sub3" to "sub3%26"))
        assertEquals("sub3=sub3%26", query)
        assertFalse(query.contains("%2526"))
    }
}
