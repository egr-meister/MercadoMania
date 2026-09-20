package com.mercadomania.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.ScreenHeader

/** How both modes work, in Compose-drawn panels that grow with their text. */
@Composable
fun RulesScreen(
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
                title = stringResource(R.string.rules_title),
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
                ModalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.rules_quiz_header)
                ) {
                    PanelBody(
                        text = stringResource(R.string.rules_quiz_body),
                        align = TextAlign.Start
                    )
                }
                ModalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.rules_pairs_header)
                ) {
                    PanelBody(
                        text = stringResource(R.string.rules_pairs_body),
                        align = TextAlign.Start
                    )
                }
                ModalPanel(
                    modifier = Modifier.fillMaxWidth(),
                    title = stringResource(R.string.rules_general_header)
                ) {
                    PanelBody(
                        text = stringResource(R.string.rules_general_body),
                        align = TextAlign.Start
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
