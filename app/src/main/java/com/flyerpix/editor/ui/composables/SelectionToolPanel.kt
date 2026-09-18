package com.flyerpix.editor.ui.composables

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.PixelCanvasView

private val SelectColorScheme = lightColors(
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
 * Compose bottom sheet untuk Selection Tool Studio.
 * Gayanya identik 100% dengan DrawDetailComposable & 3D Rotate:
 * Card bawah putih + drag handle + divider, baris kontrol di kolom kiri,
 * tombol Cancel / Apply (To Mask) vertikal di kolom kanan.
 */
@Composable
fun SelectionToolPanel(
    hasSelection: Boolean,
    onToolChanged: (PixelCanvasView.SelectionTool) -> Unit,
    onApplyToMask: (feather: Int, inverted: Boolean) -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 400
) {
    var activeTool by remember { mutableStateOf(PixelCanvasView.SelectionTool.RECT) }
    var feather by remember { mutableFloatStateOf(0f) }
    var inverted by remember { mutableStateOf(false) }

    MaterialTheme(colors = SelectColorScheme) {
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
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kolom Kiri: Baris Kontrol (Scrollable)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            // Pilihan Mode Seleksi (Segmented Pill Chips)
                            Text(
                                text = "Selection Mode",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("Rectangle", PixelCanvasView.SelectionTool.RECT, R.drawable.ic_sharp_crop_square_24px),
                                    Triple("Ellipse", PixelCanvasView.SelectionTool.ELLIPSE, R.drawable.ic_sharp_circle_outline_24px),
                                    Triple("Lasso", PixelCanvasView.SelectionTool.LASSO, R.drawable.ic_curve_24px)
                                ).forEach { (label, tool, iconRes) ->
                                    val isSelected = activeTool == tool
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colors.primary else Color(0xFFF1F5F9)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colors.primary else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                activeTool = tool
                                                onToolChanged(tool)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = label,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else Color(PanelTextSecondary)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color(PanelTextSecondary)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // SettingRow Feather (Label | Slider | Nilai)
                            SettingRow(
                                label = "Feather",
                                value = feather,
                                valueRange = 0f..100f,
                                displayValue = "${feather.toInt()} px",
                                onValueChange = { feather = it }
                            )

                            // Baris Invert Mask
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Invert Mask",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(76.dp)
                                )
                                Switch(
                                    checked = inverted,
                                    onCheckedChange = { inverted = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colors.primary,
                                        checkedTrackColor = MaterialTheme.colors.primaryVariant,
                                        uncheckedThumbColor = Color(0xFFB0BEC5),
                                        uncheckedTrackColor = Color(0xFFCFD8DC)
                                    )
                                )
                            }

                            // Caption Petunjuk Kanvas
                            Text(
                                text = "Drag pada foto di canvas untuk membuat area seleksi",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Kolom Kanan: Tombol Cancel & Apply (Vertikal identik Draw Studio)
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
                                Icon(
                                    painter = painterResource(R.drawable.ic_sharp_clear_24px),
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(PanelTextSecondary)
                                )
                            }
                            Button(
                                onClick = { onApplyToMask(feather.toInt(), inverted) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = hasSelection,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White,
                                    disabledBackgroundColor = Color(0xFFE2E8F0),
                                    disabledContentColor = Color(0xFF94A3B8)
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = "Apply To Mask",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (hasSelection) Color.White else Color(0xFF94A3B8)
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
 * SettingRow di DrawDetailComposable & 3D Rotate.
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