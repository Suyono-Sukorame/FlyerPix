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
import com.flyerpix.editor.canvas.model.TorusMaterial
import com.flyerpix.editor.canvas.model.TorusStyle

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val BASE_COLORS = listOf(
    0xFFFFD700.toInt(),
    0xFFEDEFF5.toInt(),
    0xFFE8A95B.toInt(),
    0xFF00E5FF.toInt(),
    0xFF1769FF.toInt(),
    0xFFE53935.toInt(),
    0xFF1A1A2E.toInt(),
    0xFFFFFFFF.toInt()
)

private val ICING_COLORS = listOf(
    0xFFFF69B4.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF5D3A1A.toInt(),
    0xFFB71C1C.toInt(),
    0xFF8FD14F.toInt(),
    0xFF64B5F6.toInt()
)

private val GEM_COLORS = listOf(
    0xFFD6EFFF.toInt(),
    0xFF00E5FF.toInt(),
    0xFFE91E63.toInt(),
    0xFF2196F3.toInt(),
    0xFF00C853.toInt(),
    0xFFFFD700.toInt()
)

/** Preset cepat donat/cincin 3D — satu sumber kebenaran untuk kartu preset panel & controller. */
data class TorusQuickPreset(
    val style: TorusStyle,
    val materialType: TorusMaterial,
    val baseColor: Int,
    val icingEnabled: Boolean,
    val icingColor: Int,
    val sprinklesEnabled: Boolean,
    val sprinkleDensity: Int,
    val gemEnabled: Boolean,
    val gemColor: Int,
    val gemSize: Float,
    val specularIntensity: Float,
    val tiltAngle: Float,
    val spinAngle: Float,
    val floatingElevation: Float,
    val floorShadowOpacityPct: Float,
    val neonEnabled: Boolean,
    val neonColor: Int
)

val TorusPresets: Map<String, TorusQuickPreset> = listOf(
    "gold_wedding" to TorusQuickPreset(
        TorusStyle.LUXURY_JEWELRY, TorusMaterial.METALLIC_GOLD, 0xFFFFD700.toInt(),
        false, 0xFFFF69B4.toInt(), false, 24,
        true, 0xFFD6EFFF.toInt(), 26f,
        0.95f, 40f, 0f, 24f, 50f, false, 0xFF00E5FF.toInt()
    ),
    "strawberry_donut" to TorusQuickPreset(
        TorusStyle.SWEET_DONUT, TorusMaterial.GLOSSY, 0xFFE8A95B.toInt(),
        true, 0xFFFF6FA5.toInt(), true, 26,
        false, 0xFF00E5FF.toInt(), 26f,
        0.80f, 45f, 0f, 12f, 55f, false, 0xFF00E5FF.toInt()
    ),
    "cyber_portal" to TorusQuickPreset(
        TorusStyle.CYBER_NEON, TorusMaterial.NEON, 0xFF00E5FF.toInt(),
        false, 0xFFFF69B4.toInt(), false, 20,
        false, 0xFF00E5FF.toInt(), 24f,
        1.00f, 28f, 25f, 30f, 60f, true, 0xFF00E5FF.toInt()
    ),
    "silver_chrome" to TorusQuickPreset(
        TorusStyle.LUXURY_JEWELRY, TorusMaterial.CHROME_SILVER, 0xFFEDEFF5.toInt(),
        false, 0xFFFF69B4.toInt(), false, 20,
        false, 0xFF2196F3.toInt(), 26f,
        1.10f, 50f, 0f, 22f, 50f, false, 0xFF00E5FF.toInt()
    ),
    "choco_glaze" to TorusQuickPreset(
        TorusStyle.SWEET_DONUT, TorusMaterial.MATTE, 0xFFD2A05A.toInt(),
        true, 0xFF5D3A1A.toInt(), true, 20,
        false, 0xFFFFD700.toInt(), 26f,
        0.75f, 45f, 0f, 12f, 55f, false, 0xFF00E5FF.toInt()
    )
).associate { it.first to it.second }

/**
 * Compose bottom sheet untuk studio 3D Donat / Cincin (Torus).
 *
 * Urutan kontrol: Preset Cepat ➔ Gaya Tema ➔ Material ➔ Warna & Topping ➔
 * Dimensi & Sudut 3D ➔ Pencahayaan & Bayangan ➔ Finishing Effects.
 */
