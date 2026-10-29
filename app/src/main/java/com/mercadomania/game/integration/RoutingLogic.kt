package com.mercadomania.game.integration

import java.util.concurrent.TimeUnit

/**
 * The branch and the offer address are two different things with opposite lifetimes:
 * the White verdict is permanent, the offer address lives 24h and resets itself. They are
 * never merged into one "route decision" with a shared TTL.
 */
sealed interface RouteDecision {
    data object White : RouteDecision

    /**
     * @param fromCache true when [url] came from the fresh cache, false when it was just
     * built from the campaign link. The WebView screen is keyed on this: a rebuild after a
     * dead cache is a different load with a different meaning for its errors, so it needs
     * its own WebView and client even if the bytes of the URL happen to match.
     */
    data class Black(val url: String, val fromCache: Boolean) : RouteDecision
}

object RoutingRules {

    const val NOT_FOUND = 404

    /**
     * 404 is the agreed "this install gets the app itself" answer. Everything else means
     * the offer exists: 200, a redirect chain that ended somewhere, even a 500 — the server
     * is simply having a moment and the WebView can retry.
     *
     * 403 is deliberately NOT White: that is Cloudflare or a WAF, not the tracker's
     * decision. Reading it as White would write off an install over someone else's error,
     * and write it off forever — the White verdict is irreversible.
     */
    fun isWhite(mainFrameStatus: Int): Boolean = mainFrameStatus == NOT_FOUND
}

object OfferCache {

    val OFFER_TTL_MS: Long = TimeUnit.HOURS.toMillis(24)

    fun isFresh(savedAt: Long, now: Long, ttlMs: Long): Boolean {
        // 0 is also a record written before the timestamp existed: treat it as expired, so
        // an app update heals installs that got stuck on a timeless cache.
        if (savedAt <= 0L) return false
        val age = now - savedAt
        // Negative age — the device clock moved backwards. Rebuilding the link is cheaper
        // than leaving the install on an eternal cache.
        if (age < 0L) return false
        return age < ttlMs
    }
}
