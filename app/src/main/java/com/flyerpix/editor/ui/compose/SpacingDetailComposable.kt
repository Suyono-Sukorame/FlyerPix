package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import java.util.Locale

private val SpacingColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF4F6FA
private const val PanelHandle = 0xFFD0D4DE

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(PanelTextSecondary)
        )
    }
}

/**
 * Compose bottom sheet untuk Letter & Line Spacing.
 */
@Composable
fun SpacingDetailPage(
    letterSpacing: Float,
    lineSpacing: Float,
    onLetterSpacingChange: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var letterState by remember(letterSpacing) { mutableStateOf(letterSpacing) }
    var lineState by remember(lineSpacing) { mutableStateOf(lineSpacing) }

    MaterialTheme(colors = SpacingColorScheme) {
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

                            // Header row: Title + Reset Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Text Spacing",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            letterState = 0f
                                            lineState = 0f
                                            onReset()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            // ─── 1. Letter Spacing ───
                            SettingSliderRow(
                                label = "Letter",
                                value = letterState,
                                range = -0.2f..1.0f,
                                steps = 60,
                                valueText = String.format(Locale.US, "%.2f", letterState),
                                onValueChange = { v ->
                                    letterState = v
                                    onLetterSpacingChange(v)
                                }
                            )

                            // Letter Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresetChip("Tight", Math.abs(letterState - (-0.10f)) < 0.02f) {
                                    letterState = -0.10f
                                    onLetterSpacingChange(-0.10f)
                                }
                                PresetChip("Normal", Math.abs(letterState - 0f) < 0.02f) {
                                    letterState = 0f
                                    onLetterSpacingChange(0f)
                                }
                                PresetChip("Medium", Math.abs(letterState - 0.15f) < 0.02f) {
                                    letterState = 0.15f
                                    onLetterSpacingChange(0.15f)
                                }
                                PresetChip("Wide", Math.abs(letterState - 0.40f) < 0.02f) {
                                    letterState = 0.40f
                                    onLetterSpacingChange(0.40f)
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // ─── 2. Line Spacing ───
                            SettingSliderRow(
                                label = "Line",
                                value = lineState,
                                range = -20f..80f,
                                steps = 100,
                                valueText = "${lineState.toInt()}px",
                                onValueChange = { v ->
                                    lineState = v
                                    onLineSpacingChange(v)
                                }
                            )

                            // Line Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresetChip("Tight", Math.abs(lineState - (-10f)) < 1f) {
                                    lineState = -10f
                                    onLineSpacingChange(-10f)
                                }
                                PresetChip("Normal", Math.abs(lineState - 0f) < 1f) {
                                    lineState = 0f
                                    onLineSpacingChange(0f)
                                }
                                PresetChip("Medium", Math.abs(lineState - 20f) < 1f) {
                                    lineState = 20f
                                    onLineSpacingChange(20f)
                                }
                                PresetChip("Wide", Math.abs(lineState - 40f) < 1f) {
                                    lineState = 40f
                                    onLineSpacingChange(40f)
                                }
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
