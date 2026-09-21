package com.mercadomania.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferUrlBuilderTest {

    @Test
    fun `builds the full offer url with sub1 in path and every param in query`() {
        val params = linkedMapOf(
            "af_status" to "Non-organic",
            "media_source" to "facebook",
            "campaign" to "android1_AR_sub3%26_sub4_WBZ_sub6"
        )

        val url = OfferUrlBuilder.build(
            baseUrl = "https://secret-joker.cfd/",
            appsFlyerParams = params,
            appsFlyerId = "test-af-id"
        )

        // sub1 in the path, right after the domain.
        assertTrue(url, url.startsWith("https://secret-joker.cfd/android1?"))

        // every AppsFlyer param survives.
        assertTrue(url, url.contains("af_status=Non-organic"))
        assertTrue(url, url.contains("media_source=facebook"))
        // the raw campaign is kept even after being exploded.
        assertTrue(url, url.contains("campaign=android1_AR_sub3%26_sub4_WBZ_sub6"))

        // the exploded subs are all in the query, sub1 included.
        assertTrue(url, url.contains("sub1=android1"))
        assertTrue(url, url.contains("sub2=AR"))
        assertTrue(url, url.contains("sub3=sub3%26"))
        assertTrue(url, url.contains("sub4=sub4"))
        assertTrue(url, url.contains("sub5=WBZ"))
        assertTrue(url, url.contains("sub6=sub6"))

        // appsflyer_id is appended.
        assertTrue(url, url.contains("appsflyer_id=test-af-id"))

        // no double-encoding.
        assertFalse(url, url.contains("%2526"))
    }

    @Test
    fun `produces the exact expected url for the worked example`() {
        val params = linkedMapOf(
            "af_status" to "Non-organic",
            "media_source" to "facebook",
            "campaign" to "android1_AR_sub3%26_sub4_WBZ_sub6"
        )
        val url = OfferUrlBuilder.build("https://secret-joker.cfd/", params, "abc")
        assertEquals(
            "https://secret-joker.cfd/android1?af_status=Non-organic&media_source=facebook" +
                "&campaign=android1_AR_sub3%26_sub4_WBZ_sub6" +
                "&sub1=android1&sub2=AR&sub3=sub3%26&sub4=sub4&sub5=WBZ&sub6=sub6" +
                "&appsflyer_id=abc",
            url
        )
    }

    @Test
    fun `no campaign means no path segment but params are kept`() {
        val params = linkedMapOf("af_status" to "Organic", "media_source" to "organic")
        val url = OfferUrlBuilder.build("https://domain.com/", params, null)

        assertTrue(url, url.startsWith("https://domain.com/?"))
        assertFalse(url, url.startsWith("https://domain.com/android"))
        assertTrue(url, url.contains("af_status=Organic"))
        assertTrue(url, url.contains("media_source=organic"))
    }

    @Test
    fun `empty sub1 does not add a path segment`() {
        // campaign present but its first segment is empty.
        val params = linkedMapOf("campaign" to "_AR_x")
        val url = OfferUrlBuilder.build("https://domain.com/", params, null)
        assertTrue(url, url.startsWith("https://domain.com/?"))
        // it still appears in the query, empty.
        assertTrue(url, url.contains("sub1=&"))
        assertTrue(url, url.contains("sub2=AR"))
    }

    @Test
    fun `sub1 is appended after an existing base path`() {
        val params = linkedMapOf("campaign" to "test_AR")
        val url = OfferUrlBuilder.build("https://domain.com/offer/", params, null)
        assertTrue(url, url.startsWith("https://domain.com/offer/test?"))
    }

    @Test
    fun `adds a slash when the base path has none`() {
        val params = linkedMapOf("campaign" to "test")
        val url = OfferUrlBuilder.build("https://domain.com", params, null)
        assertTrue(url, url.startsWith("https://domain.com/test?"))
    }

    @Test
    fun `build is idempotent on its own output`() {
        val params = linkedMapOf("campaign" to "android1_AR")
        val once = OfferUrlBuilder.build("https://domain.com/", params, "id")
        // Feeding the built URL back in must not add android1 to the path twice.
        val twice = OfferUrlBuilder.build(once, params, "id")
        assertFalse(twice, twice.contains("/android1/android1"))
        assertTrue(twice, twice.startsWith("https://domain.com/android1?"))
    }

    @Test
    fun `preserves fragment`() {
        val params = linkedMapOf("campaign" to "x")
        val url = OfferUrlBuilder.build("https://domain.com/#frag", params, null)
        assertTrue(url, url.endsWith("#frag"))
        assertTrue(url, url.startsWith("https://domain.com/x?"))
    }

    @Test
    fun `blank base url yields empty string`() {
        assertEquals("", OfferUrlBuilder.build("   ", linkedMapOf("campaign" to "x"), "id"))
    }
}
