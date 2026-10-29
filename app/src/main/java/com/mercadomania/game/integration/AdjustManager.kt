package com.mercadomania.game.integration

import android.app.Application
import android.content.Context
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAttribution
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.mercadomania.game.BuildConfig

/**
 * Adjust SDK v5. Differences from v4 that are easy to trip on: initialisation is
 * `Adjust.initSdk(config)`, not `Adjust.onCreate`; Activity lifecycle callbacks are no
 * longer needed, sessions are counted on their own.
 */
object AdjustManager {

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        val token = BuildConfig.ADJUST_APP_TOKEN
        if (token.isBlank()) return // build without a tracker: let White work

        val environment =
            if (BuildConfig.DEBUG) AdjustConfig.ENVIRONMENT_SANDBOX
            else AdjustConfig.ENVIRONMENT_PRODUCTION

        val config = AdjustConfig(app, token, environment)
        config.setLogLevel(if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN)

        // Set BEFORE initSdk, otherwise the first attribution passes the listener by.
        config.setOnAttributionChangedListener { attribution ->
            attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
        }

        Adjust.initSdk(config)

        // adid does not appear at once: a callback delivers it.
        Adjust.getAdid { adid -> IntegrationStorage.adid = adid }
    }

    /**
     * Waits for attribution for a bounded time. Adjust provides this out of the box, so no
     * home-grown CountDownLatch is needed: getAttributionWithTimeout returns null itself if
     * the data did not arrive. [onDone] always fires — on data or on timeout.
     */
    fun awaitAttribution(timeoutMs: Long, onDone: () -> Unit) {
        if (!::appContext.isInitialized || BuildConfig.ADJUST_APP_TOKEN.isBlank()) {
            onDone()
            return
        }
        Adjust.getAttributionWithTimeout(appContext, timeoutMs) { attribution ->
            attribution?.let { IntegrationStorage.mergeAttribution(it.toMap()) }
            onDone()
        }
    }

    /**
     * AdjustAttribution is an object with fields, not a map. Lay it out by field and keep
     * everything that is non-null; fields that are absent arrive as null. A null is not
     * stored ("null" is a four-character string, not an absence), but an empty string is:
     * on the offer side "arrived empty" and "did not arrive" mean different things.
     */
    private fun AdjustAttribution.toMap(): Map<String, String?> = linkedMapOf(
        "tracker_token" to trackerToken,
        "tracker_name" to trackerName,
        "network" to network,
        "campaign" to campaign,
        "adgroup" to adgroup,
        "creative" to creative,
        "click_label" to clickLabel,
    )
}
