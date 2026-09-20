package com.mercadomania.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.data.GameData
import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.game.QuizContent
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.GoldRule
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.ProgressStrip
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette

/** Best score per quiz category plus levels cleared out of nine. */
@Composable
fun ResultsSummaryScreen(
    data: GameData,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = GameLogic.QUIZ_QUESTIONS_PER_CATEGORY
    val cleared = GameLogic.completedLevelCount(data.pairsBest)
    val anyProgress = data.quizBest.values.any { it > 0 } || data.pairsBest.values.any { it > 0 }

    BazaarBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            ScreenHeader(
                title = stringResource(R.string.results_title),
                backDescription = stringResource(R.string.cd_back),
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!anyProgress) {
                    ModalPanel(modifier = Modifier.fillMaxWidth()) {
                        PanelBody(text = stringResource(R.string.results_empty))
                    }
                }

                ModalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.results_quiz_header)
                ) {
                    QuizContent.categories.forEach { category ->
                        val best = data.quizBestFor(category.id)
                        ResultRow(
                            iconIndex = category.iconIndex,
                            label = category.title,
                            value = if (best > 0) {
                                stringResource(R.string.quiz_score, best, total)
                            } else {
                                stringResource(R.string.quiz_not_played)
                            },
                            highlighted = best > 0
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                ModalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.results_pairs_header)
                ) {
                    Text(
                        text = stringResource(
                            R.string.pairs_cleared_summary,
                            cleared,
                            GameLogic.levelCount
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = BazaarPalette.Gold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    ProgressStrip(
                        progress = cleared.toFloat() / GameLogic.levelCount.toFloat(),
                        label = "$cleared / ${GameLogic.levelCount}"
                    )
                    Spacer(Modifier.height(14.dp))
                    GoldRule()
                    Spacer(Modifier.height(12.dp))
                    GameLogic.levels.forEach { config ->
                        val best = data.pairsBestFor(config.level)
                        ResultRow(
                            iconIndex = (config.level - 1) % GameLogic.ICON_COUNT,
                            label = stringResource(R.string.pairs_level, config.level),
                            value = stringResource(R.string.pairs_best, best, config.pairs),
                            highlighted = best >= config.pairs
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun ResultRow(
    iconIndex: Int,
    label: String,
    value: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "$label, $value" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Assets.item(iconIndex)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = BazaarPalette.Cream,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = if (highlighted) BazaarPalette.GemCyan else BazaarPalette.MutedSlate
        )
    }
}
