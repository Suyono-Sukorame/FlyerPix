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
import com.flyerpix.editor.canvas.model.CrescentMaterial
import com.flyerpix.editor.canvas.model.CrescentStyle
import com.flyerpix.editor.canvas.model.StarType

private val PrimaryBlue = Color(0xFF1769FF)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val BASE_COLORS = listOf(
    0xFFFFD700.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF1769FF.toInt(),
    0xFFE91E63.toInt(),
    0xFF1B7A4B.toInt(),
    0xFF00E5FF.toInt(),
    0xFFE53935.toInt(),
    0xFF1A1A2E.toInt()
)

private val AURA_COLORS = listOf(
    0xFFFFD700.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF00E5FF.toInt(),
    0xFFE91E63.toInt(),
    0xFF4CAF50.toInt(),
    0xFF1769FF.toInt()
)

private val STAR_COLORS = listOf(
    0xFFFFD700.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFC0C0C0.toInt(),
    0xFF00E5FF.toInt(),
    0xFFE53935.toInt(),
    0xFF1769FF.toInt()
)

private val WIRE_COLORS = listOf(
    0xFF1A1A2E.toInt(),
    0xFF000000.toInt(),
    0xFF7A8699.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFE53935.toInt(),
    0xFFFFD700.toInt()
)

/** Quick 3D moon presets — single source of truth for the panel preset cards & controller. */
data class MoonQuickPreset(
    val style: CrescentStyle,
    val materialType: CrescentMaterial,
    val baseColor: Int,
    val innerOffset: Float,
    val extrusionDepth: Float,
    val tiltAngle: Float,
    val starType: StarType,
    val starScale: Float,
    val auraEnabled: Boolean,
    val auraRadius: Float,
    val floorShadowOpacity: Float,
    val neonEnabled: Boolean
)

val MoonPresets: Map<String, MoonQuickPreset> = listOf(
    "luxury_gold" to MoonQuickPreset(
        CrescentStyle.WIDE_CRESCENT, CrescentMaterial.LUXURY_GOLD, 0xFFD4AF37.toInt(),
        0.55f, 28f, 15f, StarType.STAR_8, 0.28f, true, 1.4f, 0.45f, false
    ),
    "rose_gold" to MoonQuickPreset(
        CrescentStyle.WIDE_CRESCENT, CrescentMaterial.ROSE_GOLD, 0xFFE8956D.toInt(),
        0.50f, 24f, 12f, StarType.STAR_8, 0.26f, true, 1.3f, 0.40f, false
    ),
    "silver_chrome" to MoonQuickPreset(
        CrescentStyle.THIN_CRESCENT, CrescentMaterial.CHROME_SILVER, 0xFFEDEFF5.toInt(),
        0.68f, 18f, 18f, StarType.STAR_5, 0.22f, false, 1.2f, 0.35f, false
    ),
    "emerald" to MoonQuickPreset(
        CrescentStyle.FINIAL_SPIRE, CrescentMaterial.EMERALD, 0xFF1B7A4B.toInt(),
        0.48f, 32f, 10f, StarType.STAR_8, 0.30f, true, 1.5f, 0.50f, false
    ),
    "neon_ramadan" to MoonQuickPreset(
        CrescentStyle.FLOATING_ORB, CrescentMaterial.NEON, 0xFF00E5FF.toInt(),
        0.52f, 22f, 20f, StarType.STAR_5, 0.24f, true, 1.6f, 0.55f, true
    )
).associate { it.first to it.second }

/**
 * Compose bottom sheet for the 3D Moon (Gold Crescent) studio.
 *
 * Control order: Presets ➔ Crescent Shape ➔ Orientation ➔ Material & Color ➔
 * Golden Aura ➔ Hanging Star ➔ Wireframe ➔ Floor Shadow ➔ Effects.
 */
