package com.mercadomania.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.CheckGlyph
import com.mercadomania.game.ui.components.CrossGlyph
import com.mercadomania.game.ui.components.ModalOverlay
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelActionsRow
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.ProgressStrip
import com.mercadomania.game.ui.components.RoundIconButton
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette
import com.mercadomania.game.ui.viewmodel.QuizUiState

/**
 * One question at a time, with a progress strip and an "n/10" counter.
 *
 * Answer options are drawn in Compose rather than stamped on a plate image:
 * four fixed-aspect plates cannot fit on one portrait screen, and an option
 * has to grow with its text (and with the user's font scale).
 */
@Composable
fun QuizScreen(
    state: QuizUiState,
    onAnswer: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Back closes the pause overlay first; otherwise it leaves the screen.
    BackHandler(enabled = state.paused) { onResume() }

    BazaarBackdrop(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Hoisted out of BoxWithConstraintsScope: the nested Column
            // lambdas below cannot call `maxHeight` with an implicit receiver.
            val mascotHeight: Dp = (maxHeight * 0.20f).coerceIn(110.dp, 220.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = state.categoryTitle.ifBlank { stringResource(R.string.quiz_title) },
                    backDescription = stringResource(R.string.cd_back),
                    onBack = onBack,
                    trailing = {
                        if (!state.finished) {
                            RoundIconButton(
                                icon = Assets.iconPause,
                                contentDescription = stringResource(R.string.cd_pause),
                                onClick = onPause
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProgressStrip(
                        progress = state.progress,
                        label = stringResource(
                            R.string.quiz_progress,
                            state.displayNumber,
                            state.totalQuestions
                        ),
                        progressDescription = stringResource(
                            R.string.quiz_question_number,
                            state.displayNumber,
                            state.totalQuestions
                        )
                    )

                    Spacer(Modifier.height(18.dp))

                    ModalPanel(modifier = Modifier.fillMaxWidth()) {
                        PanelBody(text = state.question?.text.orEmpty())
                    }

                    Spacer(Modifier.height(18.dp))

                    val options = state.question?.options.orEmpty()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        options.forEachIndexed { index, option ->
                            AnswerOption(
                                text = option,
                                index = index,
                                state = state,
                                onClick = { onAnswer(index) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // The category's host, filling the space under the answers.
                    // Decorative: the question and every option are already read
                    // out, so a screen reader gains nothing from announcing it.
                    Image(
                        painter = painterResource(Assets.mascot(state.mascotIndex)),
                        contentDescription = null,
                        modifier = Modifier
                            .height(mascotHeight)
                            .clearAndSetSemantics { }
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (state.paused && !state.finished) {
            ModalOverlay {
                ModalPanel(title = stringResource(R.string.pause_title)) {
                    PanelBody(text = stringResource(R.string.quiz_score, state.score, state.totalQuestions))
                    Spacer(Modifier.height(18.dp))
                    PanelActionsRow(
                        continueLabel = stringResource(R.string.action_continue),
                        restartLabel = stringResource(R.string.action_restart),
                        menuLabel = stringResource(R.string.action_menu),
                        onContinue = onResume,
                        onRestart = onRestart,
                        onMenu = onMenu
                    )
                }
            }
        }

        if (state.finished) {
            ModalOverlay {
                ModalPanel(title = stringResource(R.string.quiz_title)) {
                    Text(
                        text = stringResource(R.string.quiz_score, state.score, state.totalQuestions),
                        style = MaterialTheme.typography.displayMedium,
                        color = BazaarPalette.Gold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    PanelBody(
                        text = stringResource(
                            R.string.quiz_best,
                            maxOf(state.bestScore, state.score),
                            state.totalQuestions
                        ),
                        color = BazaarPalette.GemCyan
                    )
                    Spacer(Modifier.height(18.dp))
                    PanelActionsRow(
                        continueLabel = stringResource(R.string.action_continue),
                        restartLabel = stringResource(R.string.action_restart),
                        menuLabel = stringResource(R.string.action_menu),
                        onContinue = onContinue,
                        onRestart = onRestart,
                        onMenu = onMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    state: QuizUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCorrect = state.isCorrectOption(index)
    val isWrong = state.isWrongPick(index)

    val targetBorder = when {
        isCorrect -> BazaarPalette.Emerald
        isWrong -> BazaarPalette.Ruby
        else -> BazaarPalette.Gold
    }
    val borderColor by animateColorAsState(targetBorder, label = "answerBorder")

    val fill: Brush = when {
        isCorrect -> Brush.horizontalGradient(
            listOf(Color(0xFF14532D), BazaarPalette.Emerald.copy(alpha = 0.85f))
        )
        isWrong -> Brush.horizontalGradient(
            listOf(Color(0xFF5A1010), BazaarPalette.Ruby.copy(alpha = 0.85f))
        )
        else -> Brush.horizontalGradient(
            listOf(BazaarPalette.PanelTop, BazaarPalette.PanelBottom)
        )
    }

    val stateLabel = when {
        isCorrect -> stringResource(R.string.quiz_answer_correct)
        isWrong -> stringResource(R.string.quiz_answer_wrong)
        else -> null
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(
                enabled = !state.revealed && !state.paused && !state.finished,
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick
            )
            .semantics {
                stateLabel?.let { stateDescription = it }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        // Colour is never the only signal: the reveal also carries a glyph.
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            when {
                isCorrect -> CheckGlyph(size = 22.dp, color = Color.White)
                isWrong -> CrossGlyph(size = 20.dp, color = Color.White)
            }
        }
    }
}
