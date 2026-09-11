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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType

private val CanvasColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF0F3F8
private const val PanelHandle = 0xFFD0D4DE

private val SolidPresetColors = intArrayOf(
    0xFFFFFFFF.toInt(), // White
    0xFF000000.toInt(), // Black
    0xFF212121.toInt(), // Charcoal
    0xFFB0BEC5.toInt(), // Gray
    0xFF00E5FF.toInt(), // Cyan
    0xFF00B0FF.toInt(), // Light Blue
    0xFF2979FF.toInt(), // Blue
    0xFF651FFF.toInt(), // Indigo
    0xFFAA00FF.toInt(), // Purple
    0xFFF50057.toInt(), // Pink
    0xFFE53935.toInt(), // Red
    0xFFFF6D00.toInt(), // Orange
    0xFFFFD600.toInt(), // Yellow
    0xFF00E676.toInt(), // Green
    0xFF00BFA5.toInt(), // Teal
    0xFFFFF9C4.toInt(), // Pastel Cream
    0xFFBBDEFB.toInt(), // Pastel Blue
    0xFFC8E6C9.toInt(), // Pastel Mint
    0xFFFFCCBC.toInt()  // Pastel Peach
)

/**
 * Compose bottom sheet untuk pengaturan latar belakang kanvas (Transparent, Solid, Gradient, Image).
 * Didesain 100% identik dengan ThreeDRotateDetailPage: 2 kolom (kiri: pengaturan & reset, kanan: Cancel & Apply).
 */
@Composable
fun CanvasBgDetailPage(
    currentMode: CanvasBackgroundMode,
    solidColor: Int,
    gradient: GradientColor?,
    hasImage: Boolean,
    onModeChange: (CanvasBackgroundMode) -> Unit,
    onSolidColorChange: (Int) -> Unit,
    onOpenColorPicker: () -> Unit,
    onGradientChange: (GradientColor) -> Unit,
    onOpenGradientPicker: () -> Unit,
    onGalleryPickRequested: () -> Unit,
    onCameraRequested: () -> Unit,
    onRemoveImage: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var modeState by remember(currentMode) { mutableStateOf(currentMode) }
    var solidColorState by remember(solidColor) { mutableStateOf(solidColor) }
    var gradState by remember(gradient) {
        mutableStateOf(gradient ?: GradientColor.PRESETS.first())
    }

    MaterialTheme(colors = CanvasColorScheme) {
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
                    // Drag handle
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
                        // Left Column (Controls & Reset)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            // Segmented Mode Selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modes = listOf(
                                    CanvasBackgroundMode.TRANSPARENT to "Transparent",
                                    CanvasBackgroundMode.SOLID_COLOR to "Solid",
                                    CanvasBackgroundMode.GRADIENT to "Gradient",
                                    CanvasBackgroundMode.IMAGE to "Image"
                                )
                                for ((m, label) in modes) {
                                    val isSelected = modeState == m
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colors.primary
                                                else Color(PanelAlt)
                                            )
                                            .clickable {
                                                modeState = m
                                                onModeChange(m)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(PanelTextSecondary)
                                        )
                                    }
                                }
                            }

                            // Content based on selected mode
                            when (modeState) {
                                CanvasBackgroundMode.TRANSPARENT -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(PanelDivider), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_aspect_ratio_24px),
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Checkerboard Transparent",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colors.onSurface
                                                )
                                                Text(
                                                    text = "Ideal for transparent PNGs, logos, and stickers.",
                                                    fontSize = 10.sp,
                                                    color = Color(PanelTextSecondary)
                                                )
                                            }
                                        }
                                    }
                                }

                                CanvasBackgroundMode.SOLID_COLOR -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Custom color picker button
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE8EEF5))
                                                .border(1.dp, Color(0xFFCCD6E0), CircleShape)
                                                .clickable { onOpenColorPicker() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Custom Color",
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Swatches
                                        SolidPresetColors.forEach { c ->
                                            val isSelected = (solidColorState and 0x00FFFFFF) == (c and 0x00FFFFFF)
                                            val isLight = (c == 0xFFFFFFFF.toInt() || c == 0xFFFFF9C4.toInt())
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(c))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else if (isLight) 1.dp else 0.dp,
                                                        color = if (isSelected) MaterialTheme.colors.primary
                                                        else if (isLight) Color(0xFFCCD6E0)
                                                        else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        solidColorState = c
                                                        onSolidColorChange(c)
                                                    }
                                            )
                                        }
                                    }
                                }

                                CanvasBackgroundMode.GRADIENT -> {
                                    // Gradient Type selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            GradientType.LINEAR to "Linear",
                                            GradientType.RADIAL to "Radial",
                                            GradientType.SWEEP to "Sweep"
                                        ).forEach { (gt, label) ->
                                            val sel = gradState.type == gt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (sel) MaterialTheme.colors.primary else Color(PanelAlt))
                                                    .clickable {
                                                        val updated = gradState.copy(type = gt)
                                                        gradState = updated
                                                        onGradientChange(updated)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (sel) Color.White else Color(PanelTextSecondary)
                                                )
                                            }
                                        }
                                    }

                                    // Angle slider if Linear
                                    if (gradState.type == GradientType.LINEAR) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Angle",
                                                style = MaterialTheme.typography.caption,
                                                color = Color(PanelTextSecondary),
                                                modifier = Modifier.width(44.dp)
                                            )
                                            Slider(
                                                value = gradState.angle,
                                                onValueChange = { ang ->
                                                    val updated = gradState.copy(angle = ang)
                                                    gradState = updated
                                                    onGradientChange(updated)
                                                },
                                                valueRange = 0f..360f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = MaterialTheme.colors.primary,
                                                    activeTrackColor = MaterialTheme.colors.primary
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                            )
                                            Text(
                                                text = "${gradState.angle.toInt()}°",
                                                style = MaterialTheme.typography.caption,
                                                color = Color(PanelTextSecondary),
                                                textAlign = TextAlign.End,
                                                modifier = Modifier.width(36.dp)
                                            )
                                        }
                                    }

                                    // Presets
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Custom gradient button
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE8EEF5))
                                                .border(1.dp, Color(0xFFCCD6E0), CircleShape)
                                                .clickable { onOpenGradientPicker() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Custom Gradient",
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))

                                        GradientColor.PRESETS.forEach { preset ->
                                            val isSel = gradState.colors.contentEquals(preset.colors)
                                            val brushColors = preset.colors.map { Color(it) }
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.horizontalGradient(brushColors))
                                                    .border(
                                                        width = if (isSel) 2.5.dp else 1.dp,
                                                        color = if (isSel) MaterialTheme.colors.primary else Color(0x33000000),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        val updated = preset.copy(
                                                            type = gradState.type,
                                                            angle = gradState.angle
                                                        )
                                                        gradState = updated
                                                        onGradientChange(updated)
                                                    }
                                            )
                                        }
                                    }
                                }

                                CanvasBackgroundMode.IMAGE -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onGalleryPickRequested() },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_outline_photo_24px),
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        OutlinedButton(
                                            onClick = { onCameraRequested() },
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_outline_camera_alt_24px),
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Camera", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    if (hasImage) {
                                        TextButton(
                                            onClick = { onRemoveImage() },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete_24px),
                                                contentDescription = null,
                                                tint = Color(0xFFE53935),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Remove Background Image",
                                                color = Color(0xFFE53935),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Reset Action centered
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        modeState = CanvasBackgroundMode.SOLID_COLOR
                                        solidColorState = 0xFFFFFFFF.toInt()
                                        onReset()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Right Column (Cancel & Apply)
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
