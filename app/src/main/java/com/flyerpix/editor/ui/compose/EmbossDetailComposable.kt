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

private val EmbossColorScheme = lightColors(
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

/**
 * Compose bottom sheet untuk Text Emboss effect (Light Angle, Intensity, Ambient, Specular, Bevel).
 */
@Composable
fun EmbossDetailPage(
    enabled: Boolean,
    lightAngle: Float,
    intensity: Float,
    ambient: Float,
    specular: Float,
    bevel: Float,
    onEnabledChange: (Boolean) -> Unit,
    onLightAngleChange: (Float) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onAmbientChange: (Float) -> Unit,
    onSpecularChange: (Float) -> Unit,
    onBevelChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var angleState by remember(lightAngle) { mutableStateOf(lightAngle) }
    var intensityState by remember(intensity) { mutableStateOf(intensity) }
    var ambientState by remember(ambient) { mutableStateOf(ambient) }
    var specularState by remember(specular) { mutableStateOf(specular) }
    var bevelState by remember(bevel) { mutableStateOf(bevel) }

    MaterialTheme(colors = EmbossColorScheme) {
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

                    // Main 2-column layout: Left (scrollable controls), Right (action buttons)
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
                                        text = "Enable Emboss",
                                        style = MaterialTheme.typography.subtitle2,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        enabledState = true
                                        angleState = 90f
                                        intensityState = 1.0f
                                        ambientState = 0.5f
                                        specularState = 10f
                                        bevelState = 3.0f
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
                                Divider(color = Color(PanelDivider), thickness = 0.75.dp)

                                // 1. Light Angle Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Light Angle",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "${angleState.toInt()}°",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    Slider(
                                        value = angleState,
                                        onValueChange = { angle ->
                                            angleState = angle
                                            onLightAngleChange(angle)
                                        },
                                        valueRange = 0f..360f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        )
                                    )
                                }

                                // 2. Intensity Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Intensity",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "${(intensityState * 100).toInt()}%",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    Slider(
                                        value = intensityState,
                                        onValueChange = { intens ->
                                            intensityState = intens
                                            onIntensityChange(intens)
                                        },
                                        valueRange = 0f..2.5f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        )
                                    )
                                }

                                // 3. Ambient Light Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ambient Light",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "${(ambientState * 100).toInt()}%",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    Slider(
                                        value = ambientState,
                                        onValueChange = { amb ->
                                            ambientState = amb
                                            onAmbientChange(amb)
                                        },
                                        valueRange = 0f..1f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        )
                                    )
                                }

                                // 4. Specular Hardness Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Specular Hardness",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", specularState),
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    Slider(
                                        value = specularState,
                                        onValueChange = { spec ->
                                            specularState = spec
                                            onSpecularChange(spec)
                                        },
                                        valueRange = 0.1f..20f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        )
                                    )
                                }

                                // 5. Bevel Size Slider
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Bevel",
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", bevelState),
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colors.primary
                                        )
                                    }
                                    Slider(
                                        value = bevelState,
                                        onValueChange = { bev ->
                                            bevelState = bev
                                            onBevelChange(bev)
                                        },
                                        valueRange = 0.5f..12f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        )
                                    )
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
