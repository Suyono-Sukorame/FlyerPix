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

private val BackgroundColorScheme = lightColors(
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

private val BgPresetColors = intArrayOf(
    0xFF000000.toInt(), // Black
    0xFFFFFFFF.toInt(), // White
    0xFF1769FF.toInt(), // Brand Blue
    0xFFE53935.toInt(), // Red
    0xFFFFB300.toInt(), // Amber
    0xFF43A047.toInt(), // Green
    0xFFFB8C00.toInt(), // Orange
    0xFF8E24AA.toInt(), // Purple
    0xFF00ACC1.toInt(), // Teal
    0xFF616161.toInt()  // Gray
)

/**
 * Compose bottom sheet untuk Text Background.
 */
@Composable
fun BackgroundDetailPage(
    enabled: Boolean,
    color: Int,
    opacityPct: Float,
    padding: Float,
    cornerRadius: Float,
    onEnabledChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onColorPickRequested: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onPaddingChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var colorState by remember(color) { mutableStateOf(color) }
    var opacityState by remember(opacityPct) { mutableStateOf(opacityPct) }
    var paddingState by remember(padding) { mutableStateOf(padding) }
    var cornerState by remember(cornerRadius) { mutableStateOf(cornerRadius) }

    val baseRgb = colorState or 0xFF000000.toInt()

    MaterialTheme(colors = BackgroundColorScheme) {
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
                                        text = "Enable Background",
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
                                            opacityState = 100f
                                            paddingState = 10f
                                            cornerState = 0f
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
                                        val isCustom = baseRgb !in BgPresetColors
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

                                        BgPresetColors.forEachIndexed { index, c ->
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
                                            if (index < BgPresetColors.lastIndex) {
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
                                            contentDescription = "Pick custom background color",
                                            tint = MaterialTheme.colors.primary
                                        )
                                    }
                                }

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

                                // Padding Slider Row
                                SettingSliderRow(
                                    label = "Padding",
                                    value = paddingState,
                                    range = 0f..100f,
                                    steps = 100,
                                    valueText = "${paddingState.toInt()}px",
                                    onValueChange = { v ->
                                        paddingState = v
                                        onPaddingChange(v)
                                    }
                                )

                                // Corner Radius Slider Row
                                SettingSliderRow(
                                    label = "Radius",
                                    value = cornerState,
                                    range = 0f..120f,
                                    steps = 120,
                                    valueText = "${cornerState.toInt()}px",
                                    onValueChange = { v ->
                                        cornerState = v
                                        onCornerRadiusChange(v)
                                    }
                                )
                            } else {
                                Text(
                                    text = "Text background is disabled",
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
