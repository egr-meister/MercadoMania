package com.mercadomania.game.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.ProgressStrip

/**
 * Shown between the system splash and the main screen while startup work
 * finishes: reading saved progress, decoding the SFX and warming the artwork.
 *
 * The bar is fed by real completion, not a timer, so it tells the player the
 * app is working rather than hung. The value is animated only to smooth the
 * jumps between units of work.
 */
@Composable
fun LoadingScreen(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 220),
        label = "loadingProgress"
    )
    val percent = (animated * 100f).toInt().coerceIn(0, 100)

    BazaarBackdrop(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(Assets.logo),
                    contentDescription = stringResource(R.string.cd_logo),
                    modifier = Modifier.width(screenWidth * 0.72f)
                )

                Spacer(Modifier.height(36.dp))

                Box(modifier = Modifier.width(screenWidth * 0.66f)) {
                    ProgressStrip(
                        progress = animated,
                        label = stringResource(R.string.loading_percent, percent),
                        progressDescription = stringResource(R.string.cd_loading, percent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // The host waits below, decorative only.
            Image(
                painter = painterResource(Assets.mascotA),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(screenHeight * 0.24f)
                    .clearAndSetSemantics { }
            )
        }
    }
}