@Composable
fun Moon3DDetailPage(
    // Shape
    outerRadius: Float,
    innerOffset: Float,
    extrusionDepth: Float,
    tiltAngle: Float,
    spinAngle: Float,
    // Style & Material
    style: CrescentStyle,
    materialType: CrescentMaterial,
    baseColor: Int,
    // Lighting
    lightAngle: Float,
    specularIntensity: Float,
    auraEnabled: Boolean,
    auraColor: Int,
    auraRadius: Float,
    // Star
    starType: StarType,
    starScale: Float,
    starColor: Int,
    hangingCordEnabled: Boolean,
    // Floor Shadow
    floorShadowEnabled: Boolean,
    floatingElevation: Float,
    floorShadowOpacityPct: Float,
    // Opacity
    opacityPct: Float,
    // Wireframe
    wireframeEnabled: Boolean,
    wireStrokeWidth: Float,
    wireColor: Int,
    wireStrokeOpacityPct: Float,
    // Callbacks
    onPresetClick: (String) -> Unit,
    onStyleChange: (CrescentStyle) -> Unit,
    onMaterialChange: (CrescentMaterial) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onInnerOffsetChange: (Float) -> Unit,
    onExtrusionDepthChange: (Float) -> Unit,
    onTiltChange: (Float) -> Unit,
    onSpinChange: (Float) -> Unit,
    onSpecularChange: (Float) -> Unit,
    onAuraEnabledChange: (Boolean) -> Unit,
    onAuraColorChange: (Int) -> Unit,
    onOpenAuraColorPicker: () -> Unit,
    onAuraRadiusChange: (Float) -> Unit,
    onStarTypeChange: (StarType) -> Unit,
    onStarScaleChange: (Float) -> Unit,
    onStarColorChange: (Int) -> Unit,
    onOpenStarColorPicker: () -> Unit,
    onHangingCordChange: (Boolean) -> Unit,
    onWireframeEnabledChange: (Boolean) -> Unit,
    onWireStrokeWidthChange: (Float) -> Unit,
    onWireColorChange: (Int) -> Unit,
    onOpenWireColorPicker: () -> Unit,
    onWireStrokeOpacityChange: (Float) -> Unit,
    onFloorShadowEnabledChange: (Boolean) -> Unit,
    onElevationChange: (Float) -> Unit,
    onFloorShadowOpacityChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onOpenEmbossEditor: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 560
) {
    var innerOffsetState by remember(innerOffset) { mutableStateOf(innerOffset) }
    var extrusionDepthState by remember(extrusionDepth) { mutableStateOf(extrusionDepth) }
    var tiltState by remember(tiltAngle) { mutableStateOf(tiltAngle) }
    var spinState by remember(spinAngle) { mutableStateOf(spinAngle) }
    var styleState by remember(style) { mutableStateOf(style) }
    var materialState by remember(materialType) { mutableStateOf(materialType) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var specularState by remember(specularIntensity) { mutableStateOf(specularIntensity) }
    var auraEnabledState by remember(auraEnabled) { mutableStateOf(auraEnabled) }
    var auraColorState by remember(auraColor) { mutableStateOf(auraColor) }
    var auraRadiusState by remember(auraRadius) { mutableStateOf(auraRadius) }
    var starTypeState by remember(starType) { mutableStateOf(starType) }
    var starScaleState by remember(starScale) { mutableStateOf(starScale) }
    var starColorState by remember(starColor) { mutableStateOf(starColor) }
    var hangingCordState by remember(hangingCordEnabled) { mutableStateOf(hangingCordEnabled) }
    var wireframeState by remember(wireframeEnabled) { mutableStateOf(wireframeEnabled) }
    var wireStrokeWidthState by remember(wireStrokeWidth) { mutableStateOf(wireStrokeWidth) }
    var wireColorState by remember(wireColor) { mutableStateOf(wireColor) }
    var wireOpacityState by remember(wireStrokeOpacityPct) { mutableStateOf(wireStrokeOpacityPct) }
    var floorShadowState by remember(floorShadowEnabled) { mutableStateOf(floorShadowEnabled) }
    var elevationState by remember(floatingElevation) { mutableStateOf(floatingElevation) }
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

                        // ── 1. Presets ──────────────────────────────────────────
                        Text(
                            text = "Presets",
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
                                "luxury_gold" to "Luxury Gold",
                                "rose_gold" to "Rose Gold",
                                "silver_chrome" to "Silver"
                            ).forEach { (key, label) ->
                                val preset = MoonPresets.getValue(key)
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
                                            innerOffsetState = preset.innerOffset
                                            extrusionDepthState = preset.extrusionDepth
                                            tiltState = preset.tiltAngle
                                            starTypeState = preset.starType
                                            starScaleState = preset.starScale
                                            auraEnabledState = preset.auraEnabled
                                            auraRadiusState = preset.auraRadius
                                            floorShadowOpacityState = preset.floorShadowOpacity
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                "emerald" to "Emerald",
                                "neon_ramadan" to "Neon Ramadan"
                            ).forEach { (key, label) ->
                                val preset = MoonPresets.getValue(key)
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
                                            innerOffsetState = preset.innerOffset
                                            extrusionDepthState = preset.extrusionDepth
                                            tiltState = preset.tiltAngle
                                            starTypeState = preset.starType
                                            starScaleState = preset.starScale
                                            auraEnabledState = preset.auraEnabled
                                            auraRadiusState = preset.auraRadius
                                            floorShadowOpacityState = preset.floorShadowOpacity
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
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFA0AEC0),
                                    fontSize = 8.5.sp
                                )
                            }
                        }

                        // ── 2. Crescent Shape ─────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Crescent Shape",
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
                                CrescentStyle.THIN_CRESCENT to "Thin",
                                CrescentStyle.WIDE_CRESCENT to "Wide",
                                CrescentStyle.FINIAL_SPIRE to "Finial",
                                CrescentStyle.FLOATING_ORB to "Orb"
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
                        LabeledSliderRow(
                            label = "Crescent Width",
                            value = innerOffsetState,
                            onValueChange = { v ->
                                innerOffsetState = v
                                onInnerOffsetChange(v)
                            },
                            valueRange = 0.3f..0.9f,
                            suffix = ""
                        )
                        LabeledSliderRow(
                            label = "Extrusion Depth",
                            value = extrusionDepthState,
                            onValueChange = { v ->
                                extrusionDepthState = v
                                onExtrusionDepthChange(v)
                            },
                            valueRange = 4f..80f,
                            suffix = " px"
                        )

                        // ── 3. Orientation ────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Orientation",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LabeledSliderRow(
                            label = "Tilt Angle",
                            value = tiltState,
                            onValueChange = { v ->
                                tiltState = v
                                onTiltChange(v)
                            },
                            valueRange = -45f..45f,
                            suffix = "\u00B0"
                        )
                        LabeledSliderRow(
                            label = "Spin Angle",
                            value = spinState,
                            onValueChange = { v ->
                                spinState = v
                                onSpinChange(((v % 360f) + 360f) % 360f)
                            },
                            valueRange = 0f..360f,
                            suffix = "\u00B0"
                        )

                        // ── 4. Material & Color ────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Material & Color",
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
                                CrescentMaterial.LUXURY_GOLD to "Gold",
                                CrescentMaterial.ROSE_GOLD to "Rose Gold",
                                CrescentMaterial.CHROME_SILVER to "Silver",
                                CrescentMaterial.EMERALD to "Emerald",
                                CrescentMaterial.NEON to "Neon"
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
                        LabeledSliderRow(
                            label = "Specular Highlight",
                            value = specularState,
                            onValueChange = { v ->
                                specularState = v
                                onSpecularChange(v)
                            },
                            valueRange = 0f..2f,
                            suffix = ""
                        )

                        // ── 5. Golden Aura ────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Golden Aura",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = auraEnabledState,
                                onCheckedChange = {
                                    auraEnabledState = it
                                    onAuraEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (auraEnabledState) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AURA_COLORS.forEach { c ->
                                    val isPicked = auraColorState == c
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
                                                auraColorState = c
                                                onAuraColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenAuraColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            LabeledSliderRow(
                                label = "Glow Radius",
                                value = auraRadiusState,
                                onValueChange = { v ->
                                    auraRadiusState = v
                                    onAuraRadiusChange(v)
                                },
                                valueRange = 0.5f..3.0f,
                                suffix = "x"
                            )
                        }

                        // ── 6. Hanging Star ───────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Hanging Star",
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
                                StarType.NONE to "None",
                                StarType.STAR_8 to "8-pt",
                                StarType.STAR_5 to "5-pt",
                                StarType.STAR_6 to "6-pt"
                            ).forEach { (m, label) ->
                                val isSelected = starTypeState == m
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
                                            starTypeState = m
                                            onStarTypeChange(m)
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
                        if (starTypeState != StarType.NONE) {
                            LabeledSliderRow(
                                label = "Star Size",
                                value = starScaleState,
                                onValueChange = { v ->
                                    starScaleState = v
                                    onStarScaleChange(v)
                                },
                                valueRange = 0.1f..0.5f,
                                suffix = "x"
                            )
                            Text(
                                text = "Star Color",
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
                                STAR_COLORS.forEach { c ->
                                    val isPicked = starColorState == c
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
                                                starColorState = c
                                                onStarColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenStarColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hanging Cord",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                Switch(
                                    checked = hangingCordState,
                                    onCheckedChange = {
                                        hangingCordState = it
                                        onHangingCordChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                        }

                        // ── 7. Wireframe ─────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Wireframe",
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
                                text = "Show Wire",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = wireframeState,
                                onCheckedChange = {
                                    wireframeState = it
                                    onWireframeEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (wireframeState) {
                            LabeledSliderRow(
                                label = "Stroke Width",
                                value = wireStrokeWidthState,
                                onValueChange = { v ->
                                    wireStrokeWidthState = v
                                    onWireStrokeWidthChange(v)
                                },
                                valueRange = 0.5f..8f,
                                suffix = ""
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WIRE_COLORS.forEach { c ->
                                    val isPicked = wireColorState == c
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
                                                wireColorState = c
                                                onWireColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenWireColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            LabeledSliderRow(
                                label = "Stroke Opacity",
                                value = wireOpacityState,
                                onValueChange = { v ->
                                    wireOpacityState = v
                                    onWireStrokeOpacityChange(v)
                                },
                                valueRange = 0f..100f,
                                suffix = "%"
                            )
                        }

                        // ── 8. Floor Shadow ──────────────────────────────────
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
                                checked = floorShadowState,
                                onCheckedChange = {
                                    floorShadowState = it
                                    onFloorShadowEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        LabeledSliderRow(
                            label = "Elevation",
                            value = elevationState,
                            onValueChange = { v ->
                                elevationState = v
                                onElevationChange(v)
                            },
                            valueRange = 0f..80f,
                            suffix = ""
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

                        // ── 9. Effects ─────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Effects",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenShadowEditor,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Text("Drop Shadow", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Text("Neon Glow", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = onOpenEmbossEditor,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 5.dp)
                            ) {
                                Text("Emboss", fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "Tip: Drop Shadow grounds the moon realistically \u2022 Neon Glow turns the crescent into a sci-fi beacon.",
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
