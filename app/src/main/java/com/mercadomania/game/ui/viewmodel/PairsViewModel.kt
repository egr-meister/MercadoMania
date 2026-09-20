package com.mercadomania.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercadomania.game.audio.Sfx
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.data.GameRepository
import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.game.LevelConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** One card on the board. Matched cards stay face up. */
data class PairCard(
    val id: Int,
    val iconIndex: Int,
    val faceUp: Boolean = false,
    val matched: Boolean = false
)

enum class PairsOutcome { PLAYING, WIN, LOSE }

/** Immutable UI state for one Matching Pairs round. */
data class PairsUiState(
    val level: Int = 1,
    val pairs: Int = 0,
    val columns: Int = 2,
    val totalSeconds: Int = 0,
    val secondsLeft: Int = 0,
    val cards: List<PairCard> = emptyList(),
    val matchedPairs: Int = 0,
    val bestPairs: Int = 0,
    val paused: Boolean = false,
    val inputLocked: Boolean = false,
    val outcome: PairsOutcome = PairsOutcome.PLAYING,
    val valid: Boolean = true
) {
    val finished: Boolean get() = outcome != PairsOutcome.PLAYING
    val timeProgress: Float
        get() = if (totalSeconds <= 0) 0f else (secondsLeft.toFloat() / totalSeconds).coerceIn(0f, 1f)
}

class PairsViewModel(
    private val repository: GameRepository,
    private val soundManager: SoundManager,
    private val level: Int
) : ViewModel() {

    private val config: LevelConfig? = GameLogic.levelConfig(level)

    private val _state = MutableStateFlow(initialState(round = 0))
    val state: StateFlow<PairsUiState> = _state.asStateFlow()

    private var round = 0
    private var timerJob: Job? = null
    private var flipBackJob: Job? = null

    init {
        viewModelScope.launch {
            repository.data
                .map { it.pairsBestFor(level) }
                .distinctUntilChanged()
                .collect { best -> _state.update { it.copy(bestPairs = best) } }
        }
        startTimer()
    }

    private fun initialState(round: Int): PairsUiState {
        val cfg = config ?: return PairsUiState(level = level, valid = false)
        val deck = GameLogic.buildDeck(
            pairs = cfg.pairs,
            iconCount = GameLogic.ICON_COUNT,
            random = Random(GameLogic.seedFor(cfg.level, round))
        )
        return PairsUiState(
            level = cfg.level,
            pairs = cfg.pairs,
            columns = cfg.columns,
            totalSeconds = cfg.seconds,
            secondsLeft = cfg.seconds,
            cards = deck.mapIndexed { index, icon -> PairCard(id = index, iconIndex = icon) },
            valid = true
        )
    }

    private fun startTimer() {
        if (config == null) return
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MILLIS)
                val current = _state.value
                if (current.paused || current.finished) continue
                val next = current.secondsLeft - 1
                if (next <= 0) {
                    _state.update { it.copy(secondsLeft = 0) }
                    loseByTimeout()
                    return@launch
                }
                _state.update { it.copy(secondsLeft = next) }
            }
        }
    }

    /** Tap on card [cardId]. */
    fun flip(cardId: Int) {
        val current = _state.value
        if (!current.valid || current.paused || current.finished || current.inputLocked) return
        val card = current.cards.firstOrNull { it.id == cardId } ?: return
        if (card.faceUp || card.matched) return

        val alreadyUp = current.cards.filter { it.faceUp && !it.matched }
        if (alreadyUp.size >= 2) return

        soundManager.play(Sfx.CLICK)
        val flipped = current.cards.map { if (it.id == cardId) it.copy(faceUp = true) else it }
        val nowUp = flipped.filter { it.faceUp && !it.matched }

        if (nowUp.size < 2) {
            _state.update { it.copy(cards = flipped) }
            return
        }

        val first = nowUp[0]
        val second = nowUp[1]

        if (first.iconIndex == second.iconIndex) {
            val matchedCards = flipped.map {
                if (it.id == first.id || it.id == second.id) it.copy(matched = true) else it
            }
            val matchedCount = current.matchedPairs + 1
            val won = matchedCount >= current.pairs
            soundManager.play(Sfx.MATCH)
            _state.update {
                it.copy(
                    cards = matchedCards,
                    matchedPairs = matchedCount,
                    outcome = if (won) PairsOutcome.WIN else PairsOutcome.PLAYING
                )
            }
            if (won) winRound(matchedCount)
        } else {
            _state.update { it.copy(cards = flipped, inputLocked = true) }
            flipBackJob?.cancel()
            flipBackJob = viewModelScope.launch {
                delay(MISMATCH_MILLIS)
                while (_state.value.paused) delay(PAUSE_POLL_MILLIS)
                _state.update { s ->
                    s.copy(
                        cards = s.cards.map {
                            if ((it.id == first.id || it.id == second.id) && !it.matched) {
                                it.copy(faceUp = false)
                            } else {
                                it
                            }
                        },
                        inputLocked = false
                    )
                }
            }
        }
    }

    private fun winRound(matchedPairs: Int) {
        timerJob?.cancel()
        flipBackJob?.cancel()
        soundManager.play(Sfx.WIN)
        viewModelScope.launch { repository.savePairsBest(level, matchedPairs) }
    }

    private fun loseByTimeout() {
        flipBackJob?.cancel()
        val matched = _state.value.matchedPairs
        _state.update { it.copy(outcome = PairsOutcome.LOSE, inputLocked = false) }
        soundManager.play(Sfx.LOSE)
        viewModelScope.launch { repository.savePairsBest(level, matched) }
    }

    fun pause() {
        if (_state.value.finished) return
        soundManager.play(Sfx.CLICK)
        _state.update { it.copy(paused = true) }
    }

    fun resume() {
        soundManager.play(Sfx.CLICK)
        _state.update { it.copy(paused = false) }
    }

    /** Fresh board, fresh timer, same level. */
    fun restart() {
        soundManager.play(Sfx.CLICK)
        flipBackJob?.cancel()
        timerJob?.cancel()
        round += 1
        val best = _state.value.bestPairs
        _state.value = initialState(round).copy(bestPairs = best)
        startTimer()
    }

    override fun onCleared() {
        timerJob?.cancel()
        flipBackJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TICK_MILLIS: Long = 1_000L
        const val MISMATCH_MILLIS: Long = 700L
        const val PAUSE_POLL_MILLIS: Long = 80L
    }
}

class PairsViewModelFactory(
    private val repository: GameRepository,
    private val soundManager: SoundManager,
    private val level: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PairsViewModel::class.java)) {
            "Unexpected ViewModel class ${modelClass.name}"
        }
        return PairsViewModel(repository, soundManager, level) as T
    }
}
