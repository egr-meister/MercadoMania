package com.mercadomania.game.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercadomania.game.R
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.components.MenuPlateButton
import com.mercadomania.game.ui.components.drawBuntingDiamond
import com.mercadomania.game.ui.components.drawSparkle
import com.mercadomania.game.ui.theme.BazaarPalette
import kotlin.math.sin

/**
 * The title screen, composed as a lit stall rather than a dashboard.
 *
 * Reading down the screen: a swaying bunting of gold diamonds strung across
 * the top, the wordmark hanging from it on a cord, the Start plate floating in
 * the middle of the empty market floor, and the three mascots arranged along
 * the bottom edge as an audience - the gecko low on the left, the automaton
 * low on the right, the genie rising large from the centre in front of both.
 *
 * Deliberately not the generic mascot -> title -> stats card -> button stack:
 * there is no stats card here at all, the mascots are staged rather than
 * centred, and the single call to action sits inside the composition instead
 * of under it.
 */
@Composable
fun MainScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    BazaarBackdrop(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            val transition = rememberInfiniteTransition(label = "bazaarIdle")
            val sway by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 5200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sway"
            )
            val twinkle by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2400),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "twinkle"
            )

            // --- Bunting strung across the very top -------------------------
            Bunting(
                sway = sway,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(76.dp)
            )

            // --- Wordmark, hanging from the bunting -------------------------
            Image(
                painter = painterResource(Assets.logo),
                contentDescription = stringResource(R.string.cd_logo),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = screenHeight * 0.075f)
                    .width(screenWidth * 0.70f)
                    .graphicsLayer {
                        rotationZ = (sway - 0.5f) * 2.4f
                        translationY = sin(sway * 3.14159f) * 6f
                    }
            )

            // --- Sparkle dust around the middle of the floor ----------------
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(screenHeight * 0.30f)
            ) {
                val w = size.width
                val h = size.height
                drawSparkle(Offset(w * 0.14f, h * 0.22f), w * 0.035f, BazaarPalette.GoldLight, twinkle)
                drawSparkle(Offset(w * 0.88f, h * 0.34f), w * 0.028f, BazaarPalette.GemCyan, 1.35f - twinkle)
                drawSparkle(Offset(w * 0.22f, h * 0.78f), w * 0.022f, BazaarPalette.GemCyan, twinkle * 0.8f)
                drawSparkle(Offset(w * 0.80f, h * 0.80f), w * 0.032f, BazaarPalette.GoldLight, 1.3f - twinkle)
            }

            // --- Mascots along the bottom edge ------------------------------
            Image(
                painter = painterResource(Assets.mascotB),
                contentDescription = stringResource(R.string.cd_mascot_gecko),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -screenWidth * 0.045f, y = -screenHeight * 0.012f)
                    .height(screenHeight * 0.215f)
            )
            Image(
                painter = painterResource(Assets.mascotC),
                contentDescription = stringResource(R.string.cd_mascot_robot),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = screenWidth * 0.035f, y = -screenHeight * 0.02f)
                    .height(screenHeight * 0.20f)
            )
            Image(
                painter = painterResource(Assets.mascotA),
                contentDescription = stringResource(R.string.cd_mascot_genie),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -screenHeight * 0.005f)
                    .height(screenHeight * 0.30f)
                    .graphicsLayer {
                        translationY = sin(sway * 3.14159f) * -9f
                    }
            )

            // --- The one call to action, inside the scene -------------------
            MenuPlateButton(
                text = stringResource(R.string.action_start),
                onClick = onStart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -screenHeight * 0.305f)
                    .width(screenWidth * 0.62f)
            )
        }
    }
}

/** Papel-picado style bunting: a slack cord hung with alternating diamonds. */
@Composable
private fun Bunting(sway: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dip = h * (0.30f + sway * 0.06f)
            val cord = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h * 0.10f)
                quadraticBezierTo(w * 0.5f, dip + h * 0.18f, w, h * 0.10f)
            }
            drawPath(
                path = cord,
                color = BazaarPalette.Gold,
                style = Stroke(width = h * 0.045f, cap = StrokeCap.Round)
            )

            val count = 9
            val flagSize = w / (count * 1.9f)
            for (i in 0 until count) {
                val t = (i + 0.5f) / count
                // Point on the quadratic cord.
                val x = w * t
                val y = (1 - t) * (1 - t) * (h * 0.10f) +
                    2 * (1 - t) * t * (dip + h * 0.18f) +
                    t * t * (h * 0.10f)
                val tilt = (sway - 0.5f) * 0.10f * flagSize
                val color = when (i % 3) {
                    0 -> BazaarPalette.Gold
                    1 -> BazaarPalette.GemCyan
                    else -> BazaarPalette.GoldLight
                }
                drawBuntingDiamond(
                    rect = Rect(
                        left = x - flagSize * 0.42f + tilt,
                        top = y + flagSize * 0.18f,
                        right = x + flagSize * 0.42f + tilt,
                        bottom = y + flagSize * 1.32f
                    ),
                    color = color,
                    alpha = 0.92f
                )
            }
        }
    }
}
