package com.mercadomania.game.ui.viewmodel

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.data.GameRepository
import com.mercadomania.game.ui.Assets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Immutable state for the loading screen. */
data class LoadingUiState(
    val progress: Float = 0f,
    val ready: Boolean = false
) {
    val percent: Int get() = (progress.coerceIn(0f, 1f) * 100f).toInt()
}

/**
 * Drives the startup progress bar off real work, not a timer.
 *
 * Three things genuinely take time on a cold start, and all three are done
 * here so the first frame of the main screen does not have to do them:
 *
 *  1. the first read of [GameRepository.data] - DataStore hitting the disk;
 *  2. SoundPool decoding the six SFX, which is asynchronous and can take a
 *     few hundred milliseconds;
 *  3. decoding the large drawables the opening screens draw.
 *
 * The bar is the share of those units that are finished. Two guards keep it
 * honest in both directions: [MIN_VISIBLE_MILLIS] stops it flashing past on a
 * warm start, and [MAX_WAIT_MILLIS] guarantees the game opens even if a step
 * never reports done - the player is never stuck staring at a stalled bar.
 */
class LoadingViewModel(
    context: Context,
    private val repository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(LoadingUiState())
    val state: StateFlow<LoadingUiState> = _state.asStateFlow()

    private val drawables = Assets.preloadOnStartup
    private val soundUnits = soundManager.expectedSamples.coerceAtLeast(0)
    private val totalUnits = (1 + drawables.size + soundUnits).coerceAtLeast(1)

    private var doneUnits = 0

    init {
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()

            withTimeoutOrNull(MAX_WAIT_MILLIS) {
                // 1. Stored progress.
                repository.data.first()
                completeUnits(1)

                // 2. Artwork. Going through the resource loader here warms
                //    both the file cache and the framework drawable cache.
                withContext(Dispatchers.IO) {
                    drawables.forEach { id ->
                        runCatching { ContextCompat.getDrawable(appContext, id) }
                        completeUnits(1)
                    }
                }

                // 3. SFX, reported by SoundPool as each sample finishes.
                if (soundUnits > 0) {
                    var seen = 0
                    soundManager.loadedSamples.first { loaded ->
                        val fresh = loaded.coerceAtMost(soundUnits) - seen
                        if (fresh > 0) {
                            seen += fresh
                            completeUnits(fresh)
                        }
                        seen >= soundUnits
                    }
                }
            }

            // Whatever happened above, the bar finishes full.
            doneUnits = totalUnits
            publish()

            val elapsed = System.currentTimeMillis() - startedAt
            if (elapsed < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - elapsed)

            _state.value = _state.value.copy(ready = true)
        }
    }

    private fun completeUnits(count: Int) {
        doneUnits = (doneUnits + count).coerceAtMost(totalUnits)
        publish()
    }

    private fun publish() {
        _state.value = _state.value.copy(
            progress = doneUnits.toFloat() / totalUnits.toFloat()
        )
    }

    private companion object {
        /** Below this the bar would just flash, which reads as a glitch. */
        const val MIN_VISIBLE_MILLIS = 650L

        /** Hard ceiling so a stuck step can never trap the player here. */
        const val MAX_WAIT_MILLIS = 6_000L
    }
}

class LoadingViewModelFactory(
    private val context: Context,
    private val repository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(LoadingViewModel::class.java)) {
            "Unexpected ViewModel class ${modelClass.name}"
        }
        return LoadingViewModel(context, repository, soundManager) as T
    }
}
