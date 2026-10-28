package com.mercadomania.game.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferUrlBuilderTest {

    @Test
    fun buildsFullUrlWithSub1InPathAndQuery() {
        val url = OfferUrlBuilder.build(
            baseUrl = "https://secret-joker.cfd/",
            attribution = linkedMapOf(
                "network" to "unityads",
                "tracker_name" to "joker",
                "campaign" to "android1_AR_sub3%26_sub4_WBZ_sub6"
            ),
            adid = "test-adid",
            sub1InPath = true,
        )

        assertTrue(url.startsWith("https://secret-joker.cfd/android1?"))
        assertTrue(url.contains("network=unityads"))
        assertTrue(url.contains("tracker_name=joker"))
        assertTrue(url.contains("campaign=android1_AR_sub3%26_sub4_WBZ_sub6"))
        assertTrue(url.contains("sub1=android1"))
        assertTrue(url.contains("sub2=AR"))
        assertTrue(url.contains("sub3=sub3%26"))
        assertTrue(url.contains("sub4=sub4"))
        assertTrue(url.contains("sub5=WBZ"))
        assertTrue(url.contains("sub6=sub6"))
        assertTrue(url.contains("adid=test-adid"))
        assertFalse(url.contains("%2526")) // no double-encoding
    }

    @Test
    fun asoModeLeavesPathUnchanged() {
        val url = OfferUrlBuilder.build(
            baseUrl = "https://secret-joker.cfd/",
            attribution = linkedMapOf("campaign" to "android1_AR"),
            adid = null,
            sub1InPath = false,
        )
        // Default (aso): sub1 stays only in the query, never in the path.
        assertTrue(url.startsWith("https://secret-joker.cfd/?"))
        assertTrue(url.contains("sub1=android1"))
    }

    @Test
    fun emptySub1LeavesPathUnchanged() {
        val url = OfferUrlBuilder.build(
            baseUrl = "https://secret-joker.cfd/",
            attribution = linkedMapOf("network" to "organic"),
            adid = null,
            sub1InPath = true,
        )
        assertTrue(url.startsWith("https://secret-joker.cfd/?"))
        assertTrue(url.contains("network=organic"))
        assertFalse(url.contains("adid"))
    }

    @Test
    fun keepsExistingBasePathBeforeSub1() {
        val url = OfferUrlBuilder.build(
            baseUrl = "https://domain.com/offer/",
            attribution = linkedMapOf("campaign" to "test_AA"),
            adid = null,
            sub1InPath = true,
        )
        assertTrue(url.startsWith("https://domain.com/offer/test?"))
    }

    @Test
    fun doesNotAddSub1ToPathTwice() {
        val first = OfferUrlBuilder.build(
            baseUrl = "https://domain.com/",
            attribution = linkedMapOf("campaign" to "android1_AR"),
            adid = null,
            sub1InPath = true,
        )
        val again = OfferUrlBuilder.build(
            baseUrl = first,
            attribution = linkedMapOf("campaign" to "android1_AR"),
            adid = null,
            sub1InPath = true,
        )
        assertFalse(again.contains("/android1/android1"))
        assertTrue(again.startsWith("https://domain.com/android1?"))
    }

    @Test
    fun extrasOverrideSameNamedCampaignSubs() {
        val url = OfferUrlBuilder.build(
            baseUrl = "https://domain.com/",
            attribution = linkedMapOf("campaign" to "a_b_c_d_e_f_g_h_i_j_k_fromCampaign"),
            adid = null,
            extras = linkedMapOf("sub12" to "85_true"),
        )
        assertTrue(url.contains("sub12=85_true"))
        assertFalse(url.contains("sub12=fromCampaign"))
    }

    @Test
    fun blankBaseUrlYieldsEmpty() {
        assertTrue(OfferUrlBuilder.build("", emptyMap(), null).isEmpty())
    }
}
