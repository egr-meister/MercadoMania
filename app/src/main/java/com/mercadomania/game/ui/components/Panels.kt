package com.mercadomania.game.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercadomania.game.ui.theme.BazaarPalette

/**
 * A modal panel, drawn entirely in Compose: rounded rectangle, vertical
 * gradient fill, gold accent border, plus a thin inner cyan rule.
 *
 * Deliberately NOT a fixed-shape frame image stretched to fit - the panel has
 * to hold anything from one line to a scrolling block of rules text, and a
 * stretched frame would distort at every size.
 */
@Composable
fun ModalPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    maxWidth: Dp = 420.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clip(PanelShape)
            .background(
                Brush.verticalGradient(
                    listOf(BazaarPalette.PanelTop, BazaarPalette.PanelBottom)
                )
            )
            .border(width = 3.dp, color = BazaarPalette.Gold, shape = PanelShape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(5.dp)
                .border(
                    width = 1.dp,
                    color = BazaarPalette.GemCyan.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(22.dp)
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!title.isNullOrBlank()) {
                OutlinedLabel(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fillColor = BazaarPalette.Gold,
                    outlineColor = BazaarPalette.Midnight,
                    outlineWidth = 6f,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                GoldRule()
                Spacer(Modifier.height(14.dp))
            }
            content()
        }
    }
}

/** A thin gold divider used inside panels. */
@Composable
fun GoldRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.55f)
            .height(2.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, BazaarPalette.Gold, Color.Transparent)
                )
            )
    )
}

/**
 * Full-screen scrim that hosts a [ModalPanel].
 *
 * It swallows taps so the game underneath cannot be played while a modal is
 * up, and it centres its content.
 */
@Composable
fun ModalOverlay(
    modifier: Modifier = Modifier,
    onScrimClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE6010B27))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = true
            ) { onScrimClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * The Continue / Restart / Menu triple shared by the pause and results panels.
 * Laid out as a row of plates so a panel stays a comfortable height.
 */
@Composable
fun PanelActionsRow(
    continueLabel: String,
    restartLabel: String,
    menuLabel: String,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    continueEnabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlateButton(
            text = continueLabel,
            onClick = onContinue,
            enabled = continueEnabled,
            textStyle = MaterialTheme.typography.labelMedium,
            labelWidthFraction = 0.82f,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        PlateButton(
            text = restartLabel,
            onClick = onRestart,
            textStyle = MaterialTheme.typography.labelMedium,
            labelWidthFraction = 0.82f,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        PlateButton(
            text = menuLabel,
            onClick = onMenu,
            textStyle = MaterialTheme.typography.labelMedium,
            labelWidthFraction = 0.82f,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Body copy inside a panel. */
@Composable
fun PanelBody(
    text: String,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Center,
    color: Color = BazaarPalette.Cream
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        textAlign = align,
        modifier = modifier.fillMaxWidth()
    )
}

internal val PanelShape = RoundedCornerShape(26.dp)
