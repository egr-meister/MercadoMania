package com.mercadomania.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingRulesTest {

    @Test
    fun `404 routes to White`() {
        val decision = RoutingRules.decide(ProbeResult(404, REQUESTED), REQUESTED)
        assertEquals(RouteDecision.White, decision)
    }

    @Test
    fun `200 routes to Black with the final url`() {
        val decision = RoutingRules.decide(
            ProbeResult(200, "https://domain.com/landing"),
            REQUESTED
        )
        assertTrue(decision is RouteDecision.Black)
        assertEquals("https://domain.com/landing", (decision as RouteDecision.Black).url)
    }

    @Test
    fun `other codes route to Black`() {
        for (code in intArrayOf(200, 201, 301, 302, 403, 500, 502)) {
            val decision = RoutingRules.decide(ProbeResult(code, REQUESTED), REQUESTED)
            assertTrue("code $code should be Black", decision is RouteDecision.Black)
        }
    }

    @Test
    fun `falls back to requested url when final url is null or blank`() {
        val nullFinal = RoutingRules.decide(ProbeResult(200, null), REQUESTED)
        assertEquals(REQUESTED, (nullFinal as RouteDecision.Black).url)

        val blankFinal = RoutingRules.decide(ProbeResult(200, "  "), REQUESTED)
        assertEquals(REQUESTED, (blankFinal as RouteDecision.Black).url)
    }

    private companion object {
        const val REQUESTED = "https://domain.com/x?campaign=y"
    }
}
