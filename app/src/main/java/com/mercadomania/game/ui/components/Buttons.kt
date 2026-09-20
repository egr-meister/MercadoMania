package com.mercadomania.game.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.layout.layout
import com.mercadomania.game.ui.Assets
import com.mercadomania.game.ui.theme.BazaarPalette

/**
 * The house button: an image plate with the label drawn on top in code, in
 * white bold text.
 *
 * The plate keeps its own aspect ratio (no stretching), the caller controls
 * width, and the label is drawn twice - a dark outline pass then a white fill
 * pass - so white-on-gold stays legible instead of washing out. The whole
 * button is one semantics node with the label as its name.
 */
@Composable
fun PlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    plate: Int = Assets.btnPlate,
    plateAspect: Float = Assets.BTN_PLATE_ASPECT,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    labelWidthFraction: Float = 0.72f,
    maxLines: Int = 2,
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.955f else 1f,
        label = "plateButtonScale"
    )
    // Held in a local: inside a semantics lambda the receiver's own
    // `contentDescription` property would shadow the parameter.
    val spokenLabel = contentDescription ?: text

    Box(
        modifier = modifier
            .heightIn(min = MIN_TOUCH_TARGET)
            .aspectRatioKeepWidth(plateAspect)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = text,
                onClick = onClick
            )
            .clearAndSetSemantics {
                this.contentDescription = spokenLabel
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(plate),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        OutlinedLabel(
            text = text,
            style = textStyle,
            maxLines = maxLines,
            modifier = Modifier.fillMaxWidth(labelWidthFraction)
        )
    }
}

/**
 * The ornate framed plate, used for the main menu. It is a tall plate, so the
 * caller is expected to constrain its width (~0.7 of the screen) and let the
 * screen scroll.
 */
@Composable
fun MenuPlateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    PlateButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        plate = Assets.btnMenu,
        plateAspect = Assets.BTN_MENU_ASPECT,
        textStyle = MaterialTheme.typography.headlineSmall,
        labelWidthFraction = 0.68f,
        maxLines = 1,
        contentDescription = contentDescription
    )
}

/** Round image button used for Back and Pause. Always at least 48dp. */
@Composable
fun RoundIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.92f else 1f,
        label = "roundIconScale"
    )
    Image(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .sizeIn(minWidth = MIN_TOUCH_TARGET, minHeight = MIN_TOUCH_TARGET)
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    )
}

/**
 * White bold label with a dark outline, drawn on top of a plate.
 *
 * Two passes of the same text at the same position: a stroked dark pass for
 * separation from the gold, then the white fill.
 */
@Composable
fun OutlinedLabel(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fillColor: Color = Color.White,
    outlineColor: Color = BazaarPalette.Midnight,
    outlineWidth: Float = 7f,
    maxLines: Int = 2
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = style.copy(
                color = outlineColor,
                fontWeight = FontWeight.Bold,
                drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round, miter = 4f)
            ),
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = text,
            style = style.copy(color = fillColor, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Like `Modifier.aspectRatio`, but always derived from the measured width.
 * Keeps plates from being squashed when a parent hands down a tight height.
 */
private fun Modifier.aspectRatioKeepWidth(ratio: Float): Modifier = this.layout { measurable, constraints ->
    val safeRatio = if (ratio > 0f && ratio.isFinite()) ratio else 1f
    val width = when {
        constraints.hasBoundedWidth -> constraints.maxWidth
        else -> measurable.maxIntrinsicWidth(constraints.maxHeight)
    }
    val height = (width / safeRatio).toInt().coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = width,
            maxWidth = width,
            minHeight = height,
            maxHeight = height
        )
    )
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}

private val MIN_TOUCH_TARGET: Dp = 48.dp
