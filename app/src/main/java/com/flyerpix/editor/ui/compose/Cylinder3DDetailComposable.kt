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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.PodiumStyle

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val CYLINDER_SWATCH_COLORS = listOf(
    0xFFF3F4F6.toInt(),
    0xFFD4AF37.toInt(),
    0xFF26262E.toInt(),
    0xFFF9C5D5.toInt(),
    0xFF1769FF.toInt(),
    0xFFE53935.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt()
)

private val RING_COLORS = listOf(
    0xFFFFD700.toInt() to "Emas",
    0xFFC0C0C0.toInt() to "Silver",
    0xFFB76E79.toInt() to "Rose",
    0xFFFFFFFF.toInt() to "Putih",
    0xFF111111.toInt() to "Hitam"
)

/**
 * Compose bottom sheet untuk studio Podium 3D Cylinder.
 *
 * Urutan kontrol mengalir: Gaya ➔ Bentuk & Tingkat ➔ Warna Panggung & Badan ➔
 * Ring Bibir ➔ Bayangan Lantai ➔ Finishing Effects.
 */
@Composable
fun Cylinder3DDetailPage(
    style: PodiumStyle,
    tierCount: Int,
    radiusX: Float,
    cylinderHeight: Float,
    radiusY: Float,
    baseColor: Int,
    topColorEnabled: Boolean,
    topColor: Int,
    topRingEnabled: Boolean,
    topRingColor: Int,
    topRingWidth: Float,
    lightAngle: Float,
    floorShadowEnabled: Boolean,
    floorShadowOpacityPct: Float,
    opacityPct: Float,
    onStyleChange: (PodiumStyle) -> Unit,
    onTierCountChange: (Int) -> Unit,
    onRadiusXChange: (Float) -> Unit,
    onCylinderHeightChange: (Float) -> Unit,
    onRadiusYChange: (Float) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onTopColorEnabledChange: (Boolean) -> Unit,
    onTopColorChange: (Int) -> Unit,
    onOpenTopColorPicker: () -> Unit,
    onTopRingEnabledChange: (Boolean) -> Unit,
    onTopRingColorChange: (Int) -> Unit,
    onOpenRingColorPicker: () -> Unit,
    onTopRingWidthChange: (Float) -> Unit,
    onLightAngleChange: (Float) -> Unit,
    onFloorShadowEnabledChange: (Boolean) -> Unit,
    onFloorShadowOpacityChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenEmbossEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 480
) {
    var styleState by remember(style) { mutableStateOf(style) }
    var tierCountState by remember(tierCount) { mutableStateOf(tierCount) }
    var radiusXState by remember(radiusX) { mutableStateOf(radiusX) }
    var cylinderHeightState by remember(cylinderHeight) { mutableStateOf(cylinderHeight) }
    var radiusYState by remember(radiusY) { mutableStateOf(radiusY) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var topColorEnabledState by remember(topColorEnabled) { mutableStateOf(topColorEnabled) }
    var topColorState by remember(topColor) { mutableStateOf(topColor) }
    var topRingEnabledState by remember(topRingEnabled) { mutableStateOf(topRingEnabled) }
    var topRingColorState by remember(topRingColor) { mutableStateOf(topRingColor) }
    var topRingWidthState by remember(topRingWidth) { mutableStateOf(topRingWidth) }
    var lightAngleState by remember(lightAngle) { mutableStateOf(lightAngle) }
    var floorShadowEnabledState by remember(floorShadowEnabled) { mutableStateOf(floorShadowEnabled) }
    var floorShadowOpacityState by remember(floorShadowOpacityPct) { mutableStateOf(floorShadowOpacityPct) }
    var opacityState by remember(opacityPct) { mutableStateOf(opacityPct) }

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
            backgroundColor = Color.White
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
                        .background(DragHandleColor, RoundedCornerShape(50))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: Scrollable controls
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        // ── 1. Gaya Panggung (Style Presets) ────────────────
                        Text(
                            text = "Gaya Panggung",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                PodiumStyle.MINIMAL_STUDIO to "Studio",
                                PodiumStyle.LUXURY_GOLD to "Gold",
                                PodiumStyle.DARK_ELEGANCE to "Dark",
                                PodiumStyle.PASTEL_POP to "Pastel",
                                PodiumStyle.CYBER_NEON to "Neon"
                            ).forEach { (s, label) ->
                                val isSelected = styleState == s
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE8F0FE) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            styleState = s
                                            onStyleChange(s)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // ── 2. Bentuk & Tingkat Podium ───────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Level & Bentuk Podium",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1 to "1 Tingkat", 2 to "2 Tingkat").forEach { (level, label) ->
                                val isSelected = tierCountState == level
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFFDEBC8) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            tierCountState = level
                                            onTierCountChange(level)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFB45309) else Color(0xFF1A1A2E),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        LabeledSliderRow(
                            label = "Lebar Podium",
                            value = radiusXState * 2f,
                            onValueChange = { v ->
                                radiusXState = v / 2f
                                onRadiusXChange(v / 2f)
                            },
                            valueRange = 40f..700f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Tinggi Silinder",
                            value = cylinderHeightState,
                            onValueChange = { v ->
                                cylinderHeightState = v
                                onCylinderHeightChange(v)
                            },
                            valueRange = 10f..400f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Perspective Tilt",
                            value = radiusYState,
                            onValueChange = { v ->
                                radiusYState = v
                                onRadiusYChange(v)
                            },
                            valueRange = 5f..220f,
                            suffix = ""
                        )

                        // ── 3. Warna Panggung & Badan ────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Warna Panggung & Badan",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "Warna Badan Silinder",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CYLINDER_SWATCH_COLORS.forEach { c ->
                                val isPicked = baseColorState == c
                                Box(
                                    modifier = Modifier
                                        .size(if (isPicked) 26.dp else 22.dp)
                                        .clip(CircleShape)
                                        .background(Color(c))
                                        .border(2.dp, Color.White, CircleShape)
                                        .then(
                                            if (isPicked) Modifier.border(2.dp, PrimaryBlue, CircleShape) else Modifier
                                        )
                                        .clickable {
                                            baseColorState = c
                                            onBaseColorChange(c)
                                        }
                                ) {}
                            }
                            AddColorSwatchButton(
                                onClick = onOpenBaseColorPicker,
                                buttonSize = 22.dp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Warna Atas Independen",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = topColorEnabledState,
                                onCheckedChange = {
                                    topColorEnabledState = it
                                    onTopColorEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (topColorEnabledState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(topColorState))
                                        .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                                        .clickable(onClick = onOpenTopColorPicker)
                                ) {}
                                Text(
                                    text = "Pilih warna permukaan atas",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // ── 4. Hiasan Bibir Panggung (Ring) ──────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ring Bibir Panggung",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = topRingEnabledState,
                                onCheckedChange = {
                                    topRingEnabledState = it
                                    onTopRingEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (topRingEnabledState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RING_COLORS.forEach { (c, label) ->
                                    val isPicked = topRingColorState == c
                                    Box(
                                        modifier = Modifier
                                            .size(if (isPicked) 26.dp else 22.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(2.dp, Color.White, CircleShape)
                                            .then(
                                                if (isPicked) Modifier.border(2.dp, Color(0xFFF59E0B), CircleShape)
                                                else Modifier
                                            )
                                            .clickable {
                                                topRingColorState = c
                                                onTopRingColorChange(c)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (label) {
                                                "Emas", "Silver", "Rose" -> ""
                                                else -> label.substring(0, 1)
                                            },
                                            style = MaterialTheme.typography.caption,
                                            color = if (c == 0xFFFFFFFF.toInt() || isPicked) Color(0xFF1A1A2E) else Color.White,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenRingColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            LabeledSliderRow(
                                label = "Ketebalan Ring",
                                value = topRingWidthState,
                                onValueChange = { v ->
                                    topRingWidthState = v
                                    onTopRingWidthChange(v)
                                },
                                valueRange = 0.5f..10f,
                                suffix = " px"
                            )
                            LabeledSliderRow(
                                label = "Arah Cahaya",
                                value = lightAngleState,
                                onValueChange = { v ->
                                    lightAngleState = v
                                    onLightAngleChange(v)
                                },
                                valueRange = -180f..180f,
                                suffix = "°"
                            )
                        }

                        // ── 5. Bayangan Lantai ───────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bayangan Lantai",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = floorShadowEnabledState,
                                onCheckedChange = {
                                    floorShadowEnabledState = it
                                    onFloorShadowEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        LabeledSliderRow(
                            label = "Opacity Shadow",
                            value = floorShadowOpacityState,
                            onValueChange = { v ->
                                floorShadowOpacityState = v
                                onFloorShadowOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )
                        LabeledSliderRow(
                            label = "Opacity Panggung",
                            value = opacityState,
                            onValueChange = { v ->
                                opacityState = v
                                onOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 6. Finishing Effects ─────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Finishing Effects",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenShadowEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Drop Shadow", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onOpenEmbossEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Emboss", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Neon Glow", fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "Tip: Drop Shadow membentuk bayangan panggung • Emboss meng-holis ring bibir • Neon Glow membuat ring & halo berpendar.",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // RIGHT COLUMN: Buttons (Fixed)
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
                                tint = Color(TextSecondary)
                            )
                        }
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24px),
                                contentDescription = "Apply",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Text("Reset", style = MaterialTheme.typography.caption, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            modifier = Modifier.width(104.dp),
            fontSize = 10.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = if (valueRange.endInclusive - valueRange.start > 1f) 99 else 0,
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        )
        Text(
            text = "${value.toInt()}$suffix",
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(42.dp),
            fontSize = 10.sp
        )
    }
}