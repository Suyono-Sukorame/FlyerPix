package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.ShapeType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val PickerPrimaryBlue      = Color(0xFF1769FF)
private val PickerTextPrimary      = Color(0xFF1E293B)
private val PickerTextSecondary    = Color(0xFF64748B)
private val PickerSurfaceBg        = Color(0xFFFFFFFF)
private val PickerDragHandle       = Color(0xFFCBD5E1)
private val PickerSelectedBg       = Color(0xFFE8F0FE)
private val PickerUnselectedBg     = Color(0xFFF8FAFC)
private val PickerSelectedBorder   = Color(0xFF1769FF)
private val PickerUnselectedBorder = Color(0xFFE2E8F0)

private val SHAPE_ENTRIES = listOf(
    ShapeType.RECTANGLE         to "Rectangle",
    ShapeType.ROUNDED_RECTANGLE to "Rounded",
    ShapeType.CIRCLE            to "Circle",
    ShapeType.TRIANGLE          to "Triangle",
    ShapeType.STAR              to "Star"
)

/**
 * Sheet compact untuk memilih tipe Shape sebelum layer dibuat.
 * Mirip dengan StickerDetailPage — user memilih lebih dulu, baru layer dibuat.
 *
 * @param onShapeTypeSelected Dipanggil dengan ShapeType yang dipilih user
 * @param onClose             Dipanggil ketika user membatalkan
 * @param maxHeightPx         Tinggi maksimal sheet dalam pixel
 */
@Composable
fun ShapePickerPage(
    onShapeTypeSelected: (ShapeType) -> Unit,
    onClose: () -> Unit,
    maxHeightPx: Int = 320
) {
    var selectedType by remember { mutableStateOf<ShapeType?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { maxHeightPx.toDp() }),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = 8.dp,
            backgroundColor = PickerSurfaceBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp, bottom = 10.dp, start = 14.dp, end = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(PickerDragHandle, CircleShape)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pilih Bentuk",
                        color = PickerTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Batal",
                            color = PickerTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pilih bentuk yang ingin ditambahkan ke canvas",
                    color = PickerTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Shape grid — 5 items in a single row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SHAPE_ENTRIES.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PickerSelectedBg else PickerUnselectedBg)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PickerSelectedBorder else PickerUnselectedBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedType = type
                                    onShapeTypeSelected(type)
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Visual shape preview using Compose Canvas
                                Canvas(modifier = Modifier.size(36.dp)) {
                                    drawShapePreview(
                                        type = type,
                                        fillColor = if (isSelected) PickerPrimaryBlue else Color(0xFFCBD5E1)
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PickerPrimaryBlue else PickerTextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Setelah memilih, Anda bisa mengatur warna dan ukurannya",
                    color = PickerTextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Helper menggambar preview bentuk geometris dalam DrawScope Compose Canvas.
 */
private fun DrawScope.drawShapePreview(type: ShapeType, fillColor: Color) {
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
            val path = buildStarPath(
                cx = w / 2f, cy = h / 2f,
                outerRadius = outerR,
                innerRadius = innerR,
                points = 5
            )
            drawPath(path = path, color = fillColor)
        }
    }
}

/**
 * Membangun path bintang N-titik.
 */
private fun buildStarPath(
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
