package com.mercadomania.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.MenuPlateButton
import com.mercadomania.game.ui.components.ScreenHeader

/**
 * The stall picker.
 *
 * The ornate plate is a wide, tall shape, so its width is derived from the
 * height actually left under the header: all five entries fit on one screen
 * without scrolling, and the plate never grows past ~0.72 of the screen width.
 */
@Composable
fun MenuScreen(
    onQuiz: () -> Unit,
    onPairs: () -> Unit,
    onResults: () -> Unit,
    onSettings: () -> Unit,
    onRules: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BazaarBackdrop(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            ScreenHeader(
                title = stringResource(R.string.menu_title),
                backDescription = stringResource(R.string.cd_back),
                onBack = onBack
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val listWidth = maxWidth
                val listHeight = maxHeight

                val heightLimited =
                    ((listHeight - MENU_GAP * (MENU_ITEM_COUNT - 1)) / MENU_ITEM_COUNT) *
                        Assets.BTN_MENU_ASPECT
                val plateWidth: Dp = minOf(listWidth * 0.72f, heightLimited)
                    .coerceAtLeast(MIN_PLATE_WIDTH)

                // Scroll is a safety net for very short screens only.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = listHeight),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            MENU_GAP,
                            Alignment.CenterVertically
                        )
                    ) {
                        MenuPlateButton(
                            text = stringResource(R.string.menu_quiz),
                            onClick = onQuiz,
                            modifier = Modifier.width(plateWidth)
                        )
                        MenuPlateButton(
                            text = stringResource(R.string.menu_pairs),
                            onClick = onPairs,
                            modifier = Modifier.width(plateWidth)
                        )
                        MenuPlateButton(
                            text = stringResource(R.string.menu_results),
                            onClick = onResults,
                            modifier = Modifier.width(plateWidth)
                        )
                        MenuPlateButton(
                            text = stringResource(R.string.menu_settings),
                            onClick = onSettings,
                            modifier = Modifier.width(plateWidth)
                        )
                        MenuPlateButton(
                            text = stringResource(R.string.menu_rules),
                            onClick = onRules,
                            modifier = Modifier.width(plateWidth)
                        )
                    }
                }
            }
        }
    }
}

private const val MENU_ITEM_COUNT = 5
private val MENU_GAP: Dp = 8.dp
private val MIN_PLATE_WIDTH: Dp = 150.dp
