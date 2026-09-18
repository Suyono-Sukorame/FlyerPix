package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.ShapeType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val DialogPrimaryBlue      = Color(0xFF1769FF)
private val DialogTextPrimary      = Color(0xFF1E293B)
private val DialogTextSecondary    = Color(0xFF64748B)
private val DialogSelectedBg       = Color(0xFFE8F0FE)
private val DialogUnselectedBg     = Color(0xFFF8FAFC)
private val DialogSelectedBorder   = Color(0xFF1769FF)
private val DialogUnselectedBorder = Color(0xFFE2E8F0)

/** Urutan 9 bentuk esensial dalam grid 3 × 3. */
val SHAPE_TYPE_ENTRIES: List<Pair<ShapeType, String>> = listOf(
    ShapeType.RECTANGLE         to "Rectangle",
    ShapeType.ROUNDED_RECTANGLE to "Rounded",
    ShapeType.CIRCLE            to "Circle",
    ShapeType.TRIANGLE          to "Triangle",
    ShapeType.STAR              to "Star",
    ShapeType.HEART             to "Heart",
    ShapeType.HEXAGON           to "Hexagon",
    ShapeType.DIAMOND           to "Diamond",
    ShapeType.ARC               to "Arc"
)

/** Label nama bentuk dari [ShapeType]. */
fun shapeTypeLabel(type: ShapeType): String =
    SHAPE_TYPE_ENTRIES.firstOrNull { it.first == type }?.second ?: type.name.lowercase().replaceFirstChar { it.uppercase() }

/**
 * Pop-up dialog floating ala PixelLab untuk memilih tipe bentuk.
 * Kartu mengambang di tengah layar, sudut membulat 16.dp, grid 3 × 3,
 * menekan salah satu bentuk langsung memanggil [onShapeSelected] dan menutup dialog.
 *
 * @param currentType      Bentuk yang sedang aktif (disorot di grid)
 * @param onShapeSelected  Dipanggil sekali dengan tipe terpilih; dialog ditutup oleh pemanggil
 * @param onDismiss        Dipanggil saat dialog ditutup (tombol silang / tap luar)
 */
@Composable
fun ShapePickerPopupDialog(
    currentType: ShapeType,
    onShapeSelected: (ShapeType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 12.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Shapes",
                        color = DialogTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sharp_clear_24px),
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp),
                            tint = DialogTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SHAPE_TYPE_ENTRIES.chunked(3).forEach { rowEntries ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowEntries.forEach { (type, label) ->
                            DialogShapeCard(
                                type = type,
                                label = label,
                                isSelected = type == currentType,
                                onClick = { onShapeSelected(type) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogShapeCard(
    type: ShapeType,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) DialogSelectedBg else DialogUnselectedBg
    val borderColor = if (isSelected) DialogSelectedBorder else DialogUnselectedBorder
    val fillColor = if (isSelected) DialogPrimaryBlue else Color(0xFF7C8B9C)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            drawShapePickerPreview(type = type, fillColor = fillColor)
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DialogPrimaryBlue else DialogTextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** Preview vektor bentuk untuk icon drawer (dipakai dialog & dropdown selector). */
fun DrawScope.drawShapePickerPreview(type: ShapeType, fillColor: Color) {
    val w = size.width
    val h = size.height
    val pad = w * 0.1f

    when (type) {
        ShapeType.RECTANGLE -> {
            drawRect(
                color = fillColor,
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2f, h - pad * 2f)
            )
        }
        ShapeType.ROUNDED_RECTANGLE -> {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2f, h - pad * 2f),
                cornerRadius = CornerRadius(w * 0.22f, h * 0.22f)
            )
        }
        ShapeType.CIRCLE -> {
            val radius = (min(w, h) - pad * 2f) / 2f
            drawCircle(
                color = fillColor,
                radius = radius,
                center = Offset(w / 2f, h / 2f)
            )
        }
        ShapeType.ARC -> {
            drawArc(
                color = fillColor,
                startAngle = 300f,
                sweepAngle = 270f,
                useCenter = true,
                topLeft = Offset(pad, pad),
                size = Size(w - pad * 2f, h - pad * 2f)
            )
        }
        ShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo(w / 2f, pad)
                lineTo(w - pad, h - pad)
                lineTo(pad, h - pad)
                close()
            }
            drawPath(path = path, color = fillColor)
        }
        ShapeType.STAR -> {
            val outerR = (min(w, h) / 2f) - pad
            val innerR = outerR * 0.45f
            val path = buildDialogStarPath(
                cx = w / 2f, cy = h / 2f,
                outerRadius = outerR,
                innerRadius = innerR,
                points = 5
            )
            drawPath(path = path, color = fillColor)
        }
        ShapeType.HEART -> {
            val sx = w / 32f
            val sy = h / 28.4f
            val path = Path().apply {
                moveTo(16f * sx, 28.4f * sy)
                cubicTo(6f * sx, 21.6f * sy, 0f * sx, 13.4f * sy, 0f * sx, 7.1f * sy)
                cubicTo(0f * sx, 2.4f * sy, 4.2f * sx, 0f * sy, 8f * sx, 0f * sy)
                cubicTo(12f * sx, 0f * sy, 16f * sx, 3.9f * sy, 16f * sx, 9f * sy)
                cubicTo(16f * sx, 3.9f * sy, 20f * sx, 0f * sy, 24f * sx, 0f * sy)
                cubicTo(27.8f * sx, 0f * sy, 32f * sx, 2.4f * sy, 32f * sx, 7.1f * sy)
                cubicTo(32f * sx, 13.4f * sy, 26f * sx, 21.6f * sy, 16f * sx, 28.4f * sy)
                close()
            }
            drawPath(path = path, color = fillColor)
        }
        ShapeType.HEXAGON -> {
            val cx = w / 2f
            val cy = h / 2f
            val rx = (min(w, h) - pad * 2f) / 2f
            val path = Path().apply {
                for (i in 0 until 6) {
                    val angle = (Math.PI / 3.0 * i - Math.PI / 2.0).toFloat()
                    val px = cx + rx * cos(angle)
                    val py = cy + rx * sin(angle)
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(path = path, color = fillColor)
        }
        ShapeType.DIAMOND -> {
            val path = Path().apply {
                moveTo(w / 2f, pad)
                lineTo(w - pad, h / 2f)
                lineTo(w / 2f, h - pad)
                lineTo(pad, h / 2f)
                close()
            }
            drawPath(path = path, color = fillColor)
        }
    }
}

/**
 * Membangun path bintang N-titik (versi dialog).
 */
private fun buildDialogStarPath(
    cx: Float, cy: Float,
    outerRadius: Float, innerRadius: Float,
    points: Int
): Path {
    val path = Path()
    val totalPoints = points * 2
    for (i in 0 until totalPoints) {
        val angle = (Math.PI / points * i - Math.PI / 2).toFloat()
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}