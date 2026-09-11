package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasSizePreset

private val CanvasSizeColorScheme = lightColors(
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
 * Compose bottom sheet untuk pengaturan resolusi dan rasio aspek kanvas.
 */
@Composable
fun CanvasSizeDetailPage(
    initialWidth: Int,
    initialHeight: Int,
    onApply: (width: Int, height: Int) -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 450
) {
    val startW = if (initialWidth > 0) initialWidth else 1080
    val startH = if (initialHeight > 0) initialHeight else 1080

    var widthText by remember { mutableStateOf(startW.toString()) }
    var heightText by remember { mutableStateOf(startH.toString()) }
    var isLocked by remember { mutableStateOf(true) }
    var lockedRatio by remember {
        mutableStateOf(startW.toFloat() / startH.toFloat().coerceAtLeast(1f))
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentW = widthText.toIntOrNull() ?: 0
    val currentH = heightText.toIntOrNull() ?: 0

    val ratioBadgeText = remember(currentW, currentH) {
        if (currentW > 0 && currentH > 0) {
            CanvasSizePreset.formatAspectRatio(currentW, currentH)
        } else "—"
    }

    fun applyDimensions(w: Int, h: Int) {
        widthText = w.toString()
        heightText = h.toString()
        lockedRatio = w.toFloat() / h.toFloat().coerceAtLeast(1f)
        errorMessage = null
    }

    MaterialTheme(colors = CanvasSizeColorScheme) {
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
                            .background(Color(PanelHandle), CircleShape)
                    )

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Canvas Size & Ratio",
                            style = MaterialTheme.typography.subtitle1.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface
                            )
                        )
                        TextButton(
                            onClick = {
                                applyDimensions(1080, 1080)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Reset",
                                color = Color(0xFFE53935),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Divider(color = Color(PanelDivider), thickness = 1.dp)

                    // Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Ratio & Dimension Status Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F4FF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_aspect_ratio_24px),
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ratio: $ratioBadgeText",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.primary
                                )
                            }
                            Text(
                                text = "${currentW} × ${currentH} px",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(PanelTextSecondary)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Width, Lock, Height Inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = widthText,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }.take(5)
                                    widthText = filtered
                                    val w = filtered.toIntOrNull() ?: 0
                                    if (isLocked && lockedRatio > 0f && w > 0) {
                                        val newH = (w / lockedRatio).toInt().coerceAtLeast(1)
                                        heightText = newH.toString()
                                    }
                                    errorMessage = null
                                },
                                label = { Text("Width", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFFCCD6E0)
                                )
                            )

                            // Lock Aspect Ratio button
                            IconButton(
                                onClick = {
                                    val next = !isLocked
                                    isLocked = next
                                    if (next && currentH > 0 && currentW > 0) {
                                        lockedRatio = currentW.toFloat() / currentH.toFloat()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLocked) MaterialTheme.colors.primary.copy(alpha = 0.12f)
                                        else Color(0xFFF0F3F8)
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLocked) R.drawable.ic_lock_24px
                                        else R.drawable.ic_lock_open_24px
                                    ),
                                    contentDescription = if (isLocked) "Aspect Ratio Locked" else "Aspect Ratio Unlocked",
                                    tint = if (isLocked) MaterialTheme.colors.primary else Color(PanelTextSecondary),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            OutlinedTextField(
                                value = heightText,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }.take(5)
                                    heightText = filtered
                                    val h = filtered.toIntOrNull() ?: 0
                                    if (isLocked && lockedRatio > 0f && h > 0) {
                                        val newW = (h * lockedRatio).toInt().coerceAtLeast(1)
                                        widthText = newW.toString()
                                    }
                                    errorMessage = null
                                },
                                label = { Text("Height", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = MaterialTheme.colors.primary,
                                    unfocusedBorderColor = Color(0xFFCCD6E0)
                                )
                            )
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFE53935),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Presets
                        Text(
                            text = "Standard Presets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(PanelTextSecondary),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CanvasSizePreset.PRESETS.filter { it.name != "Custom" }.forEach { preset ->
                                val isSelected = (currentW == preset.width && currentH == preset.height)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colors.primary
                                            else Color(0xFFF0F3F8)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colors.primary else Color(PanelDivider),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            applyDimensions(preset.width, preset.height)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = preset.name.substringBefore(" ("),
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "${preset.width}×${preset.height}",
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color(0xFFE0E8FF) else Color(PanelTextSecondary)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color(PanelDivider), thickness = 1.dp)

                    // Footer Actions: Cancel & Apply
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Cancel",
                                color = Color(PanelTextSecondary),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                val w = widthText.toIntOrNull() ?: 0
                                val h = heightText.toIntOrNull() ?: 0
                                when {
                                    w < 50 || h < 50 -> {
                                        errorMessage = "Minimum canvas size is 50 × 50 px"
                                    }
                                    w > 8192 || h > 8192 -> {
                                        errorMessage = "Maximum canvas size is 8192 × 8192 px"
                                    }
                                    else -> {
                                        onApply(w, h)
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                        ) {
                            Text(
                                "Apply",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
