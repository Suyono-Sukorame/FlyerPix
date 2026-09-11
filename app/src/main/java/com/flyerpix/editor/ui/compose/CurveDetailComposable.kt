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

private val CurveColorScheme = lightColors(
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
private fun CurvePresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
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
 * Compose bottom sheet untuk Text Curve / Bending.
 */
@Composable
fun CurveDetailPage(
    curvePercent: Int,
    onCurvePercentChange: (Int) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var curveState by remember(curvePercent) { mutableStateOf(curvePercent) }

    fun formatCurve(v: Int) = when {
        v == 0 -> "0% (Flat)"
        v > 0 -> "+$v% ▲"
        else -> "$v% ▼"
    }

    MaterialTheme(colors = CurveColorScheme) {
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
                                    text = "Curve / Arc Text",
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
                                            curveState = 0
                                            onReset()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            // Curve Slider Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bending",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(52.dp)
                                )
                                Slider(
                                    value = curveState.toFloat(),
                                    onValueChange = { v ->
                                        val intVal = v.toInt()
                                        curveState = intVal
                                        onCurvePercentChange(intVal)
                                    },
                                    valueRange = -100f..100f,
                                    steps = 199,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                Text(
                                    text = formatCurve(curveState),
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(60.dp)
                                )
                            }

                            // Preset Chips: [-100% | -50% | Flat | +50% | +100%]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CurvePresetChip("-100%", curveState == -100) {
                                    curveState = -100
                                    onCurvePercentChange(-100)
                                }
                                CurvePresetChip("-50%", curveState == -50) {
                                    curveState = -50
                                    onCurvePercentChange(-50)
                                }
                                CurvePresetChip("Flat (0)", curveState == 0) {
                                    curveState = 0
                                    onCurvePercentChange(0)
                                }
                                CurvePresetChip("+50%", curveState == 50) {
                                    curveState = 50
                                    onCurvePercentChange(50)
                                }
                                CurvePresetChip("+100%", curveState == 100) {
                                    curveState = 100
                                    onCurvePercentChange(100)
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
