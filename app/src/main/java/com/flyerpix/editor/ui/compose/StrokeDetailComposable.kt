package com.flyerpix.editor.ui.compose

import androidx.compose.ui.res.painterResource

import com.flyerpix.editor.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private val StrokeColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

private val StrokePresetColors = intArrayOf(
    0xFF000000.toInt(), // Black
    0xFFFFFFFF.toInt(), // White
    0xFF1769FF.toInt(), // Blue
    0xFFFF0055.toInt(), // Red/Pink
    0xFFFFCC00.toInt(), // Yellow
    0xFF00CC66.toInt(), // Green
    0xFFFF6600.toInt(), // Orange
    0xFF9933FF.toInt(), // Purple
    0xFF00FFFF.toInt(), // Cyan
    0xFF888888.toInt()  // Gray
)

@Composable
fun StrokeDetailPage(
    enabled: Boolean,
    width: Float,
    opacityPct: Float,
    color: Int,
    strokeStyle: String = "Solid",
    strokeJoin: String = "Miter",
    onEnabledChange: (Boolean) -> Unit,
    onWidthChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onColorPickRequested: () -> Unit,
    onStrokeStyleChange: (String) -> Unit = {},
    onStrokeJoinChange: (String) -> Unit = {},
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var widthState by remember(width) { mutableStateOf(width) }
    var opacityState by remember(opacityPct) { mutableStateOf(opacityPct) }
    var colorState by remember(color) { mutableStateOf(color) }
    var strokeStyleState by remember(strokeStyle) { mutableStateOf(strokeStyle) }
    var strokeJoinState by remember(strokeJoin) { mutableStateOf(strokeJoin) }

    val baseRgb = colorState or 0xFF000000.toInt()

    MaterialTheme(colors = StrokeColorScheme) {
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
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Header row: Switch Enable + Reset Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = enabledState,
                                        onCheckedChange = { isChecked ->
                                            enabledState = isChecked
                                            onEnabledChange(isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colors.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Enable Stroke",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }

                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            enabledState = true
                                            widthState = 4f
                                            opacityState = 100f
                                            colorState = 0xFF000000.toInt()
                                            strokeStyleState = "Solid"
                                            strokeJoinState = "Miter"
                                            onReset()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            if (enabledState) {
                                // Color swatches row + picker '+'
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Color",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        modifier = Modifier.width(52.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isCustom = baseRgb !in StrokePresetColors
                                        if (isCustom) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(baseRgb))
                                                    .border(2.dp, MaterialTheme.colors.primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        StrokePresetColors.forEachIndexed { index, c ->
                                            val isSelected = baseRgb == c
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(c))
                                                    .clickable {
                                                        colorState = c
                                                        onColorChange(c)
                                                    }
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colors.primary else Color(0x33000000),
                                                        shape = CircleShape
                                                    )
                                            )
                                            if (index < StrokePresetColors.lastIndex) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = onColorPickRequested,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Pick custom stroke color",
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }

                                // Width Slider
                                SettingSliderRow(
                                    label = "Width",
                                    value = widthState,
                                    range = 0f..60f,
                                    valueText = "${String.format(java.util.Locale.US, "%.1f", widthState)}px",
                                    onValueChange = { v ->
                                        widthState = v
                                        onWidthChange(v)
                                    }
                                )

                                // Opacity Slider
                                SettingSliderRow(
                                    label = "Opacity",
                                    value = opacityState,
                                    range = 0f..100f,
                                    valueText = "${opacityState.toInt()}%",
                                    onValueChange = { v ->
                                        opacityState = v
                                        onOpacityChange(v)
                                    }
                                )

                                // Stroke Style Section
                                Divider(color = Color(PanelDivider), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Style",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Solid", "Dashed", "Dotted").forEach { style ->
                                        val isSelected = strokeStyleState == style
                                        Button(
                                            onClick = { 
                                                strokeStyleState = style
                                                onStrokeStyleChange(style)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = if (isSelected) MaterialTheme.colors.primary else Color(0xFFF1F4FA),
                                                contentColor = if (isSelected) Color.White else Color(PanelTextSecondary)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(style, fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.caption.fontSize)
                                        }
                                    }
                                }

                                // Stroke Join Section
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Text(
                                    text = "Join",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Miter", "Bevel", "Round").forEach { join ->
                                        val isSelected = strokeJoinState == join
                                        Button(
                                            onClick = { 
                                                strokeJoinState = join
                                                onStrokeJoinChange(join)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(32.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = if (isSelected) MaterialTheme.colors.primary else Color(0xFFF1F4FA),
                                                contentColor = if (isSelected) Color.White else Color(PanelTextSecondary)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(join, fontWeight = FontWeight.SemiBold, fontSize = MaterialTheme.typography.caption.fontSize)
                                        }
                                    }
                                }

                                // Live Preview
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(PanelDivider), thickness = 1.dp)
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(PanelDivider), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawLine(
                                            color = Color(colorState),
                                            start = androidx.compose.ui.geometry.Offset(20f, size.height / 2),
                                            end = androidx.compose.ui.geometry.Offset(size.width - 20f, size.height / 2),
                                            strokeWidth = widthState.coerceAtLeast(1f)
                                        )
                                    }
                                    Text(
                                        "Preview: ${String.format("%.1f", widthState)}px • $strokeStyleState",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Stroke outline is disabled",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }

                        // Right column: Cancel & Apply
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
                                    tint = Color(PanelTextSecondary)
                                )
                            }
                            Button(
                                onClick = onApply,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    var localValue by remember { mutableStateOf(value) }
    var debounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(value) {
        localValue = value
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = localValue.coerceIn(range.start, range.endInclusive),
            onValueChange = { newValue ->
                localValue = newValue
                debounceJob?.cancel()
                debounceJob = coroutineScope.launch {
                    delay(300)
                    onValueChange(newValue)
                }
            },
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp)
        )
    }
}

