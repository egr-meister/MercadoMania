package com.mercadomania.game.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mercadomania.game.audio.Sfx
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.data.GameRepository
import com.mercadomania.game.game.QuizCategory
import com.mercadomania.game.game.QuizContent
import com.mercadomania.game.game.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Immutable UI state for one quiz run.
 *
 * [selectedIndex] is only meaningful while [revealed] is true - that is the
 * ~0.75s window where the correct answer is green and a wrong pick is red.
 */
data class QuizUiState(
    val categoryId: String = "",
    val categoryTitle: String = "",
    /** Index into `Assets.mascots` - this category's host. */
    val mascotIndex: Int = 0,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val question: QuizQuestion? = null,
    val selectedIndex: Int? = null,
    val revealed: Boolean = false,
    val score: Int = 0,
    val bestScore: Int = 0,
    val paused: Boolean = false,
    val finished: Boolean = false,
    val valid: Boolean = true
) {
    /** 0f..1f for the progress strip. */
    val progress: Float
        get() = if (totalQuestions <= 0) 0f
        else (questionIndex.toFloat() / totalQuestions.toFloat()).coerceIn(0f, 1f)

    /** Human counter, 1-based, e.g. "3/10" while question three is on screen. */
    val displayNumber: Int get() = (questionIndex + 1).coerceAtMost(totalQuestions.coerceAtLeast(1))

    fun isCorrectOption(index: Int): Boolean = revealed && question?.correctIndex == index
    fun isWrongPick(index: Int): Boolean = revealed && selectedIndex == index && question?.correctIndex != index
}

class QuizViewModel(
    private val repository: GameRepository,
    private val soundManager: SoundManager,
    private val categoryId: String
) : ViewModel() {

    private val category: QuizCategory? = QuizContent.category(categoryId)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var advanceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.data
                .map { it.quizBestFor(categoryId) }
                .distinctUntilChanged()
                .collect { best -> _state.update { it.copy(bestScore = best) } }
        }
    }

    private fun initialState(): QuizUiState {
        val questions = category?.questions.orEmpty().filter { it.isValid }
        return QuizUiState(
            categoryId = categoryId,
            categoryTitle = category?.title.orEmpty(),
            mascotIndex = category?.mascotIndex ?: 0,
            questionIndex = 0,
            totalQuestions = questions.size,
            question = questions.firstOrNull(),
            valid = category != null && questions.isNotEmpty()
        )
    }

    private fun questions(): List<QuizQuestion> =
        category?.questions.orEmpty().filter { it.isValid }

    /** Handles a tap on answer [index]. Ignored while revealing or paused. */
    fun answer(index: Int) {
        val current = _state.value
        val question = current.question ?: return
        if (current.revealed || current.finished || current.paused) return
        if (index !in question.options.indices) return

        val correct = index == question.correctIndex
        soundManager.play(if (correct) Sfx.CORRECT else Sfx.WRONG)

        _state.update {
            it.copy(
                selectedIndex = index,
                revealed = true,
                score = it.score + if (correct) 1 else 0
            )
        }

        advanceJob?.cancel()
        advanceJob = viewModelScope.launch {
            delay(REVEAL_MILLIS)
            // Holding the pause overlay open holds the reveal open too.
            while (_state.value.paused) delay(PAUSE_POLL_MILLIS)
            advance()
        }
    }

    private suspend fun advance() {
        val all = questions()
        val current = _state.value
        val next = current.questionIndex + 1
        if (next >= all.size) {
            finish(current.score, all.size)
        } else {
            _state.update {
                it.copy(
                    questionIndex = next,
                    question = all.getOrNull(next),
                    selectedIndex = null,
                    revealed = false
                )
            }
        }
    }

    private suspend fun finish(score: Int, total: Int) {
        _state.update { it.copy(finished = true, revealed = true) }
        soundManager.play(if (total > 0 && score * 2 >= total) Sfx.WIN else Sfx.LOSE)
        repository.saveQuizBest(categoryId, score)
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

    /** Starts the same category over from question one. */
    fun restart() {
        soundManager.play(Sfx.CLICK)
        advanceJob?.cancel()
        advanceJob = null
        val best = _state.value.bestScore
        _state.value = initialState().copy(bestScore = best)
    }

    override fun onCleared() {
        advanceJob?.cancel()
        super.onCleared()
    }

    companion object {
        /** Reveal window before the quiz auto-advances. */
        const val REVEAL_MILLIS: Long = 750L
        private const val PAUSE_POLL_MILLIS: Long = 80L
    }
}

class QuizViewModelFactory(
    private val repository: GameRepository,
    private val soundManager: SoundManager,
    private val categoryId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            "Unexpected ViewModel class ${modelClass.name}"
        }
        return QuizViewModel(repository, soundManager, categoryId) as T
    }
}
