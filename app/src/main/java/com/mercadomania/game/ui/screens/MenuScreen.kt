package com.mercadomania.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.MenuPlateButton
import com.mercadomania.game.ui.components.ScreenHeader

/**
 * The stall picker. Ornate framed plates, width-constrained to ~0.7 of the
 * screen and vertically scrollable, because the plate is a tall shape and five
 * of them will not fit on a short device.
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val plateWidth = maxWidth * 0.70f

            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = stringResource(R.string.menu_title),
                    backDescription = stringResource(R.string.cd_back),
                    onBack = onBack
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    Spacer(Modifier.height(12.dp))
                }
            }

        }
    }
}
