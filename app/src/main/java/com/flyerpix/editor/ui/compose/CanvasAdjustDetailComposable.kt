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

private val AdjustColorScheme = lightColors(
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
 * Compose bottom sheet untuk Canvas Adjustments (Brightness, Contrast, Saturation).
 * Gayanya identik 100% dengan halaman 3D Rotate:
 * Card bawah, drag handle, divider, daftar baris slider (Label | Slider | Nilai) + tombol Reset
 * di kolom kiri (scrollable), dan tombol Cancel / Apply di kolom kanan.
 */
@Composable
fun CanvasAdjustDetailPage(
    brightness: Float, // -100f..100f
    contrast: Float,   // -100f..100f
    saturation: Float, // -100f..100f
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var brightnessState by remember(brightness) { mutableStateOf(brightness) }
    var contrastState by remember(contrast) { mutableStateOf(contrast) }
    var saturationState by remember(saturation) { mutableStateOf(saturation) }

    MaterialTheme(colors = AdjustColorScheme) {
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
                        // Kolom Kiri: Baris kontrol slider (Identik RotateRow di 3D Rotate)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            AdjustRow(
                                label = "Brightness",
                                value = brightnessState,
                                onValueChange = { new ->
                                    brightnessState = new
                                    onBrightnessChange(new)
                                }
                            )

                            AdjustRow(
                                label = "Contrast",
                                value = contrastState,
                                onValueChange = { new ->
                                    contrastState = new
                                    onContrastChange(new)
                                }
                            )

                            AdjustRow(
                                label = "Saturation",
                                value = saturationState,
                                onValueChange = { new ->
                                    saturationState = new
                                    onSaturationChange(new)
                                }
                            )

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
                                        brightnessState = 0f
                                        contrastState = 0f
                                        saturationState = 0f
                                        onBrightnessChange(0f)
                                        onContrastChange(0f)
                                        onSaturationChange(0f)
                                        onReset()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Kolom Kanan: Tombol Cancel & Apply (Identik 3D Rotate)
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

/**
 * Baris item pengaturan tunggal (Label | Slider | Nilai), identik 100% dengan RotateRow di 3D Rotate.
 */
@Composable
private fun AdjustRow(
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
            text = formatAdjustValue(value),
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}

private fun formatAdjustValue(v: Float): String {
    val intVal = v.toInt()
    return if (intVal > 0) "+$intVal" else "$intVal"
}
