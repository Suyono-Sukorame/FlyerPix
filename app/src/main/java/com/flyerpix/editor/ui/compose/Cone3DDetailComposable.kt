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
import com.flyerpix.editor.canvas.model.ConeMaterial
import com.flyerpix.editor.canvas.model.ConeStripeMode
import com.flyerpix.editor.canvas.model.ConeStyle

private val PrimaryBlue = Color(0xFF1769FF)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val BASE_COLORS = listOf(
    0xFFFF7F1A.toInt(),
    0xFFF3F4F6.toInt(),
    0xFF1769FF.toInt(),
    0xFFFFD700.toInt(),
    0xFFE53935.toInt(),
    0xFF00E5FF.toInt(),
    0xFF2E7D32.toInt(),
    0xFF1A1A2E.toInt()
)

private val STRIPE_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFC93C.toInt(),
    0xFF1A1A2E.toInt(),
    0xFFE53935.toInt(),
    0xFF00E5FF.toInt(),
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

/** Quick 3D cone presets — single source of truth for the panel preset cards & controller. */
data class ConeQuickPreset(
    val style: ConeStyle,
    val materialType: ConeMaterial,
    val baseColor: Int,
    val autoShade: Boolean,
    val specularIntensity: Float,
    val stripeMode: ConeStripeMode,
    val stripeColor: Int,
    val stripeWidth: Float,
    val tiltAngle: Float,
    val floatingElevation: Float,
    val floorShadowOpacityPct: Float,
    val neonEnabled: Boolean,
    val neonColor: Int
)

val ConePresets: Map<String, ConeQuickPreset> = listOf(
    "minimal" to ConeQuickPreset(
        ConeStyle.MINIMAL, ConeMaterial.MATTE, 0xFFF3F4F6.toInt(),
        true, 0.60f, ConeStripeMode.NONE, 0xFFFFFFFF.toInt(), 36f,
        18f, 14f, 45f, false, 0xFF00E5FF.toInt()
    ),
    "traffic_cone" to ConeQuickPreset(
        ConeStyle.TRAFFIC_CONE, ConeMaterial.MATTE, 0xFFFF7F1A.toInt(),
        true, 0.80f, ConeStripeMode.TWO, 0xFFFFFFFF.toInt(), 44f,
        14f, 16f, 50f, false, 0xFF00E5FF.toInt()
    ),
    "party_hat" to ConeQuickPreset(
        ConeStyle.PARTY_HAT, ConeMaterial.GLOSSY, 0xFF1769FF.toInt(),
        true, 0.90f, ConeStripeMode.TWO, 0xFFFFC93C.toInt(), 38f,
        20f, 10f, 40f, false, 0xFFFFC93C.toInt()
    ),
    "gold_peak" to ConeQuickPreset(
        ConeStyle.GOLD_PEAK, ConeMaterial.METALLIC_GOLD, 0xFFFFD700.toInt(),
        true, 1.10f, ConeStripeMode.NONE, 0xFFFFFFFF.toInt(), 36f,
        16f, 20f, 45f, false, 0xFFFFC93C.toInt()
    ),
    "cyber_neon" to ConeQuickPreset(
        ConeStyle.CYBER_NEON, ConeMaterial.NEON, 0xFF00E5FF.toInt(),
        true, 1.00f, ConeStripeMode.ONE, 0xFFFFFFFF.toInt(), 34f,
        22f, 26f, 55f, true, 0xFF00E5FF.toInt()
    ),
    "pine_tree" to ConeQuickPreset(
        ConeStyle.SITH_PYRAMID, ConeMaterial.MATTE, 0xFF2E7D32.toInt(),
        true, 0.70f, ConeStripeMode.NONE, 0xFFFFFFFF.toInt(), 40f,
        12f, 8f, 40f, false, 0xFF00E5FF.toInt()
    )
).associate { it.first to it.second }

/**
 * Compose bottom sheet for the 3D Cone (Koni 3D) studio.
 *
 * Control order: Presets ➔ Style ➔ Dimensions & Tilt ➔ Color & Shading ➔
 * Stripes & Bands ➔ Wireframe ➔ Floor Shadow ➔ Effects.
 */
