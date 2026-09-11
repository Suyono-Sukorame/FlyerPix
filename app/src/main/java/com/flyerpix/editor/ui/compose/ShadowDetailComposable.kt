package com.flyerpix.editor.ui.compose

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

private val ShadowColorScheme = lightColors(
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

private val ShadowPresetColors = intArrayOf(
    0xFF000000.toInt(), // Pure Black
    0xFF212121.toInt(), // Charcoal
    0xFF424242.toInt(), // Dark Gray
    0xFF0D47A1.toInt(), // Deep Navy
    0xFF1B5E20.toInt(), // Deep Green
    0xFFB71C1C.toInt(), // Deep Red
    0xFF4A148C.toInt(), // Deep Purple
    0xFFE65100.toInt(), // Deep Orange
    0xFFFFFFFF.toInt(), // Pure White
    0xFF9E9E9E.toInt()  // Light Gray
)

/**
 * Compose bottom sheet untuk Drop Shadow dan Inner Shadow.
 */
@Composable
fun ShadowDetailPage(
    title: String = "Shadow",
    enabled: Boolean,
    color: Int,
    radius: Float,
    opacityPct: Float,
    dx: Float,
    dy: Float,
    onEnabledChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onColorPickRequested: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDxChange: (Float) -> Unit,
    onDyChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var colorState by remember(color) { mutableStateOf(color) }
    var radiusState by remember(radius) { mutableStateOf(radius) }
    var opacityState by remember(opacityPct) { mutableStateOf(opacityPct) }
    var dxState by remember(dx) { mutableStateOf(dx) }
    var dyState by remember(dy) { mutableStateOf(dy) }

    val baseRgb = colorState or 0xFF000000.toInt()

    MaterialTheme(colors = ShadowColorScheme) {
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
                                        text = "Enable $title",
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
                                            colorState = 0xFF000000.toInt()
                                            radiusState = 10f
                                            opacityState = 60f
                                            dxState = 0f
                                            dyState = 0f
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
                                        val isCustom = baseRgb !in ShadowPresetColors
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

                                        ShadowPresetColors.forEachIndexed { index, c ->
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
                                            if (index < ShadowPresetColors.lastIndex) {
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
                                            contentDescription = "Pick custom shadow color",
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }

                                // Blur Radius Slider Row
                                SettingSliderRow(
                                    label = "Blur",
                                    value = radiusState,
                                    range = 0f..40f,
                                    steps = 80,
                                    valueText = "${String.format(java.util.Locale.US, "%.1f", radiusState)}px",
                                    onValueChange = { v ->
                                        radiusState = v
                                        onRadiusChange(v)
                                    }
                                )

                                // Opacity Slider Row
                                SettingSliderRow(
                                    label = "Opacity",
                                    value = opacityState,
                                    range = 0f..100f,
                                    steps = 100,
                                    valueText = "${opacityState.toInt()}%",
                                    onValueChange = { v ->
                                        opacityState = v
                                        onOpacityChange(v)
                                    }
                                )

                                // Offset X Slider Row
                                SettingSliderRow(
                                    label = "Offset X",
                                    value = dxState,
                                    range = -30f..30f,
                                    steps = 60,
                                    valueText = "${dxState.toInt()}px",
                                    onValueChange = { v ->
                                        dxState = v
                                        onDxChange(v)
                                    }
                                )

                                // Offset Y Slider Row
                                SettingSliderRow(
                                    label = "Offset Y",
                                    value = dyState,
                                    range = -30f..30f,
                                    steps = 60,
                                    valueText = "${dyState.toInt()}px",
                                    onValueChange = { v ->
                                        dyState = v
                                        onDyChange(v)
                                    }
                                )
                            } else {
                                Text(
                                    text = "$title is disabled",
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
                                Text("Cancel", style = MaterialTheme.typography.caption)
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
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
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
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
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
