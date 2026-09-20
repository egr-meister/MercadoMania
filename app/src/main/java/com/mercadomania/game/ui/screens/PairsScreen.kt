package com.mercadomania.game.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.CheckGlyph
import com.mercadomania.game.ui.components.InfoPill
import com.mercadomania.game.ui.components.ModalOverlay
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelActionsRow
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.RoundIconButton
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette
import com.mercadomania.game.ui.viewmodel.PairCard
import com.mercadomania.game.ui.viewmodel.PairsOutcome
import com.mercadomania.game.ui.viewmodel.PairsUiState

/** The Matching Pairs board, its countdown, and its pause/results panels. */
@Composable
fun PairsScreen(
    state: PairsUiState,
    onFlip: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    continueEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state.paused) { onResume() }

    BazaarBackdrop(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val horizontalPadding = 16.dp
            val gap = 8.dp
            val columns = state.columns.coerceAtLeast(1)

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.pairs_level, state.level),
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    val minutes = state.secondsLeft / 60
                    val seconds = state.secondsLeft % 60
                    InfoPill(
                        text = stringResource(R.string.pairs_time, minutes, seconds),
                        accent = if (state.secondsLeft <= 10) BazaarPalette.Ruby else BazaarPalette.GemCyan
                    )
                    InfoPill(
                        text = stringResource(R.string.pairs_found, state.matchedPairs, state.pairs),
                        accent = BazaarPalette.Gold
                    )
                }

                // The whole board has to be visible at once - a memory game
                // where you scroll to see half the cards is unplayable. So the
                // card size is derived from BOTH axes of the space actually
                // left over, and the smaller of the two limits wins.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding, vertical = 8.dp)
                ) {
                    val gridWidth = maxWidth
                    val gridHeight = maxHeight
                    val rowCount = ((state.cards.size + columns - 1) / columns)
                        .coerceAtLeast(1)

                    val widthLimited = (gridWidth - gap * (columns - 1)) / columns
                    val heightLimited =
                        ((gridHeight - gap * (rowCount - 1)) / rowCount) * CARD_ASPECT
                    val cardWidth: Dp = minOf(widthLimited, heightLimited)
                        .coerceAtLeast(MIN_CARD_WIDTH)

                    // The scroll is a safety net only: it engages solely when
                    // a very small screen or a huge font scale makes even
                    // MIN_CARD_WIDTH cards overflow.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = gridHeight),
                            verticalArrangement = Arrangement.spacedBy(
                                gap,
                                Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            state.cards.chunked(columns).forEach { rowCards ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        gap,
                                        Alignment.CenterHorizontally
                                    )
                                ) {
                                    rowCards.forEach { card ->
                                        MemoryCard(
                                            card = card,
                                            width = cardWidth,
                                            enabled = !state.paused &&
                                                !state.finished &&
                                                !state.inputLocked,
                                            onClick = { onFlip(card.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.paused && !state.finished) {
                ModalOverlay {
                    ModalPanel(title = stringResource(R.string.pause_title)) {
                        PanelBody(
                            text = stringResource(
                                R.string.pairs_found,
                                state.matchedPairs,
                                state.pairs
                            )
                        )
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
                val won = state.outcome == PairsOutcome.WIN
                ModalOverlay {
                    ModalPanel(
                        title = stringResource(
                            if (won) R.string.pairs_win else R.string.pairs_lose
                        )
                    ) {
                        Text(
                            text = stringResource(
                                R.string.pairs_found,
                                state.matchedPairs,
                                state.pairs
                            ),
                            style = MaterialTheme.typography.displayMedium,
                            color = if (won) BazaarPalette.Gold else BazaarPalette.Cream,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        PanelBody(
                            text = stringResource(
                                R.string.pairs_best,
                                maxOf(state.bestPairs, state.matchedPairs),
                                state.pairs
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
                            onMenu = onMenu,
                            continueEnabled = continueEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    card: PairCard,
    width: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val revealed = card.faceUp || card.matched
    val rotation by animateFloatAsState(
        targetValue = if (revealed) 180f else 0f,
        animationSpec = tween(durationMillis = 280),
        label = "cardFlip"
    )

    val itemName = Assets.itemName(card.iconIndex)
    val description = when {
        card.matched -> stringResource(R.string.cd_card_matched, itemName)
        card.faceUp -> stringResource(R.string.cd_card_face_up, itemName)
        else -> stringResource(R.string.cd_card_face_down)
    }

    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(CARD_ASPECT)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clickable(
                enabled = enabled && !revealed,
                role = Role.Button,
                onClick = onClick
            )
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            CardBack()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                CardFace(card = card)
            }
        }
    }
}

@Composable
private fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(BazaarPalette.Azure, BazaarPalette.Midnight)
                )
            )
            .border(2.dp, BazaarPalette.Gold, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Assets.splashLogo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.62f)
        )
    }
}

@Composable
private fun CardFace(card: PairCard, modifier: Modifier = Modifier) {
    val borderColor = if (card.matched) BazaarPalette.Emerald else BazaarPalette.Gold
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(BazaarPalette.PanelTop, BazaarPalette.PanelBottom)
                )
            )
            .border(3.dp, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(Assets.item(card.iconIndex)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.82f)
        )
        if (card.matched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BazaarPalette.Emerald),
                contentAlignment = Alignment.Center
            ) {
                CheckGlyph(size = 13.dp)
            }
        }
    }
}

/** Card width / height. The grid maths and the card itself must agree. */
private const val CARD_ASPECT = 0.78f

/** Never shrink a card below a comfortable touch target. */
private val MIN_CARD_WIDTH = 44.dp
