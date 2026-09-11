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

private val PositionColorScheme = lightColors(
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
private fun PositionActionChip(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun NudgeButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(PanelAlt))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary
        )
    }
}

/**
 * Compose bottom sheet untuk Posisi Layer (X/Y Slider, nudge, dan center canvas).
 */
@Composable
fun PositionDetailPage(
    posX: Float,
    posY: Float,
    range: Float = 2000f,
    onPositionChange: (Float, Float) -> Unit,
    onCenterHorizontal: () -> Unit,
    onCenterVertical: () -> Unit,
    onCenterBoth: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var posXState by remember(posX) { mutableStateOf(posX.coerceIn(-range, range)) }
    var posYState by remember(posY) { mutableStateOf(posY.coerceIn(-range, range)) }

    MaterialTheme(colors = PositionColorScheme) {
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
                                    text = "Layer Position",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "Reset (0,0)",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            posXState = 0f
                                            posYState = 0f
                                            onReset()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            // ─── X Position Slider + Nudge ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "X",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(16.dp)
                                )
                                NudgeButton(text = "−10") {
                                    val newX = (posXState - 10f).coerceIn(-range, range)
                                    posXState = newX
                                    onPositionChange(newX, posYState)
                                }
                                Slider(
                                    value = posXState.coerceIn(-range, range),
                                    onValueChange = { v ->
                                        posXState = v
                                        onPositionChange(v, posYState)
                                    },
                                    valueRange = -range..range,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                NudgeButton(text = "+10") {
                                    val newX = (posXState + 10f).coerceIn(-range, range)
                                    posXState = newX
                                    onPositionChange(newX, posYState)
                                }
                                Text(
                                    text = "${posXState.toInt()}px",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(48.dp)
                                )
                            }

                            // ─── Y Position Slider + Nudge ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Y",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(16.dp)
                                )
                                NudgeButton(text = "−10") {
                                    val newY = (posYState - 10f).coerceIn(-range, range)
                                    posYState = newY
                                    onPositionChange(posXState, newY)
                                }
                                Slider(
                                    value = posYState.coerceIn(-range, range),
                                    onValueChange = { v ->
                                        posYState = v
                                        onPositionChange(posXState, v)
                                    },
                                    valueRange = -range..range,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                NudgeButton(text = "+10") {
                                    val newY = (posYState + 10f).coerceIn(-range, range)
                                    posYState = newY
                                    onPositionChange(posXState, newY)
                                }
                                Text(
                                    text = "${posYState.toInt()}px",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(48.dp)
                                )
                            }

                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Canvas alignment buttons
                            Text(
                                text = "Align to Canvas",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PositionActionChip(
                                    label = "Center H",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onCenterHorizontal()
                                }
                                PositionActionChip(
                                    label = "Center V",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onCenterVertical()
                                }
                                PositionActionChip(
                                    label = "Center Both",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onCenterBoth()
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
