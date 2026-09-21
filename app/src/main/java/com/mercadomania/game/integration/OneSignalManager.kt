package com.mercadomania.game.integration

import android.app.Application
import android.util.Log
import com.mercadomania.game.BuildConfig
import com.onesignal.OneSignal
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * OneSignal push, initialised strictly after AppsFlyer so the AppsFlyer UID is available
 * to use as the OneSignal external id. A notification carrying a `url` in its additional
 * data is forwarded to [DeepLinkRouter], which the root composable turns into an offer
 * WebView.
 */
object OneSignalManager {

    private const val TAG = "OneSignalManager"

    @Volatile
    private var initialized = false

    fun init(app: Application) {
        val appId = BuildConfig.ONESIGNAL_APP_ID
        if (appId.isBlank()) {
            // A build without an app id simply runs without push; nothing else depends on it.
            Log.w(TAG, "ONESIGNAL_APP_ID is empty - OneSignal disabled for this build")
            return
        }

        OneSignal.initWithContext(app, appId)
        initialized = true

        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                // A pushed `url` becomes a deep link into the offer WebView. Empty means
                // "no deep link", and the tap just opens the app normally.
                val url = event.notification.additionalData?.optString("url").orEmpty()
                if (url.isNotBlank()) {
                    DeepLinkRouter.handle(url)
                }
            }
        })

        // requestPermission is a suspend function in OneSignal 5.x, so it must run in a
        // coroutine. Failure here is non-fatal - the user simply is not prompted.
        CoroutineScope(Dispatchers.Main).launch {
            runCatching { OneSignal.Notifications.requestPermission(true) }
                .onFailure { Log.w(TAG, "requestPermission failed", it) }
        }

        // The UID may already have been captured by an earlier AppsFlyer callback, before
        // OneSignal was up. Link it now if so.
        syncExternalId()
    }

    /**
     * Sets OneSignal's external id to the AppsFlyer UID, so a push audience can be
     * addressed by the same identity the offer URL carries. Safe to call repeatedly and
     * before init - it no-ops until both the SDK and a non-empty UID are available.
     */
    fun syncExternalId() {
        if (!initialized) return
        val id = IntegrationStorage.appsFlyerId
        if (id.isNullOrBlank()) return
        runCatching { OneSignal.login(id) }
            .onFailure { Log.w(TAG, "OneSignal.login failed", it) }
    }
}
