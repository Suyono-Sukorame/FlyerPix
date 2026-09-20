package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.SphereMaterial
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val SPHERE_SWATCH_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF1769FF.toInt(),
    0xFFE53935.toInt(),
    0xFF43A047.toInt(),
    0xFFFFB300.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00ACC1.toInt(),
    0xFF212121.toInt()
)

/**
 * Compose bottom sheet untuk studio 3D Sphere.
 *
 * Urutan kontrol mengalir:
 * Material ➔ Warna Dasar ➔ Sumber Cahaya (Light Touchpad) ➔ Dimensi &
 * Transparansi ➔ Bayangan Lantai ➔ Outline & Neon Orb.
 */
@Composable
fun Sphere3DDetailPage(
    material: SphereMaterial,
    diameter: Float,
    baseColor: Int,
    lightAngle: Float,
    lightDistance: Float,
    lightIntensity: Float,
    opacityPct: Float,
    floorShadowEnabled: Boolean,
    floorElevation: Float,
    floorShadowOpacityPct: Float,
    strokeWidth: Float,
    strokeColor: Int,
    strokeOpacityPct: Float,
    neonEnabled: Boolean,
    neonColor: Int,
    neonRadius: Float,
    neonIntensity: Float,
    onMaterialChange: (SphereMaterial) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onLightAngleChange: (Float) -> Unit,
    onLightDistanceChange: (Float) -> Unit,
    onLightIntensityChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onFloorShadowEnabledChange: (Boolean) -> Unit,
    onFloorElevationChange: (Float) -> Unit,
    onFloorShadowOpacityChange: (Float) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Int) -> Unit,
    onOpenStrokeColorPicker: () -> Unit,
    onStrokeOpacityChange: (Float) -> Unit,
    onNeonEnabledChange: (Boolean) -> Unit,
    onNeonColorChange: (Int) -> Unit,
    onOpenNeonColorPicker: () -> Unit,
    onNeonRadiusChange: (Float) -> Unit,
    onNeonIntensityChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 460
) {
    var materialState by remember(material) { mutableStateOf(material) }
    var diameterState by remember(diameter) { mutableStateOf(diameter) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var lightAngleState by remember(lightAngle) { mutableStateOf(lightAngle) }
    var lightDistanceState by remember(lightDistance) { mutableStateOf(lightDistance) }
    var lightIntensityState by remember(lightIntensity) { mutableStateOf(lightIntensity) }
    var opacityState by remember(opacityPct) { mutableStateOf(opacityPct) }
    var floorShadowEnabledState by remember(floorShadowEnabled) { mutableStateOf(floorShadowEnabled) }
    var floorElevationState by remember(floorElevation) { mutableStateOf(floorElevation) }
    var floorShadowOpacityState by remember(floorShadowOpacityPct) { mutableStateOf(floorShadowOpacityPct) }
    var strokeWidthState by remember(strokeWidth) { mutableStateOf(strokeWidth) }
    var strokeColorState by remember(strokeColor) { mutableStateOf(strokeColor) }
    var strokeOpacityState by remember(strokeOpacityPct) { mutableStateOf(strokeOpacityPct) }
    var neonEnabledState by remember(neonEnabled) { mutableStateOf(neonEnabled) }
    var neonColorState by remember(neonColor) { mutableStateOf(neonColor) }
    var neonRadiusState by remember(neonRadius) { mutableStateOf(neonRadius) }
    var neonIntensityState by remember(neonIntensity) { mutableStateOf(neonIntensity) }

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // ── 1. Material Selector ────────────────────────────────
                        Text(
                            text = "Material",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                SphereMaterial.GLOSSY to "Glossy",
                                SphereMaterial.MATTE to "Matte",
                                SphereMaterial.METALLIC to "Metal",
                                SphereMaterial.GLASS_CRYSTAL to "Glass",
                                SphereMaterial.NEON_GLOW to "Neon"
                            ).forEach { (m, label) ->
                                val isSelected = materialState == m
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
                                            materialState = m
                                            onMaterialChange(m)
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

                        // ── 2. Base Color ───────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Base Color",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SPHERE_SWATCH_COLORS.forEach { c ->
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
                                        },
                                    contentAlignment = Alignment.Center
                                ) {}
                            }
                            AddColorSwatchButton(
                                onClick = onOpenBaseColorPicker,
                                buttonSize = 22.dp
                            )
                        }

                        // ── 3. Light Source (Touchpad) ──────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Light Source",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LightSourceTouchpad(
                                angle = lightAngleState,
                                distance = lightDistanceState,
                                modifier = Modifier.size(96.dp),
                                onAngleChange = { a ->
                                    lightAngleState = a
                                    onLightAngleChange(a)
                                },
                                onDistanceChange = { d ->
                                    lightDistanceState = d
                                    onLightDistanceChange(d)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${lightAngleState.toInt()}° • ${(lightDistanceState * 100).toInt()}%",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontSize = 10.sp
                            )
                        }
                        LabeledSliderRow(
                            label = "Light Intensity",
                            value = lightIntensityState,
                            onValueChange = { v ->
                                lightIntensityState = v
                                onLightIntensityChange(v)
                            },
                            valueRange = 0.5f..2f,
                            suffix = "x"
                        )

                        // ── 4. Size & Opacity ───────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Size & Opacity",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LabeledSliderRow(
                            label = "Radius",
                            value = diameterState,
                            onValueChange = { v ->
                                diameterState = v
                                onRadiusChange(v)
                            },
                            valueRange = 20f..400f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Opacity",
                            value = opacityState,
                            onValueChange = { v ->
                                opacityState = v
                                onOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 5. Floor Shadow ─────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Floor Shadow",
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
                            label = "Elevation",
                            value = floorElevationState,
                            onValueChange = { v ->
                                floorElevationState = v
                                onFloorElevationChange(v)
                            },
                            valueRange = 0f..200f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Shadow Opacity",
                            value = floorShadowOpacityState,
                            onValueChange = { v ->
                                floorShadowOpacityState = v
                                onFloorShadowOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 6. Outline & Glow ───────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Outline & Glow",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LabeledSliderRow(
                            label = "Stroke Width",
                            value = strokeWidthState,
                            onValueChange = { v ->
                                strokeWidthState = v
                                onStrokeWidthChange(v)
                            },
                            valueRange = 0f..30f,
                            suffix = " px"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(strokeColorState))
                                    .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                                    .clickable(onClick = onOpenStrokeColorPicker)
                            ) {}
                            LabeledSliderRow(
                                label = "Stroke Opacity",
                                value = strokeOpacityState,
                                onValueChange = { v ->
                                    strokeOpacityState = v
                                    onStrokeOpacityChange(v)
                                },
                                valueRange = 0f..100f,
                                suffix = "%",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Neon Glow",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = neonEnabledState,
                                onCheckedChange = {
                                    neonEnabledState = it
                                    onNeonEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (neonEnabledState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(neonColorState))
                                        .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                                        .clickable(onClick = onOpenNeonColorPicker)
                                ) {}
                                LabeledSliderRow(
                                    label = "Glow Radius",
                                    value = neonRadiusState,
                                    onValueChange = { v ->
                                        neonRadiusState = v
                                        onNeonRadiusChange(v)
                                    },
                                    valueRange = 2f..40f,
                                    suffix = " px",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            LabeledSliderRow(
                                label = "Glow Intensity",
                                value = neonIntensityState,
                                onValueChange = { v ->
                                    neonIntensityState = v
                                    onNeonIntensityChange(v)
                                },
                                valueRange = 0.1f..2f,
                                suffix = "x"
                            )
                        }

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

/**
 * Widget sentuhan 2D untuk mengarahkan sumber cahaya bola: sentuh/tarik sembarang
 * titik pada piringan, titik kilau di bola langsung mengikuti. Posisi handle:
 * dx = sin(θ)·k, dy = −cos(θ)·k (sinkron dengan render [Sphere3DLayer]).
 */
@Composable
private fun LightSourceTouchpad(
    angle: Float,
    distance: Float,
    modifier: Modifier = Modifier,
    onAngleChange: (Float) -> Unit,
    onDistanceChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .background(Color(0xFFF1F5F9), CircleShape)
            .border(1.dp, Color(0xFFCBD5E1), CircleShape)
            .pointerInput(Unit) {
                val armPx = minOf(size.width, size.height).toFloat() * 0.40f
                fun apply(pos: androidx.compose.ui.geometry.Offset) {
                    val rx = pos.x - size.width / 2f
                    val ry = pos.y - size.height / 2f
                    val dist = kotlin.math.sqrt(rx * rx + ry * ry)
                    val fraction = (dist / armPx).coerceIn(0f, 1f)
                    onDistanceChange(fraction * 0.8f)
                    val deg = Math.toDegrees(atan2(rx.toDouble(), (-ry).toDouble())).toFloat()
                    onAngleChange(deg.coerceIn(-180f, 180f))
                }
                detectDragGestures(
                    onDragStart = { off -> apply(off) },
                    onDrag = { change, _ ->
                        apply(change.position)
                    },
                    onDragEnd = {},
                    onDragCancel = {}
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .border(1.dp, Color(0xFFD6DEE8), CircleShape)
        )
        val armFrac = (distance.coerceIn(0f, 0.8f) / 0.8f).coerceIn(0f, 1f)
        val handlePx = with(density) { 96.dp.toPx() * 0.40f * armFrac }
        val rad = Math.toRadians(angle.toDouble())
        val dx = (sin(rad) * handlePx).toInt()
        val dy = (-cos(rad) * handlePx).toInt()
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(dx - 9.dp.toPx().toInt(), dy - 9.dp.toPx().toInt())
                }
                .size(18.dp)
                .clip(CircleShape)
                .background(PrimaryBlue)
                .border(2.dp, Color.White, CircleShape)
        )
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
            modifier = Modifier.width(92.dp),
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