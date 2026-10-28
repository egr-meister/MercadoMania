package com.mercadomania.game

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercadomania.game.data.GameRepository
import com.mercadomania.game.integration.DeepLinkRouter
import com.mercadomania.game.integration.PushPayload
import com.mercadomania.game.ui.MercadoManiaRoot
import com.mercadomania.game.ui.theme.MercadoManiaTheme
import com.mercadomania.game.ui.theme.ThemeMode
import kotlinx.coroutines.flow.map

/**
 * The single activity.
 *
 * AppCompatActivity (not ComponentActivity) is required for
 * AppCompatDelegate.setApplicationLocales to survive a restart below API 33 - that is the
 * one reason appcompat is a dependency.
 *
 * Portrait is locked in the manifest, the window is drawn edge to edge and every screen
 * applies `safeDrawingPadding()` itself, and the launch theme is swapped for the real theme
 * before the first frame so the splash does not linger behind the UI.
 *
 * `launchMode="singleTop"`: a push tapped while the app is already running lands in
 * [onNewIntent], not a second instance, and its deep link is forwarded to [DeepLinkRouter].
 */
class MainActivity : AppCompatActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Leave the splash theme behind before the content view exists.
        setTheme(R.style.Theme_MercadoMania)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // A deep link can arrive on a cold start by tapping a notification.
        consumeIntent(intent)
        requestNotificationsPermission()

        val repository: GameRepository =
            (application as? MercadoApp)?.repository ?: GameRepository(applicationContext)
        val themeModeFlow = repository.data.map { ThemeMode.fromStorage(it.settings.themeMode) }

        setContent {
            val themeMode by themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            MercadoManiaTheme(themeMode = themeMode) {
                // The White/Black orchestrator. It renders the game (White) or the offer
                // (Black); the theme, edge-to-edge and splash hand-off are unchanged.
                MercadoManiaRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop: a push into an already-running app lands here, not in a new instance.
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        // Marked on the intent itself, not a field, so one push is not opened twice when the
        // Activity is recreated for a reason other than rotation.
        if (intent.getBooleanExtra(EXTRA_CONSUMED, false)) return
        // A plain launcher intent is not a push: only act on our own deep-link extra.
        val url = intent.getStringExtra(PushPayload.KEY_URL)
        if (url.isNullOrBlank()) return
        intent.putExtra(EXTRA_CONSUMED, true)
        DeepLinkRouter.handle(url)
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        // Fire-and-forget: the token arrives without the permission, so routing and
        // push_token in the click do not depend on it. A refusal must break nothing.
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private companion object {
        const val EXTRA_CONSUMED = "mercado_push_consumed"
    }
}
