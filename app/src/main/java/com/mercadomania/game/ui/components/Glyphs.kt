package com.mercadomania.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small vector glyphs drawn straight onto a Canvas.
 *
 * They exist so that state is never signalled by colour alone - a correct
 * answer also carries a check, a wrong one a cross, a locked level a padlock -
 * without pulling in an icon dependency the app does not otherwise need.
 */

@Composable
fun CheckGlyph(size: Dp = 20.dp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.18f, w * 0.52f)
            lineTo(w * 0.42f, w * 0.77f)
            lineTo(w * 0.84f, w * 0.25f)
        }
        drawPath(path, color = color, style = Stroke(width = w * 0.17f, cap = StrokeCap.Round))
    }
}

@Composable
fun CrossGlyph(size: Dp = 20.dp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.17f
        drawLine(color, Offset(w * 0.25f, w * 0.25f), Offset(w * 0.75f, w * 0.75f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, w * 0.25f), Offset(w * 0.25f, w * 0.75f), sw, StrokeCap.Round)
    }
}

@Composable
fun LockGlyph(size: Dp = 22.dp, color: Color = Color.White, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.28f, w * 0.15f),
            size = Size(w * 0.44f, w * 0.44f),
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.19f, w * 0.43f),
            size = Size(w * 0.62f, w * 0.42f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
    }
}

/** A four-point sparkle, used as decorative punctuation around the bazaar. */
fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    val waist = radius * 0.17f
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticBezierTo(center.x + waist, center.y - waist, center.x + radius, center.y)
        quadraticBezierTo(center.x + waist, center.y + waist, center.x, center.y + radius)
        quadraticBezierTo(center.x - waist, center.y + waist, center.x - radius, center.y)
        quadraticBezierTo(center.x - waist, center.y - waist, center.x, center.y - radius)
        close()
    }
    drawPath(path, color = color, alpha = alpha)
}

/** A hanging diamond: the repeating unit of the bunting on the main screen. */
fun DrawScope.drawBuntingDiamond(rect: Rect, color: Color, alpha: Float = 1f) {
    val path = Path().apply {
        moveTo(rect.center.x, rect.top)
        lineTo(rect.right, rect.center.y)
        lineTo(rect.center.x, rect.bottom)
        lineTo(rect.left, rect.center.y)
        close()
    }
    drawPath(path, color = color, alpha = alpha)
}
