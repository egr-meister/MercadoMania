package com.mercadomania.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.audio.Sfx
import com.mercadomania.game.data.GameData
import com.mercadomania.game.data.GameRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Immutable state for every screen that only reads stored progress. */
data class GameDataUiState(
    val loaded: Boolean = false,
    val data: GameData = GameData()
)

/**
 * Shared read/write ViewModel for the menu, levels, results and settings
 * screens. Holds no game session of its own.
 */
class GameDataViewModel(
    private val repository: GameRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    val state: StateFlow<GameDataUiState> = repository.data
        .map { GameDataUiState(loaded = true, data = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GameDataUiState()
        )

    /** Button/back/pause feedback. */
    fun click() = soundManager.play(Sfx.CLICK)

    fun setSoundEnabled(enabled: Boolean) {
        // Apply immediately so the confirmation click is already correct,
        // then persist.
        soundManager.enabled = enabled
        if (enabled) soundManager.play(Sfx.CLICK)
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun resetProgress() {
        soundManager.play(Sfx.CLICK)
        viewModelScope.launch { repository.resetAll() }
    }
}

/** Plain factory - no DI framework anywhere in this project. */
class GameDataViewModelFactory(
    private val repository: GameRepository,
    private val soundManager: SoundManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameDataViewModel::class.java)) {
            "Unexpected ViewModel class ${modelClass.name}"
        }
        return GameDataViewModel(repository, soundManager) as T
    }
}
