package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.MaskUtils

private val PanelColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF0F3F8
private const val PanelHandle = 0xFFD0D4DE
private const val PanelCard = 0xFFF8FAFC
private const val PanelSwatchBorder = 0xFFCCD6E0

/** Chip ikon (mode/aksi/arah) — tanpa teks, aktif = primary. */
@Composable
private fun IconChip(
    iconRes: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (active) Color.White else Color(PanelTextSecondary),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Chip ikon panah berputar (arah gradient) — tanpa teks. */
@Composable
private fun ArrowDirectionChip(
    degrees: Float,
    modifier: Modifier = Modifier,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(PanelAlt))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_24px),
            contentDescription = contentDescription,
            tint = Color(PanelTextSecondary),
            modifier = Modifier
                .size(18.dp)
                .rotate(degrees)
        )
    }
}

/** Baris slider: ikon + slider + nilai angka (mis. ukuran kuas). */
@Composable
private fun IconSliderRow(
    iconRes: Int,
    iconDesc: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    valueWidth: Dp = 44.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconDesc,
            tint = Color(PanelTextSecondary),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
        )
        Text(
            text = valueText,
            fontSize = 10.sp,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(valueWidth)
        )
    }
}

/** Card toggle (Switch) ikon — tanpa teks. */
@Composable
private fun IconToggleRowCard(
    iconRes: Int,
    iconDesc: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(PanelCard),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = iconDesc,
                tint = MaterialTheme.colors.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colors.primary,
                    checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun LayerMaskEditorComposable(
    layer: CanvasLayer,
    onMaskChange: () -> Unit,
    onClose: () -> Unit,
    onGradientMaskApply: (GradientType, FloatArray) -> Unit,
    onFeatherApply: (Float) -> Unit,
    onBrushConfig: (brushSize: Float, brushOpacity: Int, color: Int, isEraser: Boolean) -> Unit,
    onEnableMaskPaint: () -> Unit,
    onFinish: () -> Unit,
    onModeChange: (mode: String) -> Unit = {},  // "manual", "auto-erase", "clone"
    onPressureToggle: (enabled: Boolean) -> Unit = {},
    onAutoEraseThreshold: (threshold: Int) -> Unit = {},
    onCloneModeToggle: (enabled: Boolean) -> Unit = {},
    maxHeightPx: Int = 420,
    sessionKey: Int = 0
) {
    var brushSize by remember { mutableStateOf(20f) }
    var brushOpacity by remember { mutableStateOf(1f) }
    var isInverted by remember { mutableStateOf(layer.maskInverted) }
    var isEraser by remember { mutableStateOf(false) }
    // Default kuas HITAM (hapus): melukis langsung menghapus, sesuai
    // "Paint black to remove, white to restore". Putih/restore via swatch.
    var selectedBrushColor by remember { mutableStateOf(0xFF000000.toInt()) }

    // ── Phase 4 & 5: Smart Erase Modes (OPTION D) ────────────────────────────
    var paintMode by remember { mutableStateOf("manual") }  // "manual", "auto-erase", "clone"
    var pressureSensitivityEnabled by remember { mutableStateOf(true) }
    var autoEraseThreshold by remember { mutableStateOf(50) }
    var cloneModeEnabled by remember { mutableStateOf(false) }
    var featherRadius by remember { mutableStateOf(10f) }

    // Gate konten editor: selama mask sudah ada (independen dari maskEnabled).
    var maskAvailable by remember { mutableStateOf(layer.maskBitmap != null) }
    var maskEnabledState by remember { mutableStateOf(layer.maskEnabled) }
    val scrollState = remember(sessionKey) { ScrollState(0) }
    val sheetCapDp = with(LocalDensity.current) { maxHeightPx.toDp() }
        .coerceAtMost((LocalConfiguration.current.screenHeightDp * 0.45f).dp)

    fun fillColor(): Int = when (selectedBrushColor) {
        0xFF000000.toInt() -> 0x00000000.toInt()
        0xFF808080.toInt() -> 0xFF808080.toInt()
        else               -> 0xFFFFFFFF.toInt()
    }

    fun pushBrushConfig() {
        onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser)
    }

    LaunchedEffect(Unit) {
        pushBrushConfig()
        onEnableMaskPaint()
    }

    MaterialTheme(colors = PanelColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetCapDp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                elevation = 8.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(3.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 0.5.dp)

                            if (!maskAvailable) {
                                // ── Create Mask (ikon) ───────────────────────────────────
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colors.primary,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val (w, h) = layer.getUnwarpedDimensions()
                                            if (w > 0 && h > 0) {
                                                layer.createMask(w.toInt(), h.toInt())
                                                layer.maskBitmap?.eraseColor(0xFFFFFFFF.toInt())
                                                maskAvailable = true
                                                maskEnabledState = true
                                                pushBrushConfig()
                                                onEnableMaskPaint()
                                                onMaskChange()
                                            }
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.08f),
                                    elevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_mask_24px),
                                            contentDescription = "Create Mask",
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                // ── Mask Active toggle (ikon) ─────────────────────────────
                                IconToggleRowCard(
                                    iconRes = R.drawable.ic_visibility_24px,
                                    iconDesc = "Mask Active",
                                    checked = maskEnabledState,
                                    onCheckedChange = {
                                        maskEnabledState = it
                                        layer.maskEnabled = it
                                        pushBrushConfig()
                                        onMaskChange()
                                    }
                                )

                                Divider(color = Color(PanelDivider), thickness = 0.5.dp)

                                // ── Brush (ukuran & opacity) ────────────────────────────
                                IconSliderRow(
                                    iconRes = R.drawable.ic_size_24px,
                                    iconDesc = "Brush Size",
                                    value = brushSize,
                                    onValueChange = { brushSize = it; pushBrushConfig() },
                                    valueRange = 5f..100f,
                                    valueText = "${brushSize.toInt()} px"
                                )
                                IconSliderRow(
                                    iconRes = R.drawable.ic_opacity_24px,
                                    iconDesc = "Brush Opacity",
                                    value = brushOpacity,
                                    onValueChange = { brushOpacity = it; pushBrushConfig() },
                                    valueRange = 0f..1f,
                                    valueText = "${(brushOpacity * 100).toInt()}%"
                                )

                                // ── Erase Mode (ikon) ───────────────────────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconChip(
                                        iconRes = R.drawable.ic_sharp_brush_24px,
                                        modifier = Modifier.weight(1f),
                                        active = paintMode == "manual",
                                        contentDescription = "Manual Erase",
                                        onClick = {
                                            paintMode = "manual"
                                            onModeChange("manual")
                                            onCloneModeToggle(false)
                                        }
                                    )
                                    IconChip(
                                        iconRes = R.drawable.ic_sharp_colorize_24px,
                                        modifier = Modifier.weight(1f),
                                        active = paintMode == "auto-erase",
                                        contentDescription = "Auto-Color Erase",
                                        onClick = {
                                            paintMode = "auto-erase"
                                            onModeChange("auto-erase")
                                            onCloneModeToggle(false)
                                        }
                                    )
                                    IconChip(
                                        iconRes = R.drawable.ic_copy_24px,
                                        modifier = Modifier.weight(1f),
                                        active = paintMode == "clone",
                                        contentDescription = "Clone Stamp",
                                        onClick = {
                                            paintMode = "clone"
                                            onModeChange("clone")
                                        }
                                    )
                                }

                                // ── Pressure Sensitivity (ikon) ────────────────────────
                                IconToggleRowCard(
                                    iconRes = R.drawable.ic_edit_24px,
                                    iconDesc = "Pressure Sensitivity",
                                    checked = pressureSensitivityEnabled,
                                    onCheckedChange = {
                                        pressureSensitivityEnabled = it
                                        onPressureToggle(it)
                                    }
                                )

                                // ── Auto-Color Erase Threshold ─────────────────────────
                                if (paintMode == "auto-erase") {
                                    IconSliderRow(
                                        iconRes = R.drawable.ic_sharp_colorize_24px,
                                        iconDesc = "Auto-Erase Threshold",
                                        value = autoEraseThreshold.toFloat(),
                                        onValueChange = {
                                            autoEraseThreshold = it.toInt()
                                            onAutoEraseThreshold(autoEraseThreshold)
                                        },
                                        valueRange = 0f..255f,
                                        valueText = "$autoEraseThreshold"
                                    )
                                }

                                // ── Clone Stamp (icon-only, instruksi dihapus) ──────────
                                if (paintMode == "clone") {
                                    IconToggleRowCard(
                                        iconRes = R.drawable.ic_copy_24px,
                                        iconDesc = "Enable Clone Mode",
                                        checked = cloneModeEnabled,
                                        onCheckedChange = {
                                            cloneModeEnabled = it
                                            onCloneModeToggle(cloneModeEnabled)
                                        }
                                    )
                                }

                                // ── Brush Color (swatch tanpa label) ───────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    listOf(
                                        0xFFFFFFFF.toInt() to "White (Show)",
                                        0xFF000000.toInt() to "Black (Hide)",
                                        0xFF808080.toInt() to "Gray (Partial)"
                                    ).forEach { (color, label) ->
                                        val selected = selectedBrushColor == color && !isEraser
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .semantics { contentDescription = label }
                                                .background(Color(color))
                                                .border(
                                                    width = if (selected) 2.5.dp else 1.dp,
                                                    color = if (selected) MaterialTheme.colors.primary else Color(PanelSwatchBorder),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    selectedBrushColor = color
                                                    isEraser = false
                                                    pushBrushConfig()
                                                }
                                        )
                                    }
                                }

                                // ── Eraser toggle (ikon) ───────────────────────────────
                                IconChip(
                                    iconRes = R.drawable.ic_eraser_24px,
                                    modifier = Modifier.fillMaxWidth(),
                                    active = isEraser,
                                    contentDescription = if (isEraser) "Eraser On" else "Eraser Off",
                                    onClick = {
                                        isEraser = !isEraser
                                        pushBrushConfig()
                                    }
                                )

                                // ── Mask Actions: Invert / Reset (ikon) ────────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconChip(
                                        iconRes = R.drawable.ic_sharp_flip_24px,
                                        modifier = Modifier.weight(1f),
                                        active = isInverted,
                                        contentDescription = "Invert Mask",
                                        onClick = {
                                            layer.maskInverted = !layer.maskInverted
                                            isInverted = layer.maskInverted
                                            onMaskChange()
                                        }
                                    )
                                    IconChip(
                                        iconRes = R.drawable.ic_sharp_restart_24px,
                                        modifier = Modifier.weight(1f),
                                        active = false,
                                        contentDescription = "Reset Mask",
                                        onClick = {
                                            layer.maskBitmap?.let { bmp ->
                                                val px = IntArray(bmp.width * bmp.height) { 0xFFFFFFFF.toInt() }
                                                bmp.setPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                                            }
                                            onMaskChange()
                                        }
                                    )
                                }

                                // ── Auto Masks: arah gradient (ikon panah) ──────────────
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    ArrowDirectionChip(
                                        degrees = 90f,
                                        modifier = Modifier.weight(1f),
                                        contentDescription = "Gradient Top",
                                        onClick = { onGradientMaskApply(GradientType.LINEAR, floatArrayOf(MaskUtils.DIR_TOP_BOTTOM.toFloat())) }
                                    )
                                    ArrowDirectionChip(
                                        degrees = 270f,
                                        modifier = Modifier.weight(1f),
                                        contentDescription = "Gradient Bottom",
                                        onClick = { onGradientMaskApply(GradientType.LINEAR, floatArrayOf(MaskUtils.DIR_BOTTOM_TOP.toFloat())) }
                                    )
                                    ArrowDirectionChip(
                                        degrees = 0f,
                                        modifier = Modifier.weight(1f),
                                        contentDescription = "Gradient Left",
                                        onClick = { onGradientMaskApply(GradientType.LINEAR, floatArrayOf(MaskUtils.DIR_LEFT_RIGHT.toFloat())) }
                                    )
                                    ArrowDirectionChip(
                                        degrees = 180f,
                                        modifier = Modifier.weight(1f),
                                        contentDescription = "Gradient Right",
                                        onClick = { onGradientMaskApply(GradientType.LINEAR, floatArrayOf(MaskUtils.DIR_RIGHT_LEFT.toFloat())) }
                                    )
                                }
                                IconChip(
                                    iconRes = R.drawable.ic_sharp_circle_outline_24px,
                                    modifier = Modifier.fillMaxWidth(),
                                    active = false,
                                    contentDescription = "Radial Gradient Mask",
                                    onClick = { onGradientMaskApply(GradientType.RADIAL, floatArrayOf(0.5f)) }
                                )

                                // ── Feather (soft edge) ────────────────────────────────
                                IconSliderRow(
                                    iconRes = R.drawable.ic_merge_layers_24px,
                                    iconDesc = "Feather Radius",
                                    value = featherRadius,
                                    onValueChange = { featherRadius = it },
                                    valueRange = 0f..100f,
                                    valueText = "${featherRadius.toInt()} px"
                                )
                                IconChip(
                                    iconRes = R.drawable.ic_check_24px,
                                    modifier = Modifier.fillMaxWidth(),
                                    active = featherRadius > 0f,
                                    contentDescription = "Apply Feather",
                                    onClick = { if (featherRadius > 0f) onFeatherApply(featherRadius) }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .padding(start = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            TextButton(
                                onClick = onClose,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Text("Batal", style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = onFinish,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "Selesai",
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