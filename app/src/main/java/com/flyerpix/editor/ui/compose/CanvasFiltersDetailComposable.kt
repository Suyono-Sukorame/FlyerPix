package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

private val FiltersColorScheme = lightColors(
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

/**
 * Compose bottom sheet untuk Canvas Filters (Vignette, Noise, Monochrome).
 * Desain kartu preset visual interaktif yang to-the-point dan konsisten dengan 3D Shadow / Rotate.
 */
@Composable
fun CanvasFiltersDetailPage(
    vignetteEnabled: Boolean,
    noiseEnabled: Boolean,
    filterMonochromeEnabled: Boolean,
    onVignetteChange: (Boolean) -> Unit,
    onNoiseChange: (Boolean) -> Unit,
    onFilterMonochromeChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 360
) {
    var vignetteState by remember(vignetteEnabled) { mutableStateOf(vignetteEnabled) }
    var noiseState by remember(noiseEnabled) { mutableStateOf(noiseEnabled) }
    var monochromeState by remember(filterMonochromeEnabled) { mutableStateOf(filterMonochromeEnabled) }

    MaterialTheme(colors = FiltersColorScheme) {
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
                        .padding(top = 8.dp)
                ) {
                    // Drag Handle
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Tap filter to toggle on / off",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )

                            // 3 Horizontal Preset Filter Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterPresetTile(
                                    title = "Vignette",
                                    subtitle = "Dark edges",
                                    active = vignetteState,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val next = !vignetteState
                                        vignetteState = next
                                        onVignetteChange(next)
                                    }
                                )

                                FilterPresetTile(
                                    title = "Noise",
                                    subtitle = "Film grain",
                                    active = noiseState,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val next = !noiseState
                                        noiseState = next
                                        onNoiseChange(next)
                                    }
                                )

                                FilterPresetTile(
                                    title = "B&W",
                                    subtitle = "Monochrome",
                                    active = monochromeState,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val next = !monochromeState
                                        monochromeState = next
                                        onFilterMonochromeChange(next)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Reset Button
                            Text(
                                text = "Reset All Filters",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vignetteState = false
                                        noiseState = false
                                        monochromeState = false
                                        onReset()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Right column: Cancel & Apply
                        Column(
                            modifier = Modifier
                                .width(64.dp)
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
                            Spacer(modifier = Modifier.height(2.dp))
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
private fun FilterPresetTile(
    title: String,
    subtitle: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (active) MaterialTheme.colors.primary else Color(PanelDivider)
    val bgColor = if (active) MaterialTheme.colors.primary.copy(alpha = 0.08f) else Color(PanelAlt)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(if (active) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color = if (active) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.overline,
            color = Color(PanelTextSecondary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (active) MaterialTheme.colors.primary else Color(0xFFD0D4DE))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (active) "ON" else "OFF",
                style = MaterialTheme.typography.overline,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
