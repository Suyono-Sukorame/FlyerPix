package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

private val BezierColorScheme = lightColors(
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

private val BezierSwatchColors = listOf(
    0xFF1769FF.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFE53935.toInt(),
    0xFF43A047.toInt(),
    0xFFFFB300.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt(),
    0xFF212121.toInt()
)

/**
 * Compose bottom sheet untuk Bézier Curve Studio. Gayanya identik 100% dengan
 * halaman 3D Rotate: card bawah + drag handle + divider, baris kontrol
 * (Label | Slider | Nilai) di kolom kiri, tombol Cancel / Apply di kanan.
 */
@Composable
fun BezierDetailPage(
    strokeWidth: Float,
    strokeColor: Int,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Int) -> Unit,
    onOpenColorPicker: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 400
) {
    var strokeWidthState by remember(strokeWidth) { mutableStateOf(strokeWidth) }
    var strokeColorState by remember(strokeColor) { mutableStateOf(strokeColor) }

    MaterialTheme(colors = BezierColorScheme) {
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
                        // Kolom Kiri: baris kontrol (identik RotateRow di 3D Rotate)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            SettingRow(
                                label = "Stroke Width",
                                value = strokeWidthState,
                                valueRange = 2f..60f,
                                displayValue = "${strokeWidthState.toInt()} px",
                                onValueChange = { new ->
                                    strokeWidthState = new
                                    onStrokeWidthChange(new)
                                }
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Curve Color",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )
                            SwatchRow(
                                colors = BezierSwatchColors,
                                selectedColor = strokeColorState,
                                onColorChange = { color ->
                                    strokeColorState = color
                                    onStrokeColorChange(color)
                                },
                                onOpenColorPicker = onOpenColorPicker
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Edit titik anchor & handle langsung di canvas",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Kolom Kanan: tombol Cancel & Apply (identik 3D Rotate)
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
 * Baris item pengaturan tunggal (Label | Slider | Nilai), identik 100% dengan
 * RotateRow di 3D Rotate.
 */
@Composable
private fun SettingRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: String,
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
            modifier = Modifier.width(76.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

/**
 * Baris deret warna (swatch bulat + tombol "More...").
 */
@Composable
private fun SwatchRow(
    colors: List<Int>,
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
    onOpenColorPicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { colorInt ->
            val isSelected = selectedColor == colorInt
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF1769FF) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
                    .clickable { onColorChange(colorInt) }
            )
        }
        TextButton(
            onClick = onOpenColorPicker,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "More...",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}