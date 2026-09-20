package com.mercadomania.game.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.theme.BazaarPalette

/**
 * Full-bleed bazaar background plus the readability scrim.
 *
 * The artwork is bright gold, so a vertical lapis scrim is laid over it: text
 * and plates sit on the scrim and always clear WCAG AA against it, whatever
 * the photograph behind is doing.
 */
@Composable
fun BazaarBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BazaarPalette.Midnight)
    ) {
        Image(
            painter = painterResource(Assets.bgMain),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BazaarPalette.ScrimTop,
                            BazaarPalette.ScrimMid,
                            BazaarPalette.ScrimBottom
                        )
                    )
                )
        )
        content()
    }
}

/**
 * Standard screen header: round back button, centred title, optional trailing
 * action. Used on every screen except Main.
 */
@Composable
fun ScreenHeader(
    title: String,
    backDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            icon = Assets.iconBack,
            contentDescription = backDescription,
            onClick = onBack
        )
        Spacer(Modifier.width(8.dp))
        OutlinedLabel(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fillColor = BazaarPalette.Gold,
            outlineColor = BazaarPalette.Midnight,
            outlineWidth = 8f,
            maxLines = 2,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            trailing?.invoke()
        }
    }
}

/** A small capsule used for meta information such as timers and counters. */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = BazaarPalette.GemCyan,
    contentDescription: String? = null
) {
    // Held in a local: inside a semantics lambda the receiver's own
    // `contentDescription` property would shadow the parameter.
    val spoken = contentDescription

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BazaarPalette.Midnight.copy(alpha = 0.78f))
            .border(1.5.dp, accent.copy(alpha = 0.8f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            textAlign = TextAlign.Center,
            modifier = if (spoken != null) {
                Modifier.clearAndSetSemantics { this.contentDescription = spoken }
            } else {
                Modifier
            }
        )
    }
}

/**
 * The quiz progress strip: a gold fill on a lapis track, plus a discrete
 * "n/10" label so progress is never conveyed by the bar alone.
 */
@Composable
fun ProgressStrip(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    progressDescription: String? = null
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(BazaarPalette.Midnight.copy(alpha = 0.85f))
                .border(1.5.dp, BazaarPalette.Gold.copy(alpha = 0.7f), RoundedCornerShape(50))
                .then(
                    if (progressDescription != null) {
                        Modifier.clearAndSetSemantics {
                            this.contentDescription = progressDescription
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            if (clamped > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .fillMaxHeight()
                        .padding(2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BazaarPalette.GoldDeep, BazaarPalette.GoldLight)
                            )
                        )
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = BazaarPalette.Cream
        )
    }
}
