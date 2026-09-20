package com.mercadomania.game.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.CheckGlyph
import com.mercadomania.game.ui.components.LockGlyph
import com.mercadomania.game.ui.components.OutlinedLabel
import com.mercadomania.game.ui.components.PlateButton
import com.mercadomania.game.ui.components.ScreenHeader
import com.mercadomania.game.ui.theme.BazaarPalette

/**
 * Nine level bubbles in a three-by-three arrangement, plus a shortcut to the
 * rules. Level 1 is always open; every other level needs the previous one
 * cleared, and a locked bubble says so out loud as well as showing a padlock.
 */
@Composable
fun LevelsScreen(
    pairsBest: Map<String, Int>,
    onLevel: (Int) -> Unit,
    onRules: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BazaarBackdrop(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            // Hoisted out of BoxWithConstraintsScope: inside the nested
            // Column/Row lambdas `maxWidth` would need an explicit receiver.
            val screenWidth: Dp = maxWidth
            val bubbleSize: Dp = (screenWidth - 80.dp) / 3f
            val clearedCount = GameLogic.completedLevelCount(pairsBest)

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.pairs_levels_title),
                    backDescription = stringResource(R.string.cd_back),
                    onBack = onBack
                )

                Text(
                    text = stringResource(
                        R.string.pairs_cleared_summary,
                        clearedCount,
                        GameLogic.levelCount
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = BazaarPalette.GemCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GameLogic.levels.chunked(3).forEach { rowLevels ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                        ) {
                            rowLevels.forEach { config ->
                                LevelBubble(
                                    level = config.level,
                                    pairs = config.pairs,
                                    best = pairsBest[config.level.toString()]?.coerceAtLeast(0) ?: 0,
                                    unlocked = GameLogic.isLevelUnlocked(config.level, pairsBest),
                                    cleared = GameLogic.isLevelCompleted(config.level, pairsBest),
                                    size = bubbleSize,
                                    onClick = { onLevel(config.level) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    PlateButton(
                        text = stringResource(R.string.menu_rules),
                        onClick = onRules,
                        modifier = Modifier.width(screenWidth * 0.52f)
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LevelBubble(
    level: Int,
    pairs: Int,
    best: Int,
    unlocked: Boolean,
    cleared: Boolean,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = when {
        cleared -> stringResource(R.string.cd_level_cleared, level)
        unlocked -> stringResource(R.string.cd_level_unlocked, level)
        else -> stringResource(R.string.cd_level_locked, level, level - 1)
    }

    Column(
        modifier = modifier.width(size),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(
                    enabled = unlocked,
                    role = Role.Button,
                    onClick = onClick
                )
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Assets.plateRound),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (unlocked) 1f else 0.40f)
            )

            if (unlocked) {
                OutlinedLabel(
                    text = level.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(size * 0.5f)
                        .clip(CircleShape)
                        .background(BazaarPalette.Midnight.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    LockGlyph(size = size * 0.28f, color = BazaarPalette.Cream)
                }
            }

            if (cleared) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(size * 0.30f)
                        .clip(CircleShape)
                        .background(BazaarPalette.Emerald),
                    contentAlignment = Alignment.Center
                ) {
                    CheckGlyph(size = size * 0.20f)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.pairs_best, best, pairs),
            style = MaterialTheme.typography.labelMedium,
            color = if (best > 0) BazaarPalette.GemCyan else BazaarPalette.MutedSlate,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
