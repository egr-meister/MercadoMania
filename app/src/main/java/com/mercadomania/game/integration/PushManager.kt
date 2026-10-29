package com.mercadomania.game.integration

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The push token must ride the single install click.
 *
 * The push service knows a device by the `push_token` that arrived in the tracker WITH the
 * click. An install has exactly one click (see adjust-integration, section 7), and a token
 * that misses it is never seen by the service — the device stays unreachable until it is
 * reinstalled.
 *
 * So the token is requested in Application.onCreate, awaited (briefly) while the URL is
 * built, and persisted. Later launches wait for nothing.
 */
object PushManager {

    private const val TAG = "PushManager"
    private const val AWAIT_TIMEOUT_SECONDS = 3L

    private val ready = CountDownLatch(1)

    fun init(app: Application) {
        // An already-stored token answers at once: later launches wait for nothing.
        if (!IntegrationStorage.pushToken.isNullOrBlank()) {
            ready.countDown()
            return
        }
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    IntegrationStorage.pushToken = task.result
                } else {
                    Log.w(TAG, "FCM token request failed", task.exception)
                }
                // Open the latch on failure too: that is the answer "no token right now",
                // and waiting for it any longer is pointless.
                ready.countDown()
            }
        }.onFailure {
            // Missing google-services.json / Firebase not initialised: do not block startup.
            Log.w(TAG, "Could not request the FCM token", it)
            ready.countDown()
        }
    }

    /** Background threads only: blocks up to a few seconds for the first token. */
    fun awaitToken(): String {
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure {
                Thread.currentThread().interrupt()
                Log.w(TAG, "Interrupted while waiting for the FCM token", it)
            }
        return IntegrationStorage.pushToken.orEmpty()
    }
}
