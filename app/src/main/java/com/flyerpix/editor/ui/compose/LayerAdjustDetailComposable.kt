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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val LayerAdjustColorScheme = lightColors(
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
private const val PanelChipBg = 0xFFF1F4FA

/** Nama preset color match (Prompt 04). */
val LAYER_PRESET_NAMES = listOf("Normal", "Warm", "Cool", "Faded", "Punchy", "B&W")

fun layerAdjustPresetParams(preset: String): LayerAdjustPresetParams = when (preset) {
    "Warm"   -> LayerAdjustPresetParams(temperature = 35f)
    "Cool"   -> LayerAdjustPresetParams(temperature = -35f)
    "Faded"  -> LayerAdjustPresetParams(vibrance = -45f, contrast = -25f)
    "Punchy" -> LayerAdjustPresetParams(vibrance = 45f, contrast = 25f)
    "B&W"    -> LayerAdjustPresetParams(saturation = -95f)
    else     -> LayerAdjustPresetParams()
}

data class LayerAdjustPresetParams(
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val gamma: Float = 0f,
    val vibrance: Float = 0f,
    val hue: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f
)

/**
 * Compose bottom sheet untuk Adjustment per-layer (Prompt 04).
 * Switch aktif + 10 slider (skala -100..100, gamma netral 0) + tombol Reset
 * + deretan preset color match (Normal/Warm/Cool/Faded/Punchy/B&W).
 * Style identik dengan CanvasAdjustDetailPage / himpunan 3D Rotate.
 */
@Composable
fun LayerAdjustDetailPage(
    enabled: Boolean,
    contrast: Float,
    saturation: Float,
    exposure: Float,
    highlights: Float,
    shadows: Float,
    temperature: Float,
    tint: Float,
    gamma: Float,
    vibrance: Float,
    hue: Float,
    activePreset: String?,
    onEnabledChange: (Boolean) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onExposureChange: (Float) -> Unit,
    onHighlightsChange: (Float) -> Unit,
    onShadowsChange: (Float) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onTintChange: (Float) -> Unit,
    onGammaChange: (Float) -> Unit,
    onVibranceChange: (Float) -> Unit,
    onHueChange: (Float) -> Unit,
    onPreset: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var contrastState by remember(contrast) { mutableStateOf(contrast) }
    var saturationState by remember(saturation) { mutableStateOf(saturation) }
    var exposureState by remember(exposure) { mutableStateOf(exposure) }
    var highlightsState by remember(highlights) { mutableStateOf(highlights) }
    var shadowsState by remember(shadows) { mutableStateOf(shadows) }
    var temperatureState by remember(temperature) { mutableStateOf(temperature) }
    var tintState by remember(tint) { mutableStateOf(tint) }
    var gammaState by remember(gamma) { mutableStateOf(gamma) }
    var vibranceState by remember(vibrance) { mutableStateOf(vibrance) }
    var hueState by remember(hue) { mutableStateOf(hue) }
    var presetState by remember(activePreset) { mutableStateOf(activePreset) }

    fun setActive() {
        if (!enabledState) {
            enabledState = true
            onEnabledChange(true)
        }
    }

    fun applyPreset(p: String) {
        val params = layerAdjustPresetParams(p)
        contrastState = params.contrast
        saturationState = params.saturation
        exposureState = params.exposure
        highlightsState = params.highlights
        shadowsState = params.shadows
        temperatureState = params.temperature
        tintState = params.tint
        gammaState = params.gamma
        vibranceState = params.vibrance
        hueState = params.hue
        presetState = if (p == "Normal") null else p
        setActive()
        onContrastChange(params.contrast)
        onSaturationChange(params.saturation)
        onExposureChange(params.exposure)
        onHighlightsChange(params.highlights)
        onShadowsChange(params.shadows)
        onTemperatureChange(params.temperature)
        onTintChange(params.tint)
        onGammaChange(params.gamma)
        onVibranceChange(params.vibrance)
        onHueChange(params.hue)
        onPreset(p)
    }

    MaterialTheme(colors = LayerAdjustColorScheme) {
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
                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = enabledState,
                                    onCheckedChange = { c ->
                                        enabledState = c
                                        onEnabledChange(c)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colors.primary,
                                        checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Text(
                                text = "Preset",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (p in LAYER_PRESET_NAMES) {
                                    val selected = presetState == p
                                    Text(
                                        text = p,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color(PanelTextSecondary),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) MaterialTheme.colors.primary else Color(PanelChipBg),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { applyPreset(p) }
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }

                            LayerAdjustRow("Contrast", contrastState) { v -> contrastState = v; setActive(); onContrastChange(v) }
                            LayerAdjustRow("Saturation", saturationState) { v -> saturationState = v; setActive(); onSaturationChange(v) }
                            LayerAdjustRow("Exposure", exposureState) { v -> exposureState = v; setActive(); onExposureChange(v) }
                            LayerAdjustRow("Highlights", highlightsState) { v -> highlightsState = v; setActive(); onHighlightsChange(v) }
                            LayerAdjustRow("Shadows", shadowsState) { v -> shadowsState = v; setActive(); onShadowsChange(v) }
                            LayerAdjustRow("Temperature", temperatureState) { v -> temperatureState = v; setActive(); onTemperatureChange(v) }
                            LayerAdjustRow("Tint", tintState) { v -> tintState = v; setActive(); onTintChange(v) }
                            LayerAdjustRow("Gamma", gammaState) { v -> gammaState = v; setActive(); onGammaChange(v) }
                            LayerAdjustRow("Vibrance", vibranceState) { v -> vibranceState = v; setActive(); onVibranceChange(v) }
                            LayerAdjustRow("Hue", hueState) { v -> hueState = v; setActive(); onHueChange(v) }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        contrastState = 0f; saturationState = 0f
                                        exposureState = 0f; highlightsState = 0f; shadowsState = 0f
                                        temperatureState = 0f; tintState = 0f; gammaState = 0f
                                        vibranceState = 0f; hueState = 0f
                                        presetState = null
                                        onContrastChange(0f); onSaturationChange(0f)
                                        onExposureChange(0f); onHighlightsChange(0f); onShadowsChange(0f)
                                        onTemperatureChange(0f); onTintChange(0f); onGammaChange(0f)
                                        onVibranceChange(0f); onHueChange(0f)
                                        onReset()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

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
private fun LayerAdjustRow(
    label: String,
    value: Float,
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
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            steps = 200,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = formatLayerAdjustValue(value),
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}

private fun formatLayerAdjustValue(v: Float): String {
    val intVal = v.toInt()
    return if (intVal > 0) "+$intVal" else "$intVal"
}