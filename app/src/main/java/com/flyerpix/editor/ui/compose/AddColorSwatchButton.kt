package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * Tombol bulat (sama besar dengan swatch warna) ber-border putus-putus dengan tanda "+".
 * Dipakai untuk mengganti tombol "More..." yang membuka color picker.
 */
@Composable
fun AddColorSwatchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 30.dp,
    borderColor: Color = Color(0xFFCBD5E1),
    plusColor: Color = Color(0xFF1769FF)
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .drawBehind {
                val strokeWidthPx = 1.5.dp.toPx()
                val radius = size.minDimension / 2f - strokeWidthPx
                val center = Offset(size.width / 2f, size.height / 2f)
                val dashOn = 5.dp.toPx()
                val dashOff = 4.dp.toPx()
                val radiusDegrees = (dashOn * 180f / (radius * PI.toFloat())).coerceAtMost(360f)
                val gapDegrees = (dashOff * 180f / (radius * PI.toFloat())).coerceAtMost(360f)
                var angle = 0f
                while (angle < 360f) {
                    drawArc(
                        color = borderColor,
                        startAngle = angle,
                        sweepAngle = radiusDegrees,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = strokeWidthPx)
                    )
                    angle += radiusDegrees + gapDegrees
                }
            }
            .clip(CircleShape)
            .background(Color(0xFFF7F9FC))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(buttonSize - 16.dp)) {
            val strokeWidth = 2.dp.toPx()
            val w = size.width
            val h = size.height
            drawLine(
                color = plusColor,
                start = Offset(w / 2f, 0f),
                end = Offset(w / 2f, h),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = plusColor,
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}