@Composable
fun Torus3DDetailPage(
    majorRadius: Float,
    tubeThickness: Float,
    tiltAngle: Float,
    spinAngle: Float,
    style: TorusStyle,
    materialType: TorusMaterial,
    baseColor: Int,
    icingEnabled: Boolean,
    icingColor: Int,
    sprinklesEnabled: Boolean,
    sprinkleDensity: Int,
    gemEnabled: Boolean,
    gemColor: Int,
    gemSize: Float,
    specularIntensity: Float,
    floatingElevation: Float,
    floorShadowEnabled: Boolean,
    floorShadowOpacityPct: Float,
    opacityPct: Float,
    onPresetClick: (String) -> Unit,
    onStyleChange: (TorusStyle) -> Unit,
    onMaterialChange: (TorusMaterial) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onIcingEnabledChange: (Boolean) -> Unit,
    onIcingColorChange: (Int) -> Unit,
    onOpenIcingColorPicker: () -> Unit,
    onSprinklesEnabledChange: (Boolean) -> Unit,
    onSprinkleDensityChange: (Int) -> Unit,
    onGemEnabledChange: (Boolean) -> Unit,
    onGemColorChange: (Int) -> Unit,
    onOpenGemColorPicker: () -> Unit,
    onGemSizeChange: (Float) -> Unit,
    onMajorRadiusChange: (Float) -> Unit,
    onTubeThicknessChange: (Float) -> Unit,
    onTiltChange: (Float) -> Unit,
    onSpinChange: (Float) -> Unit,
    onSpecularIntensityChange: (Float) -> Unit,
    onFloatingElevationChange: (Float) -> Unit,
    onFloorShadowEnabledChange: (Boolean) -> Unit,
    onFloorShadowOpacityChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 560
) {
    var majorState by remember(majorRadius) { mutableStateOf(majorRadius) }
    var tubeState by remember(tubeThickness) { mutableStateOf(tubeThickness) }
    var tiltState by remember(tiltAngle) { mutableStateOf(tiltAngle) }
    var spinState by remember(spinAngle) { mutableStateOf(spinAngle) }
    var styleState by remember(style) { mutableStateOf(style) }
    var materialState by remember(materialType) { mutableStateOf(materialType) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var icingEnabledState by remember(icingEnabled) { mutableStateOf(icingEnabled) }
    var icingColorState by remember(icingColor) { mutableStateOf(icingColor) }
    var sprinklesState by remember(sprinklesEnabled) { mutableStateOf(sprinklesEnabled) }
    var sprinkleDensityState by remember(sprinkleDensity) { mutableStateOf(sprinkleDensity) }
    var gemEnabledState by remember(gemEnabled) { mutableStateOf(gemEnabled) }
    var gemColorState by remember(gemColor) { mutableStateOf(gemColor) }
    var gemSizeState by remember(gemSize) { mutableStateOf(gemSize) }
    var specularState by remember(specularIntensity) { mutableStateOf(specularIntensity) }
    var elevationState by remember(floatingElevation) { mutableStateOf(floatingElevation) }
    var floorShadowState by remember(floorShadowEnabled) { mutableStateOf(floorShadowEnabled) }
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

                        // ── 1. Preset Donat / Cincin Cepat ───────────────────
                        Text(
                            text = "Preset Cepat",
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
                                "gold_wedding" to "Cincin Emas",
                                "strawberry_donut" to "Donat Stroberi",
                                "cyber_portal" to "Portal Neon",
                                "silver_chrome" to "Krom Perak",
                                "choco_glaze" to "Donat Cokelat"
                            ).forEach { (key, label) ->
                                val preset = TorusPresets.getValue(key)
                                val isActive = styleState == preset.style &&
                                    materialState == preset.materialType &&
                                    baseColorState == preset.baseColor
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) Color(0xFFFFF3CD) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            styleState = preset.style
                                            materialState = preset.materialType
                                            baseColorState = preset.baseColor
                                            icingEnabledState = preset.icingEnabled
                                            icingColorState = preset.icingColor
                                            sprinklesState = preset.sprinklesEnabled
                                            sprinkleDensityState = preset.sprinkleDensity
                                            gemEnabledState = preset.gemEnabled
                                            gemColorState = preset.gemColor
                                            gemSizeState = preset.gemSize
                                            specularState = preset.specularIntensity
                                            tiltState = preset.tiltAngle
                                            spinState = preset.spinAngle
                                            elevationState = preset.floatingElevation
                                            floorShadowOpacityState = preset.floorShadowOpacityPct
                                            onPresetClick(key)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) Color(0xFFB45309) else Color(0xFF1A1A2E),
                                        fontSize = 8.5.sp
                                    )
                                }
                            }
                        }

                        // ── 2. Gaya Tema ─────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Gaya Tema",
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
                                TorusStyle.MODERN_ABSTRACT to "Modern Abstrak",
                                TorusStyle.LUXURY_JEWELRY to "Perhiasan",
                                TorusStyle.SWEET_DONUT to "Donat Bakery",
                                TorusStyle.CYBER_NEON to "Portal Neon"
                            ).forEach { (m, label) ->
                                val isSelected = styleState == m
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
                                            styleState = m
                                            onStyleChange(m)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        // ── 3. Material ──────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Material Cincin",
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
                                TorusMaterial.GLOSSY to "Glossy",
                                TorusMaterial.MATTE to "Matte",
                                TorusMaterial.METALLIC_GOLD to "Emas",
                                TorusMaterial.CHROME_SILVER to "Krom",
                                TorusMaterial.NEON to "Neon"
                            ).forEach { (m, label) ->
                                val isSelected = materialState == m
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
                                        color = if (isSelected) Color(0xFFB45309) else Color(0xFF1A1A2E),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }

                        // ── 4. Warna Dasar ───────────────────────────────────
                        Text(
                            text = "Warna Dasar",
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
                            BASE_COLORS.forEach { c ->
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

                        // ── 5. Krim & Meses (Donat) ──────────────────────────
                        if (styleState == TorusStyle.SWEET_DONUT) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Krim Leleh & Topping",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Krim Leleh",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                Switch(
                                    checked = icingEnabledState,
                                    onCheckedChange = {
                                        icingEnabledState = it
                                        onIcingEnabledChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            if (icingEnabledState) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ICING_COLORS.forEach { c ->
                                        val isPicked = icingColorState == c
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
                                                    icingColorState = c
                                                    onIcingColorChange(c)
                                                }
                                        ) {}
                                    }
                                    AddColorSwatchButton(
                                        onClick = onOpenIcingColorPicker,
                                        buttonSize = 22.dp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Meses",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                Switch(
                                    checked = sprinklesState,
                                    onCheckedChange = {
                                        sprinklesState = it
                                        onSprinklesEnabledChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            if (sprinklesState) {
                                LabeledSliderRow(
                                    label = "Jumlah Meses",
                                    value = sprinkleDensityState.toFloat(),
                                    onValueChange = { v ->
                                        sprinkleDensityState = v.toInt()
                                        onSprinkleDensityChange(v.toInt())
                                    },
                                    valueRange = 8f..40f,
                                    suffix = ""
                                )
                            }
                        }

                        // ── 6. Batu Permata (Perhiasan) ──────────────────────
                        if (styleState == TorusStyle.LUXURY_JEWELRY) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Batu Permata",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tampilkan Permata",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                Switch(
                                    checked = gemEnabledState,
                                    onCheckedChange = {
                                        gemEnabledState = it
                                        onGemEnabledChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            if (gemEnabledState) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    GEM_COLORS.forEach { c ->
                                        val isPicked = gemColorState == c
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
                                                    gemColorState = c
                                                    onGemColorChange(c)
                                                }
                                        ) {}
                                    }
                                    AddColorSwatchButton(
                                        onClick = onOpenGemColorPicker,
                                        buttonSize = 22.dp
                                    )
                                }
                                LabeledSliderRow(
                                    label = "Ukuran Permata",
                                    value = gemSizeState,
                                    onValueChange = { v ->
                                        gemSizeState = v
                                        onGemSizeChange(v)
                                    },
                                    valueRange = 14f..56f,
                                    suffix = " px"
                                )
                            }
                        }

                        // ── 7. Dimensi & Sudut 3D ────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Dimensi & Sudut 3D",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LabeledSliderRow(
                            label = "Radius Utama",
                            value = majorState,
                            onValueChange = { v ->
                                majorState = v
                                onMajorRadiusChange(v)
                            },
                            valueRange = 40f..300f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Ketebalan Pipa",
                            value = tubeState,
                            onValueChange = { v ->
                                tubeState = v
                                onTubeThicknessChange(v)
                            },
                            valueRange = 8f..150f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Kemiringan",
                            value = tiltState,
                            onValueChange = { v ->
                                tiltState = v
                                onTiltChange(v)
                            },
                            valueRange = 0f..78f,
                            suffix = "°"
                        )
                        LabeledSliderRow(
                            label = "Putar Foto",
                            value = spinState,
                            onValueChange = { v ->
                                spinState = v
                                onSpinChange(((v % 360f) + 360f) % 360f)
                            },
                            valueRange = 0f..360f,
                            suffix = "°"
                        )
                        LabeledSliderRow(
                            label = "Intensitas Kilau",
                            value = specularState,
                            onValueChange = { v ->
                                specularState = v
                                onSpecularIntensityChange(v)
                            },
                            valueRange = 0f..2f,
                            suffix = ""
                        )

                        // ── 8. Pencahayaan & Bayangan ────────────────────────
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
                                checked = floorShadowState,
                                onCheckedChange = {
                                    floorShadowState = it
                                    onFloorShadowEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        LabeledSliderRow(
                            label = "Tinggi Melayang",
                            value = elevationState,
                            onValueChange = { v ->
                                elevationState = v
                                onFloatingElevationChange(v)
                            },
                            valueRange = 0f..80f,
                            suffix = ""
                        )
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
                            label = "Opacity Cincin",
                            value = opacityState,
                            onValueChange = { v ->
                                opacityState = v
                                onOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 9. Finishing Effects ─────────────────────────────
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
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Neon Glow", fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "Tip: Drop Shadow membuat cincin melayang realistis • Neon Glow membuat cincin jadi portal sci-fi berpendar.",
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