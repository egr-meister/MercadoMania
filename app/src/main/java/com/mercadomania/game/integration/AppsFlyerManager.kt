package com.mercadomania.game.integration

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.mercadomania.game.BuildConfig // BuildConfig lives in the module's namespace

object AppsFlyerManager {

    private const val TAG = "AppsFlyerManager"

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isAttributionResolved: Boolean = false
        private set

    // Written from the main thread by the root composable, read from AppsFlyer's worker
    // thread in notifyResolved(). Without @Volatile that worker can observe null, skip the
    // callback, and leave every first install sitting out the full startup timeout.
    @Volatile
    private var onAttributionResolved: (() -> Unit)? = null

    private lateinit var appContext: Context

    fun init(app: Application) {
        appContext = app.applicationContext

        if (Holdback.isHoldback) {
            // This install is in the AppsFlyer hold-back bucket, so the SDK is deliberately
            // never initialised. Resolve attribution at once so routing does not sit out
            // the 8s wait for a callback that will never come; downstream the install is
            // told apart from a real AppsFlyer failure by sub_id_15=holdback on the URL.
            Log.i(TAG, "Hold-back install: AppsFlyer disabled")
            markResolved()
            return
        }

        if (IntegrationStorage.attributionSettled) {
            // A previous launch already got its answer; nothing to wait for.
            isAttributionResolved = true
        }

        val devKey = BuildConfig.APPSFLYER_DEV_KEY
        if (devKey.isBlank()) {
            Log.w(TAG, "APPSFLYER_DEV_KEY is empty - AppsFlyer disabled for this build")
            // Unblock the UI so a keyless debug build still runs, but deliberately do not
            // set attributionSettled: a build with a real key must wait for its own first
            // callback rather than inherit this shortcut.
            isAttributionResolved = true
            notifyResolved()
            return
        }

        AppsFlyerLib.getInstance().setDebugLog(BuildConfig.DEBUG)
        AppsFlyerLib.getInstance().init(devKey, conversionListener, app)
        AppsFlyerLib.getInstance().start(app, devKey, object : AppsFlyerRequestListener {
            override fun onSuccess() {
                captureAppsFlyerId(app)
            }

            override fun onError(code: Int, message: String) {
                Log.w(TAG, "AppsFlyer start failed: code=$code, message=$message")
            }
        })
    }

    /**
     * The UID is not available synchronously after start(); it materialises once the SDK
     * has talked to its backend. Called from several callbacks for that reason.
     *
     * On a successful, non-empty capture the id is also pushed to OneSignal as the
     * external id, so the two systems address the same install.
     */
    fun captureAppsFlyerId(context: Context) {
        val uid = runCatching { AppsFlyerLib.getInstance().getAppsFlyerUID(context) }
            .onFailure { Log.w(TAG, "getAppsFlyerUID failed", it) }
            .getOrNull()
        // A null or empty read means "not ready yet", never "there is no UID" - so it
        // must not clear what we already stored.
        if (!uid.isNullOrBlank()) {
            IntegrationStorage.appsFlyerId = uid
            OneSignalManager.syncExternalId()
        }
    }

    fun setOnAttributionResolved(callback: (() -> Unit)?) {
        onAttributionResolved = callback
        if (callback != null && isAttributionResolved) {
            // Already resolved before the screen subscribed - deliver immediately so the
            // caller does not wait out its whole timeout for an event that already fired.
            mainHandler.post { callback() }
        }
    }

    private val conversionListener = object : AppsFlyerConversionListener {

        override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
            // Store the response verbatim. Selecting "the keys we need" here is what
            // makes networks report missing attribution months later, when they start
            // sending a key this code was never taught about.
            //
            // Merge rather than replace: this callback also fires on re-attribution, and
            // those payloads are not guaranteed to be supersets of the install payload.
            data?.let { IntegrationStorage.mergeAttribution(it) }
            captureAppsFlyerId(appContext)
            markResolved()
        }

        override fun onConversionDataFail(error: String?) {
            Log.w(TAG, "Conversion data failed: $error")
            captureAppsFlyerId(appContext)
            // A failure is still an answer: it means no attribution is coming for this
            // install, and startup must stop waiting.
            markResolved()
        }

        override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
            // Merge, not replace: this event carries deep-link parameters only and would
            // otherwise wipe the install attribution.
            data?.let { IntegrationStorage.mergeAttribution(it) }
        }

        override fun onAttributionFailure(error: String?) {
            Log.w(TAG, "App open attribution failed: $error")
        }
    }

    private fun markResolved() {
        isAttributionResolved = true
        IntegrationStorage.attributionSettled = true
        notifyResolved()
    }

    private fun notifyResolved() {
        val callback = onAttributionResolved ?: return
        // AppsFlyer calls back on a background thread; the waiter is Compose state.
        mainHandler.post { callback() }
    }
}
