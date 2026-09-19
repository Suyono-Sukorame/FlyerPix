package com.flyerpix.editor.ui.compose

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlin.math.absoluteValue
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.StrokeStyle

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val SWATCH_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF1769FF.toInt(),
    0xFFE53935.toInt(),
    0xFF43A047.toInt(),
    0xFFFFB300.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt(),
    0xFF212121.toInt()
)

/**
 * Compose bottom sheet untuk Shape detail controls (redesigned to match 3D Rotate pattern).
 * Linear layout dengan scrollable left column + fixed right column buttons.
 *
 * Urutan kontrol mengikuti alur natural:
 * Pilih Objek ➔ Fill Color ➔ Geometry (Corner/Star/Arc) ➔ Opacity ➔ Stroke Color ➔
 * Stroke Width ➔ Stroke Detail (Opacity/Style/Join) ➔ Effects Finishing.
 */
@Composable
fun ShapeDetailPage(
    shapeType: ShapeType,
    cornerRadius: Float,
    opacity: Float,
    fillColor: Int,
    strokeWidth: Float,
    strokeOpacity: Float,
    strokeColor: Int,
    strokeJoin: Paint.Join,
    strokeStyle: StrokeStyle,
    arcStartAngle: Float,
    arcSweepAngle: Float,
    onShapeTypeChange: (ShapeType) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onFillColorChange: (Int) -> Unit,
    onOpenFillColorPicker: () -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeOpacityChange: (Float) -> Unit,
    onStrokeColorChange: (Int) -> Unit,
    onOpenStrokeColorPicker: () -> Unit,
    onStrokeJoinChange: (Paint.Join) -> Unit,
    onStrokeStyleChange: (StrokeStyle) -> Unit,
    onArcStartAngleChange: (Float) -> Unit,
    onArcSweepAngleChange: (Float) -> Unit,
    starPoints: Int,
    starInnerRatio: Float,
    onStarPointsChange: (Int) -> Unit,
    onStarInnerRatioChange: (Float) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onOpenEmbossEditor: () -> Unit,
    onOpenGradientEditor: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420  // Same as 3D Rotate
) {
    var shapeTypeState by remember(shapeType) { mutableStateOf(shapeType) }
    var cornerRadiusState by remember(cornerRadius) { mutableStateOf(cornerRadius) }
    var opacityState by remember(opacity) { mutableStateOf(opacity) }
    var fillColorState by remember(fillColor) { mutableStateOf(fillColor) }
    var strokeWidthState by remember(strokeWidth) { mutableStateOf(strokeWidth) }
    var strokeOpacityState by remember(strokeOpacity) { mutableStateOf(strokeOpacity) }
    var strokeColorState by remember(strokeColor) { mutableStateOf(strokeColor) }
    var strokeStyleState by remember(strokeStyle) { mutableStateOf(strokeStyle) }
    var strokeJoinState by remember(strokeJoin) { mutableStateOf(strokeJoin) }
    var arcStartAngleState by remember(arcStartAngle) { mutableStateOf(arcStartAngle) }
    var arcSweepAngleState by remember(arcSweepAngle) { mutableStateOf(arcSweepAngle) }
    var starPointsState by remember(starPoints) { mutableStateOf(starPoints) }
    var starInnerRatioState by remember(starInnerRatio) { mutableStateOf(starInnerRatio) }
    var showShapePicker by remember { mutableStateOf(false) }

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
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                // Drag handle (same as 3D Rotate)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(32.dp)
                        .height(4.dp)
                        .background(DragHandleColor, RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: Scrollable controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // ── 1. Pilih Objek (Shape Preset) ──────────────────────
                        Text(
                            text = "Shape",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .clickable { showShapePicker = true }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                drawShapePickerPreview(type = shapeTypeState, fillColor = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = shapeTypeLabel(shapeTypeState),
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_sort_24px),
                                contentDescription = "Choose shape",
                                modifier = Modifier.size(20.dp),
                                tint = Color(TextSecondary)
                            )
                        }

                        // ── 2. Fill Color ───────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Fill Color",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SWATCH_COLORS.forEach { colorInt ->
                                val isSelected = fillColorState == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            fillColorState = colorInt
                                            onFillColorChange(colorInt)
                                        }
                                )
                            }

                            if (fillColorState !in SWATCH_COLORS) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(fillColorState))
                                        .border(2.5.dp, PrimaryBlue, CircleShape)
                                )
                            }

                            AddColorSwatchButton(onClick = onOpenFillColorPicker, buttonSize = 32.dp)
                        }

                        // ── 3. Geometry & Morfologi (kondisional per tipe) ─────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        if (shapeTypeState == ShapeType.ROUNDED_RECTANGLE || shapeTypeState == ShapeType.RECTANGLE) {
                            ShapeControlRow(
                                label = "Corner Radius",
                                value = cornerRadiusState,
                                onValueChange = { new ->
                                    cornerRadiusState = new
                                    onCornerRadiusChange(new)
                                },
                                valueRange = 0f..100f,
                                suffix = " px"
                            )
                        }

                        if (shapeTypeState == ShapeType.STAR) {
                            ShapeControlRow(
                                label = "Star Points",
                                value = starPointsState.toFloat(),
                                onValueChange = { new ->
                                    val pts = new.toInt().coerceIn(3, 12)
                                    starPointsState = pts
                                    onStarPointsChange(pts)
                                },
                                valueRange = 3f..12f,
                                suffix = ""
                            )

                            ShapeControlRow(
                                label = "Inner Ratio",
                                value = starInnerRatioState,
                                onValueChange = { new ->
                                    starInnerRatioState = new
                                    onStarInnerRatioChange(new)
                                },
                                valueRange = 0.1f..0.9f,
                                suffix = ""
                            )
                        }

                        if (shapeTypeState == ShapeType.ARC) {
                            ShapeControlRow(
                                label = "Arc Start",
                                value = arcStartAngleState,
                                onValueChange = { new ->
                                    arcStartAngleState = new
                                    onArcStartAngleChange(new)
                                },
                                valueRange = -360f..360f,
                                suffix = "°"
                            )

                            ShapeControlRow(
                                label = "Arc Sweep",
                                value = arcSweepAngleState,
                                onValueChange = { new ->
                                    val clamped = if (new == 0f) 1f else new
                                    arcSweepAngleState = clamped
                                    onArcSweepAngleChange(clamped)
                                },
                                valueRange = -360f..360f,
                                suffix = "°"
                            )

                            // Peringatan visual: sweep mendekati 0° membuat shape tak terlihat
                            if (arcSweepAngleState.absoluteValue < 5f) {
                                Text(
                                    text = "⚠ Too small",
                                    color = Color(0xFFFF6B00),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(start = 64.dp)
                                )
                            }
                        }

                        // ── 4. Opacity ──────────────────────────────────────────
                        ShapeControlRow(
                            label = "Opacity",
                            value = opacityState,
                            onValueChange = { new ->
                                opacityState = new
                                onOpacityChange(new)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 5. Stroke ───────────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Stroke Color",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SWATCH_COLORS.forEach { colorInt ->
                                val isSelected = strokeColorState == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            strokeColorState = colorInt
                                            onStrokeColorChange(colorInt)
                                        }
                                )
                            }

                            if (strokeColorState !in SWATCH_COLORS) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(strokeColorState))
                                        .border(2.5.dp, PrimaryBlue, CircleShape)
                                )
                            }

                            AddColorSwatchButton(onClick = onOpenStrokeColorPicker, buttonSize = 32.dp)
                        }

                        ShapeControlRow(
                            label = "Stroke Width",
                            value = strokeWidthState,
                            onValueChange = { new ->
                                strokeWidthState = new
                                onStrokeWidthChange(new)
                            },
                            valueRange = 0f..50f,
                            suffix = " px"
                        )

                        // Smart Disclosure: detail stroke hanya relevan jika ada garis tepi
                        if (strokeWidthState > 0f) {
                            ShapeControlRow(
                                label = "Stroke Opacity",
                                value = strokeOpacityState,
                                onValueChange = { new ->
                                    strokeOpacityState = new
                                    onStrokeOpacityChange(new)
                                },
                                valueRange = 0f..100f,
                                suffix = "%"
                            )

                            Text(
                                text = "Stroke Style",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    StrokeStyle.SOLID to "Solid",
                                    StrokeStyle.DASHED to "Dash",
                                    StrokeStyle.DOTTED to "Dot"
                                ).forEach { (style, label) ->
                                    val isSelected = strokeStyleState == style
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFE8F0FE) else Color(0xFFF8FAFC))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                strokeStyleState = style
                                                onStrokeStyleChange(style)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Stroke Join",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Paint.Join.MITER to "Miter",
                                    Paint.Join.BEVEL to "Bevel",
                                    Paint.Join.ROUND to "Round"
                                ).forEach { (join, label) ->
                                    val isSelected = strokeJoinState == join
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFE8F0FE) else Color(0xFFF8FAFC))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                strokeJoinState = join
                                                onStrokeJoinChange(join)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // ── 6. Effects Finishing (tahap akhir) ─────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Effects",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // Badge status: efek menyelimuti Stroke atau Fill
                        val strokeTarget = strokeWidthState > 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (strokeTarget) Color(0xFFE8F0FE) else Color(0xFFE9F7EC))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (strokeTarget)
                                    "Efek diterapkan pada Garis Tepi (Stroke)"
                                else
                                    "Efek diterapkan pada Bidang Isi (Fill)",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Medium,
                                color = if (strokeTarget) PrimaryBlue else Color(0xFF2E7D32),
                                fontSize = 9.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpenShadowEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF0F7FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Shadow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenEmbossEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF0F7FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Emboss", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpenGradientEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFE8F5FF),
                                    contentColor = Color(0xFF00A8FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Gradient", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF0F7FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Neon", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // RIGHT COLUMN: Buttons (Fixed)
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .padding(start = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sharp_clear_24px),
                                contentDescription = "Cancel",
                                modifier = Modifier.size(18.dp),
                                tint = Color(TextSecondary)
                            )
                        }
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24px),
                                contentDescription = "Apply",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        TextButton(
                            onClick = {
                                shapeTypeState = ShapeType.RECTANGLE
                                cornerRadiusState = 0f
                                opacityState = 100f
                                fillColorState = 0xFF1769FF.toInt()
                                strokeWidthState = 0f
                                strokeOpacityState = 100f
                                strokeColorState = 0xFF000000.toInt()
                                strokeStyleState = StrokeStyle.SOLID
                                arcStartAngleState = 0f
                                arcSweepAngleState = 270f

                                onShapeTypeChange(ShapeType.RECTANGLE)
                                onCornerRadiusChange(0f)
                                onOpacityChange(100f)
                                onFillColorChange(0xFF1769FF.toInt())
                                onStrokeWidthChange(0f)
                                onStrokeOpacityChange(100f)
                                onStrokeColorChange(0xFF000000.toInt())
                                onStrokeStyleChange(StrokeStyle.SOLID)
                                onArcStartAngleChange(0f)
                                onArcSweepAngleChange(270f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.caption, fontSize = 9.sp)
                        }
                    }
                }
            }
        }

        if (showShapePicker) {
            ShapePickerPopupDialog(
                currentType = shapeTypeState,
                onShapeSelected = { type ->
                    shapeTypeState = type
                    onShapeTypeChange(type)
                    showShapePicker = false
                },
                onDismiss = { showShapePicker = false }
            )
        }
    }
}

@Composable
private fun ShapeControlRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            modifier = Modifier.width(60.dp),
            fontSize = 10.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 99,
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        )
        Text(
            text = "${value.toInt()}$suffix",
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp),
            fontSize = 10.sp
        )
    }
}