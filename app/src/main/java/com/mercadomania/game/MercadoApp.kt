package com.mercadomania.game

import android.app.Application
import android.content.Context
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.data.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Owns the two process-wide singletons: the repository and the sound manager.
 *
 * There is no DI framework here on purpose - the object graph is two nodes
 * deep, and ViewModels receive what they need through small explicit
 * [androidx.lifecycle.ViewModelProvider.Factory] implementations.
 */
// `open` so the White/Black build's Application
// (com.mercadomania.game.integration.MercadoManiaIntegrationApp) can extend it and
// bootstrap the integration layer after the game's own setup has run.
open class MercadoApp : Application() {

    lateinit var repository: GameRepository
        private set

    lateinit var soundManager: SoundManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        repository = GameRepository(this)
        soundManager = SoundManager(this)

        // Sync the persisted sound preference to the audio layer at startup and
        // on every later change, so Settings takes effect immediately.
        appScope.launch {
            repository.data
                .map { it.settings.soundEnabled }
                .distinctUntilChanged()
                .collect { enabled -> soundManager.enabled = enabled }
        }
    }

    override fun onTerminate() {
        // Only called on emulators, but releasing here costs nothing.
        soundManager.release()
        super.onTerminate()
    }
}

/**
 * Resolves the [MercadoApp] from any [Context].
 *
 * Under Compose previews, or if the app class were ever swapped out, this
 * returns `null` instead of throwing so screens can render a safe fallback.
 */
fun Context.mercadoAppOrNull(): MercadoApp? = applicationContext as? MercadoApp