@Composable
fun Cone3DDetailPage(
    style: ConeStyle,
    materialType: ConeMaterial,
    baseColor: Int,
    radiusX: Float,
    coneHeight: Float,
    tiltAngle: Float,
    flipApex: Boolean,
    autoShade: Boolean,
    specularIntensity: Float,
    stripeMode: ConeStripeMode,
    stripeColor: Int,
    stripeWidth: Float,
    wireframeEnabled: Boolean,
    wireStrokeWidth: Float,
    wireColor: Int,
    wireStrokeOpacityPct: Float,
    floorShadowEnabled: Boolean,
    floatingElevation: Float,
    floorShadowOpacityPct: Float,
    opacityPct: Float,
    onPresetClick: (String) -> Unit,
    onStyleChange: (ConeStyle) -> Unit,
    onMaterialChange: (ConeMaterial) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onRadiusChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onTiltChange: (Float) -> Unit,
    onFlipApexChange: (Boolean) -> Unit,
    onAutoShadeChange: (Boolean) -> Unit,
    onSpecularChange: (Float) -> Unit,
    onStripeModeChange: (ConeStripeMode) -> Unit,
    onStripeColorChange: (Int) -> Unit,
    onOpenStripeColorPicker: () -> Unit,
    onStripeWidthChange: (Float) -> Unit,
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
    var styleState by remember(style) { mutableStateOf(style) }
    var materialState by remember(materialType) { mutableStateOf(materialType) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var radiusState by remember(radiusX) { mutableStateOf(radiusX) }
    var heightState by remember(coneHeight) { mutableStateOf(coneHeight) }
    var tiltState by remember(tiltAngle) { mutableStateOf(tiltAngle) }
    var flipState by remember(flipApex) { mutableStateOf(flipApex) }
    var autoShadeState by remember(autoShade) { mutableStateOf(autoShade) }
    var specularState by remember(specularIntensity) { mutableStateOf(specularIntensity) }
    var stripeModeState by remember(stripeMode) { mutableStateOf(stripeMode) }
    var stripeColorState by remember(stripeColor) { mutableStateOf(stripeColor) }
    var stripeWidthState by remember(stripeWidth) { mutableStateOf(stripeWidth) }
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
                                "minimal" to "Minimal",
                                "traffic_cone" to "Traffic",
                                "party_hat" to "Party"
                            ).forEach { (key, label) ->
                                val preset = ConePresets.getValue(key)
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
                                            autoShadeState = preset.autoShade
                                            specularState = preset.specularIntensity
                                            stripeModeState = preset.stripeMode
                                            stripeColorState = preset.stripeColor
                                            stripeWidthState = preset.stripeWidth
                                            tiltState = preset.tiltAngle
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                "gold_peak" to "Gold",
                                "cyber_neon" to "Cyber",
                                "pine_tree" to "Pine"
                            ).forEach { (key, label) ->
                                val preset = ConePresets.getValue(key)
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
                                            autoShadeState = preset.autoShade
                                            specularState = preset.specularIntensity
                                            stripeModeState = preset.stripeMode
                                            stripeColorState = preset.stripeColor
                                            stripeWidthState = preset.stripeWidth
                                            tiltState = preset.tiltAngle
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

                        // ── 2. Style ─────────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Style",
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
                                ConeStyle.MINIMAL to "Minimal",
                                ConeStyle.TRAFFIC_CONE to "Traffic",
                                ConeStyle.PARTY_HAT to "Party",
                                ConeStyle.GOLD_PEAK to "Gold Peak"
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                ConeStyle.SITH_PYRAMID to "Pyramid",
                                ConeStyle.CYBER_NEON to "Cyber Neon"
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

                        // ── 3. Dimensions & Tilt ─────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Dimensions & Tilt",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        LabeledSliderRow(
                            label = "Radius",
                            value = radiusState,
                            onValueChange = { v ->
                                radiusState = v
                                onRadiusChange(v)
                            },
                            valueRange = 40f..300f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Height",
                            value = heightState,
                            onValueChange = { v ->
                                heightState = v
                                onHeightChange(v)
                            },
                            valueRange = 40f..600f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Tilt Angle",
                            value = tiltState,
                            onValueChange = { v ->
                                tiltState = v
                                onTiltChange(v)
                            },
                            valueRange = 0f..78f,
                            suffix = "°"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Flip Apex",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = flipState,
                                onCheckedChange = {
                                    flipState = it
                                    onFlipApexChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // ── 4. Color & Shading ───────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Color & Shading",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf(
                                ConeMaterial.MATTE to "Matte",
                                ConeMaterial.GLOSSY to "Glossy",
                                ConeMaterial.METALLIC_GOLD to "Metallic",
                                ConeMaterial.NEON to "Neon"
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto Shade",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = autoShadeState,
                                onCheckedChange = {
                                    autoShadeState = it
                                    onAutoShadeChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        LabeledSliderRow(
                            label = "Specular",
                            value = specularState,
                            onValueChange = { v ->
                                specularState = v
                                onSpecularChange(v)
                            },
                            valueRange = 0f..2f,
                            suffix = ""
                        )

                        // ── 5. Stripes & Bands ───────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Stripes & Bands",
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
                                ConeStripeMode.NONE to "None",
                                ConeStripeMode.ONE to "1 Band",
                                ConeStripeMode.TWO to "2 Bands"
                            ).forEach { (m, label) ->
                                val isSelected = stripeModeState == m
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
                                            stripeModeState = m
                                            onStripeModeChange(m)
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
                        if (stripeModeState != ConeStripeMode.NONE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                STRIPE_COLORS.forEach { c ->
                                    val isPicked = stripeColorState == c
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
                                                stripeColorState = c
                                                onStripeColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenStripeColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            LabeledSliderRow(
                                label = "Stripe Width",
                                value = stripeWidthState,
                                onValueChange = { v ->
                                    stripeWidthState = v
                                    onStripeWidthChange(v)
                                },
                                valueRange = 12f..120f,
                                suffix = " px"
                            )
                        }

                        // ── 6. Wireframe ─────────────────────────────────────
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
                                valueRange = 0.5f..12f,
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

                        // ── 7. Floor Shadow ──────────────────────────────────
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

                        // ── 8. Effects ─────────────────────────────────────
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
                            text = "Tip: Drop Shadow grounds the cone realistically • Neon Glow turns the cone into a sci-fi beacon.",
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