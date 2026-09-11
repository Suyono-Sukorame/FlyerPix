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
import com.flyerpix.editor.canvas.model.PerspectivePreset

private val PerspectiveColorScheme = lightColors(
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
private fun PerspectiveChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(PanelTextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Compose bottom sheet untuk Text Perspective distortion.
 */
@Composable
fun PerspectiveDetailPage(
    enabled: Boolean,
    activePreset: PerspectivePreset?,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelect: (PerspectivePreset) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var presetState by remember(activePreset) { mutableStateOf(activePreset) }

    val presets = listOf(
        "Flat" to PerspectivePreset.FLAT,
        "Slight Left" to PerspectivePreset.SLIGHT_LEFT,
        "Left Wall" to PerspectivePreset.LEFT_WALL,
        "Right Wall" to PerspectivePreset.RIGHT_WALL,
        "Gentle Tilt" to PerspectivePreset.GENTLE_TILT,
        "Billboard" to PerspectivePreset.TOP_BILLBOARD,
        "Floor Tilt" to PerspectivePreset.FLOOR_TILT
    )

    MaterialTheme(colors = PerspectiveColorScheme) {
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
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(PanelHandle))
                        )
                    }

                    // Main 2-column layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Scrollable controls
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row: Switch + Reset Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = enabledState,
                                        onCheckedChange = { isChecked ->
                                            enabledState = isChecked
                                            onEnabledChange(isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colors.primary,
                                            checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Enable Perspective",
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        presetState = PerspectivePreset.FLAT
                                        onReset()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                            }

                            if (enabledState) {
                                // Guide info card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    backgroundColor = Color(PanelAlt),
                                    elevation = 0.dp
                                ) {
                                    Text(
                                        text = "Drag the 4 corner pins directly on the canvas to distort perspective freely, or tap a preset below:",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Divider(color = Color(PanelDivider), thickness = 0.75.dp)

                                Text(
                                    text = "Perspective Presets",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(PanelTextSecondary)
                                )

                                // Presets Row 1
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 0..3) {
                                        val (label, preset) = presets[i]
                                        val isSelected = presetState == preset
                                        Box(modifier = Modifier.weight(1f)) {
                                            PerspectiveChip(
                                                label = label,
                                                selected = isSelected,
                                                onClick = {
                                                    presetState = preset
                                                    onPresetSelect(preset)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Presets Row 2
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 4..6) {
                                        val (label, preset) = presets[i]
                                        val isSelected = presetState == preset
                                        Box(modifier = Modifier.weight(1f)) {
                                            PerspectiveChip(
                                                label = label,
                                                selected = isSelected,
                                                onClick = {
                                                    presetState = preset
                                                    onPresetSelect(preset)
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // Right Column: Vertical Action Buttons (Cancel & Apply)
                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(PanelTextSecondary)
                                )
                            }

                            Button(
                                onClick = onApply,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                            ) {
                                Text(
                                    text = "✓",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
