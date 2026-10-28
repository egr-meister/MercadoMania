package com.mercadomania.game.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingRulesTest {

    @Test
    fun notFoundIsWhite() {
        assertTrue(RoutingRules.isWhite(404))
    }

    @Test
    fun everythingElseIsNotWhite() {
        // 403 (WAF), 5xx (server hiccup) and redirects are NOT the tracker's White answer.
        assertFalse(RoutingRules.isWhite(200))
        assertFalse(RoutingRules.isWhite(302))
        assertFalse(RoutingRules.isWhite(403))
        assertFalse(RoutingRules.isWhite(410))
        assertFalse(RoutingRules.isWhite(500))
    }
}
