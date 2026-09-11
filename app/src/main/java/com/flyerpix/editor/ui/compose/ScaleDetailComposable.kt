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

private val ScaleColorScheme = lightColors(
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
private fun ScalePresetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
 * Compose bottom sheet untuk Skala Objek (Shape/Image/Sticker).
 */
@Composable
fun ScaleDetailPage(
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onFitCanvas: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 360
) {
    var scaleState by remember(scale) { mutableStateOf(scale.coerceIn(0.1f, 8.0f)) }

    MaterialTheme(colors = ScaleColorScheme) {
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Header row: Title + Reset Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Object Scale",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                Text(
                                    text = "Reset (100%)",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            scaleState = 1f
                                            onReset()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }

                            // ─── Scale Slider ───
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Scale",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(44.dp)
                                )
                                Slider(
                                    value = scaleState.coerceIn(0.1f, 8.0f),
                                    onValueChange = { v ->
                                        scaleState = v
                                        onScaleChange(v)
                                    },
                                    valueRange = 0.1f..8.0f,
                                    steps = 79,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                Text(
                                    text = "${(scaleState * 100).toInt()}%",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(48.dp)
                                )
                            }

                            // Quick preset chips & Fit Canvas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { sc ->
                                    ScalePresetChip(
                                        label = "${(sc * 100).toInt()}%",
                                        selected = Math.abs(scaleState - sc) < 0.05f,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        scaleState = sc
                                        onScaleChange(sc)
                                    }
                                }
                                ScalePresetChip(
                                    label = "Fit",
                                    selected = false,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onFitCanvas()
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
