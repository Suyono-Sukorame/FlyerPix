package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType

/**
 * Composable panel untuk Replace Background workflow (Phase 8 - Prompt 08).
 * Memungkinkan user untuk memilih background baru (Solid/Gradient/Gallery),
 * dengan opsi auto color match.
 *
 * @param currentBackground Background saat ini
 * @param onBackgroundChange Callback saat background berubah
 * @param onColorMatchToggle Callback saat toggle auto color match
 * @param onGalleryClick Callback untuk membuka galeri
 * @param onClose Callback saat panel ditutup
 */
@Composable
fun ReplaceBackgroundComposable(
    currentBackground: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
    onColorMatchToggle: (Boolean) -> Unit,
    onGalleryClick: () -> Unit,
    onClose: () -> Unit,
    autoColorMatchEnabled: Boolean = false
) {
    var selectedMode by remember { mutableStateOf(currentBackground.mode) }
    var autoColorMatch by remember { mutableStateOf(autoColorMatchEnabled) }
    var selectedSolidColor by remember { mutableStateOf(currentBackground.solidColor) }
    var selectedGradient by remember { mutableStateOf(currentBackground.gradient ?: GradientColor(colors = intArrayOf(0xFF0066FF.toInt(), 0xFF00AAFF.toInt()), type = GradientType.LINEAR)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─────────────────────────────────────────────────────────────────
        // Header
        // ─────────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replace Background",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Background Type Selector
        // ─────────────────────────────────────────────────────────────────
        item {
            Text(
                text = "Background Type",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBBBBBB),
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Solid button
                BackgroundTypeButton(
                    label = "Solid",
                    isSelected = selectedMode == CanvasBackgroundMode.SOLID_COLOR,
                    onClick = { selectedMode = CanvasBackgroundMode.SOLID_COLOR },
                    modifier = Modifier.weight(1f)
                )
                // Gradient button
                BackgroundTypeButton(
                    label = "Gradient",
                    isSelected = selectedMode == CanvasBackgroundMode.GRADIENT,
                    onClick = { selectedMode = CanvasBackgroundMode.GRADIENT },
                    modifier = Modifier.weight(1f)
                )
                // Gallery button
                BackgroundTypeButton(
                    label = "Gallery",
                    isSelected = selectedMode == CanvasBackgroundMode.IMAGE,
                    onClick = { 
                        selectedMode = CanvasBackgroundMode.IMAGE
                        onGalleryClick()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Solid Color Presets
        // ─────────────────────────────────────────────────────────────────
        if (selectedMode == CanvasBackgroundMode.SOLID_COLOR) {
            item {
                Text(
                    text = "Color Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                val solidPresets = listOf(
                    0xFFFFFFFF.toInt() to "White",
                    0xFF000000.toInt() to "Black",
                    0xFF666666.toInt() to "Gray",
                    0xFFFF6B6B.toInt() to "Red",
                    0xFF4ECDC4.toInt() to "Teal",
                    0xFFFFE66D.toInt() to "Yellow"
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in solidPresets.indices step 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (j in 0..2) {
                                if (i + j < solidPresets.size) {
                                    val (color, name) = solidPresets[i + j]
                                    SolidColorPresetButton(
                                        color = color,
                                        label = name,
                                        isSelected = selectedSolidColor == color,
                                        onClick = {
                                            selectedSolidColor = color
                                            onBackgroundChange(CanvasBackground.solid(color))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Gradient Presets
        // ─────────────────────────────────────────────────────────────────
        if (selectedMode == CanvasBackgroundMode.GRADIENT) {
            item {
                Text(
                    text = "Gradient Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFBBBBBB),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sunset gradient
                    GradientPresetButton(
                        label = "Sunset",
                        gradient = GradientColor(
                            colors = intArrayOf(0xFFFF6B35.toInt(), 0xFFFFA500.toInt()),
                            type = GradientType.LINEAR
                        ),
                        onClick = {
                            selectedGradient = GradientColor(
                                colors = intArrayOf(0xFFFF6B35.toInt(), 0xFFFFA500.toInt()),
                                type = GradientType.LINEAR
                            )
                            onBackgroundChange(CanvasBackground.gradient(selectedGradient))
                        }
                    )
                    // Ocean gradient
                    GradientPresetButton(
                        label = "Ocean",
                        gradient = GradientColor(
                            colors = intArrayOf(0xFF667EEA.toInt(), 0xFF764BA2.toInt()),
                            type = GradientType.LINEAR
                        ),
                        onClick = {
                            selectedGradient = GradientColor(
                                colors = intArrayOf(0xFF667EEA.toInt(), 0xFF764BA2.toInt()),
                                type = GradientType.LINEAR
                            )
                            onBackgroundChange(CanvasBackground.gradient(selectedGradient))
                        }
                    )
                    // Lime gradient
                    GradientPresetButton(
                        label = "Lime",
                        gradient = GradientColor(
                            colors = intArrayOf(0xFF84FAFF.toInt(), 0xFF00FFC6.toInt()),
                            type = GradientType.LINEAR
                        ),
                        onClick = {
                            selectedGradient = GradientColor(
                                colors = intArrayOf(0xFF84FAFF.toInt(), 0xFF00FFC6.toInt()),
                                type = GradientType.LINEAR
                            )
                            onBackgroundChange(CanvasBackground.gradient(selectedGradient))
                        }
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Auto Color Match Toggle
        // ─────────────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2A2A), shape = MaterialTheme.shapes.small)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Auto Color Match",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Adjust layer colors to blend with new background",
                        fontSize = 11.sp,
                        color = Color(0xFF999999)
                    )
                }
                Switch(
                    checked = autoColorMatch,
                    onCheckedChange = {
                        autoColorMatch = it
                        onColorMatchToggle(it)
                    }
                )
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // Apply Button
        // ─────────────────────────────────────────────────────────────────
        item {
            Button(
                onClick = {
                    // Background change already applied in presets
                    onClose()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF0066FF)
                )
            ) {
                Text("Apply", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BackgroundTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color(0xFF0066FF) else Color(0xFF333333)
        )
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SolidColorPresetButton(
    color: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    Color(color),
                    shape = MaterialTheme.shapes.medium
                )
                .let {
                    if (isSelected) {
                        it.then(
                            Modifier
                                .border(3.dp, Color(0xFF00FF00), MaterialTheme.shapes.medium)
                        )
                    } else {
                        it
                    }
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFBBBBBB),
            maxLines = 1
        )
    }
}

@Composable
private fun GradientPresetButton(
    label: String,
    gradient: GradientColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF333333)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show gradient preview
            if (gradient.colors.size >= 2) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    Color(gradient.colors[0]),
                                    Color(gradient.colors[gradient.colors.size - 1])
                                )
                            ),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
