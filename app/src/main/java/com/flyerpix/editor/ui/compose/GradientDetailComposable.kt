package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.canvas.model.GradientType

private val GradientDetailColorScheme = lightColors(
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

/** Nama preset gradient (pasangan warna 2 stop). */
val GRADIENT_PRESET_NAMES = listOf("Sunset", "Ocean", "Lime", "Grape", "Candy")

fun gradientPresetColors(name: String): IntArray = when (name) {
    "Sunset" -> intArrayOf(0xFFFF512F.toInt(), 0xFFDD2476.toInt())
    "Ocean"  -> intArrayOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt())
    "Lime"   -> intArrayOf(0xFF11998E.toInt(), 0xFF38EF7D.toInt())
    "Grape"  -> intArrayOf(0xFF8E2DE2.toInt(), 0xFF4A00E0.toInt())
    "Candy"  -> intArrayOf(0xFFFF6E7F.toInt(), 0xFFBFE9FF.toInt())
    else     -> intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
}

/** Palet swatch inline untuk memilih warna stop gradasi. */
private val GRADIENT_PALETTE = listOf(
    0xFFFFFFFF.toInt(), 0xFF000000.toInt(),
    0xFFFF5722.toInt(), 0xFFFFC107.toInt(),
    0xFF4CAF50.toInt(), 0xFF2196F3.toInt(),
    0xFF9C27B0.toInt(), 0xFFE91E63.toInt()
)

/**
 * Compose bottom sheet untuk Gradient Fill per-layer (Edit → Gradient).
 * Style identik dengan LayerAdjustDetailPage: switch Aktif + chip tipe,
 * preview, pemilih warna, preset + slider sudut + tombol Reset / Cancel / Apply.
 */
@Composable
fun GradientDetailPage(
    enabled: Boolean,
    color1: Int,
    color2: Int,
    type: GradientType,
    angle: Float,
    activePreset: String?,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (GradientType) -> Unit,
    onColor1Change: (Int) -> Unit,
    onColor2Change: (Int) -> Unit,
    onAngleChange: (Float) -> Unit,
    onPreset: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var color1State by remember(color1) { mutableStateOf(color1) }
    var color2State by remember(color2) { mutableStateOf(color2) }
    var typeState by remember(type) { mutableStateOf(type) }
    var angleState by remember(angle) { mutableStateOf(angle) }
    var presetState by remember(activePreset) { mutableStateOf(activePreset) }

    fun setActive() {
        if (!enabledState) {
            enabledState = true
            onEnabledChange(true)
        }
    }

    fun applyPreset(p: String) {
        val colors = gradientPresetColors(p)
        color1State = colors[0]
        color2State = if (colors.size > 1) colors[1] else colors[0]
        presetState = p
        setActive()
        onPreset(p)
    }

    MaterialTheme(colors = GradientDetailColorScheme) {
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
                                text = "Type",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (t in listOf(GradientType.LINEAR, GradientType.RADIAL, GradientType.SWEEP)) {
                                    val selected = typeState == t
                                    Text(
                                        text = t.name.capitalize(),
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
                                            .clickable {
                                                typeState = t
                                                presetState = null
                                                setActive()
                                                onTypeChange(t)
                                            }
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .background(
                                        brush = when (typeState) {
                                            GradientType.LINEAR -> Brush.linearGradient(
                                                listOf(Color(color1State), Color(color2State))
                                            )
                                            GradientType.RADIAL -> Brush.radialGradient(
                                                listOf(Color(color1State), Color(color2State))
                                            )
                                            GradientType.SWEEP -> Brush.sweepGradient(
                                                listOf(Color(color1State), Color(color2State))
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(PanelDivider),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )

                            Text(
                                text = "Color 1",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )
                            GradientSwatchRow(
                                colors = GRADIENT_PALETTE,
                                selected = color1State,
                                onSelect = { c ->
                                    color1State = c
                                    presetState = null
                                    setActive()
                                    onColor1Change(c)
                                }
                            )

                            Text(
                                text = "Color 2",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )
                            GradientSwatchRow(
                                colors = GRADIENT_PALETTE,
                                selected = color2State,
                                onSelect = { c ->
                                    color2State = c
                                    presetState = null
                                    setActive()
                                    onColor2Change(c)
                                }
                            )

                            Text(
                                text = "Preset",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (p in GRADIENT_PRESET_NAMES) {
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

                            if (typeState == GradientType.LINEAR) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Angle",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        modifier = Modifier.width(68.dp)
                                    )
                                    Slider(
                                        value = angleState,
                                        onValueChange = { v ->
                                            angleState = v
                                            presetState = null
                                            setActive()
                                            onAngleChange(v)
                                        },
                                        valueRange = 0f..360f,
                                        steps = 359,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                    )
                                    Text(
                                        text = "${angleState.toInt()}°",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                            }

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
                                        enabledState = false
                                        color1State = 0xFFFFFFFF.toInt()
                                        color2State = 0xFF000000.toInt()
                                        typeState = GradientType.LINEAR
                                        angleState = 0f
                                        presetState = null
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
private fun GradientSwatchRow(
    colors: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { c ->
            val isSelected = selected == c
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(26.dp)
                    .background(Color(c), CircleShape)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colors.primary,
                                shape = CircleShape
                            )
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                color = Color(PanelDivider),
                                shape = CircleShape
                            )
                        }
                    )
                    .clickable { onSelect(c) }
            )
        }
    }
}