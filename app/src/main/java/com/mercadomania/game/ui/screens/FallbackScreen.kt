package com.mercadomania.game.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.ModalPanel
import com.mercadomania.game.ui.components.PanelBody
import com.mercadomania.game.ui.components.PlateButton

/**
 * Shown whenever a destination cannot render: an unknown quiz category, a
 * level number outside 1..9, a level that is still locked, or a back stack
 * restored after the save was wiped. Always offers a way back.
 */
@Composable
fun FallbackScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null
) {
    BazaarBackdrop(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            ModalPanel(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.fallback_title)
            ) {
                PanelBody(text = message ?: stringResource(R.string.fallback_body))
                Spacer(Modifier.height(20.dp))
                PlateButton(
                    text = stringResource(R.string.action_back),
                    onClick = onBack,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}
