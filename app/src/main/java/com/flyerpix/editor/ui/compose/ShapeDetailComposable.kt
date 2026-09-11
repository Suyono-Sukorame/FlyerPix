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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.ShapeType
import com.flyerpix.editor.canvas.model.StrokeStyle

private val PrimaryBlue = Color(0xFF1769FF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val SurfaceBg = Color(0xFFFFFFFF)
private val DragHandleColor = Color(0xFFCBD5E1)
private val TabSelectedBg = Color(0xFFE8F0FE)
private val TabUnselectedBg = Color(0xFFF1F5F9)

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

private enum class ShapeTab {
    TYPE_RADIUS,
    FILL,
    STROKE
}

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
    maxHeightPx: Int = 460
) {
    var activeTab by remember { mutableStateOf(ShapeTab.TYPE_RADIUS) }

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
            backgroundColor = SurfaceBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(DragHandleColor, CircleShape)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column (Controls)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                    ) {
                        // Title & Sub-Tabs Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_nav_shapes_24px),
                                    contentDescription = "Shapes",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shape Studio",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Sub-Tab Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tabs = listOf(
                                ShapeTab.TYPE_RADIUS to "Shape & Radius",
                                ShapeTab.FILL to "Fill Color",
                                ShapeTab.STROKE to "Stroke / Outline"
                            )
                            tabs.forEach { (tab, label) ->
                                val isSelected = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) TabSelectedBg else TabUnselectedBg)
                                        .clickable { activeTab = tab }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Divider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Scrollable content per tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (activeTab) {
                                ShapeTab.TYPE_RADIUS -> {
                                    // Shape Type Selector
                                    Text(
                                        text = "Shape Preset",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val shapeList = listOf(
                                            ShapeType.RECTANGLE to "Rectangle",
                                            ShapeType.ROUNDED_RECTANGLE to "Rounded",
                                            ShapeType.CIRCLE to "Ellipse",
                                            ShapeType.ARC to "Arc",
                                            ShapeType.TRIANGLE to "Triangle",
                                            ShapeType.STAR to "Star"
                                        )
                                        shapeList.forEach { (type, label) ->
                                            val isSelected = shapeType == type
                                            Card(
                                                shape = RoundedCornerShape(10.dp),
                                                backgroundColor = if (isSelected) TabSelectedBg else Color(0xFFF8FAFC),
                                                elevation = if (isSelected) 2.dp else 0.dp,
                                                modifier = Modifier
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable { onShapeTypeChange(type) }
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) PrimaryBlue else TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Corner Radius Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Corner Radius",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${cornerRadius.toInt()} px",
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Slider(
                                            value = cornerRadius,
                                            onValueChange = onCornerRadiusChange,
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PrimaryBlue,
                                                activeTrackColor = PrimaryBlue
                                            )
                                        )
                                    }

                                    // Opacity Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Opacity",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${opacity.toInt()}%",
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Slider(
                                            value = opacity,
                                            onValueChange = onOpacityChange,
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PrimaryBlue,
                                                activeTrackColor = PrimaryBlue
                                            )
                                        )
                                    }

                                    if (shapeType == ShapeType.ARC) {
                                        listOf(
                                            Triple("Arc Start Angle", arcStartAngle, onArcStartAngleChange),
                                            Triple("Arc Sweep Angle", arcSweepAngle, onArcSweepAngleChange)
                                        ).forEach { (label, value, onChange) ->
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(label, color = TextSecondary, fontSize = 11.sp)
                                                    Text("${value.toInt()}°", color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Slider(
                                                    value = value,
                                                    onValueChange = onChange,
                                                    valueRange = if (label.startsWith("Arc Start")) -360f..360f else -360f..360f,
                                                    colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                                                )
                                            }
                                        }
                                    }
                                }

                                ShapeTab.FILL -> {
                                    Text(
                                        text = "Fill Color Palette",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    // Color Swatches Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SWATCH_COLORS.forEach { colorInt ->
                                            val isSelected = fillColor == colorInt
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorInt))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onFillColorChange(colorInt) }
                                            )
                                        }

                                        // Custom Color Picker Button
                                        Button(
                                            onClick = onOpenFillColorPicker,
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFFF1F5F9),
                                                contentColor = PrimaryBlue
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            elevation = ButtonDefaults.elevation(0.dp)
                                        ) {
                                            Text("More...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Current Fill Preview", fontSize = 11.sp, color = TextPrimary)
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(fillColor))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                                .clickable { onOpenFillColorPicker() }
                                        )
                                    }
                                }

                                ShapeTab.STROKE -> {
                                    // Stroke Width Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Stroke Width",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${strokeWidth.toInt()} px",
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Slider(
                                            value = strokeWidth,
                                            onValueChange = onStrokeWidthChange,
                                            valueRange = 0f..50f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PrimaryBlue,
                                                activeTrackColor = PrimaryBlue
                                            )
                                        )
                                    }

                                    // Stroke Opacity Slider
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Stroke Opacity",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "${strokeOpacity.toInt()}%",
                                                color = PrimaryBlue,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Slider(
                                            value = strokeOpacity,
                                            onValueChange = onStrokeOpacityChange,
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = PrimaryBlue,
                                                activeTrackColor = PrimaryBlue
                                            )
                                        )
                                    }

                                    // Stroke Color Palette
                                    Text(
                                        text = "Stroke Color",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SWATCH_COLORS.forEach { colorInt ->
                                            val isSelected = strokeColor == colorInt
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorInt))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onStrokeColorChange(colorInt) }
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
                                            elevation = ButtonDefaults.elevation(0.dp)
                                        ) {
                                            Text("More...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Stroke Join Style
                                    Text(
                                        text = "Stroke Style",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            StrokeStyle.SOLID to "Solid",
                                            StrokeStyle.DASHED to "Dashed",
                                            StrokeStyle.DOTTED to "Dotted"
                                        ).forEach { (style, label) ->
                                            val isSelected = strokeStyle == style
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) TabSelectedBg else Color(0xFFF8FAFC))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onStrokeStyleChange(style) }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) PrimaryBlue else TextPrimary)
                                            }
                                        }
                                    }

                                    // Stroke Join Style
                                    Text(
                                        text = "Corner / Join Style",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val joins = listOf(
                                            Paint.Join.MITER to "Miter",
                                            Paint.Join.BEVEL to "Bevel",
                                            Paint.Join.ROUND to "Round"
                                        )
                                        joins.forEach { (join, label) ->
                                            val isSelected = strokeJoin == join
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) TabSelectedBg else Color(0xFFF8FAFC))
                                                    .border(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onStrokeJoinChange(join) }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PrimaryBlue else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column (Cancel & Apply)
                    Column(
                        modifier = Modifier
                            .width(62.dp)
                            .fillMaxHeight()
                            .padding(start = 4.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Apply",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
