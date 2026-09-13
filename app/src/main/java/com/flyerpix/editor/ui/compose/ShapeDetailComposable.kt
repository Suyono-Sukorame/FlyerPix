package com.flyerpix.editor.ui.compose

import android.graphics.Paint
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
    var arcStartAngleState by remember(arcStartAngle) { mutableStateOf(arcStartAngle) }
    var arcSweepAngleState by remember(arcSweepAngle) { mutableStateOf(arcSweepAngle) }

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

                        // SECTION: Shape Preset
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
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                ShapeType.RECTANGLE to "Rectangle",
                                ShapeType.ROUNDED_RECTANGLE to "Rounded",
                                ShapeType.CIRCLE to "Circle",
                                ShapeType.ARC to "Arc",
                                ShapeType.TRIANGLE to "Triangle",
                                ShapeType.STAR to "Star"
                            ).forEach { (type, label) ->
                                val isSelected = shapeTypeState == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE8F0FE) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            shapeTypeState = type
                                            onShapeTypeChange(type)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
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

                        // SECTION: Geometry
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

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
                                    arcSweepAngleState = new
                                    onArcSweepAngleChange(new)
                                },
                                valueRange = -360f..360f,
                                suffix = "°"
                            )
                        }

                        // SECTION: Fill Color
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

                            Button(
                                onClick = onOpenFillColorPicker,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF1F5F9),
                                    contentColor = PrimaryBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                elevation = ButtonDefaults.elevation(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("More", style = MaterialTheme.typography.caption, fontSize = 10.sp)
                            }
                        }

                        // SECTION: Stroke
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        // NEW: Effects Quick Buttons
                        Text(
                            text = "Effects",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { 
                                    // Will be handled by parent activity to show shadow editor
                                },
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
                                onClick = { 
                                    // Will be handled by parent activity to show emboss editor
                                },
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
                                onClick = { 
                                    // Will be handled by parent activity to show gradient editor
                                    // TODO: Open GradientEditorComposable
                                },
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
                                onClick = { 
                                    // Will be handled by parent activity to show neon editor
                                },
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
                        Spacer(modifier = Modifier.height(8.dp))

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

                            Button(
                                onClick = onOpenStrokeColorPicker,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF1F5F9),
                                    contentColor = PrimaryBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                elevation = ButtonDefaults.elevation(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("More", style = MaterialTheme.typography.caption, fontSize = 10.sp)
                            }
                        }

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
                            Text("Cancel", style = MaterialTheme.typography.caption)
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
                            Text(
                                text = "Apply",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(
                            onClick = {
                                shapeTypeState = ShapeType.RECTANGLE
                                cornerRadiusState = 0f
                                opacityState = 100f
                                fillColorState = 0xFFFFFFFF.toInt()
                                strokeWidthState = 0f
                                strokeOpacityState = 100f
                                strokeColorState = 0xFF000000.toInt()
                                strokeStyleState = StrokeStyle.SOLID
                                arcStartAngleState = 0f
                                arcSweepAngleState = 90f

                                onShapeTypeChange(ShapeType.RECTANGLE)
                                onCornerRadiusChange(0f)
                                onOpacityChange(100f)
                                onFillColorChange(0xFFFFFFFF.toInt())
                                onStrokeWidthChange(0f)
                                onStrokeOpacityChange(100f)
                                onStrokeColorChange(0xFF000000.toInt())
                                onStrokeStyleChange(StrokeStyle.SOLID)
                                onArcStartAngleChange(0f)
                                onArcSweepAngleChange(90f)
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
