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
import com.flyerpix.editor.canvas.model.ButtonIcon
import com.flyerpix.editor.canvas.model.CapsuleMaterial
import com.flyerpix.editor.canvas.model.CapsuleMode
import com.flyerpix.editor.canvas.model.IconPosition

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val PRIMARY_COLORS = listOf(
    0xFFE53935.toInt(),
    0xFF1769FF.toInt(),
    0xFFFFB300.toInt(),
    0xFF2E7D32.toInt(),
    0xFF8E24AA.toInt(),
    0xFF00E5FF.toInt(),
    0xFF1A1A2E.toInt(),
    0xFFFFFFFF.toInt()
)

private val SECONDARY_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFFFFB300.toInt(),
    0xFFE53935.toInt(),
    0xFF1769FF.toInt(),
    0xFF2E7D32.toInt(),
    0xFF8E24AA.toInt(),
    0xFF1A1A2E.toInt(),
    0xFF00E5FF.toInt()
)

private val TEXT_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF1A1A2E.toInt(),
    0xFFFFD700.toInt(),
    0xFF00E5FF.toInt(),
    0xFFC62828.toInt()
)

/** Preset cepat kapsul — satu sumber kebenaran untuk kartu preset panel & controller. */
data class CapsuleQuickPreset(
    val mode: CapsuleMode,
    val primaryColor: Int,
    val secondaryColor: Int,
    val splitRatio: Float,
    val buttonText: String,
    val textColor: Int,
    val isTextBold: Boolean,
    val iconType: ButtonIcon,
    val iconPosition: IconPosition,
    val materialType: CapsuleMaterial,
    val specularIntensity: Float,
    val neonEnabled: Boolean,
    val neonColor: Int
)

val CapsulePresets: Map<String, CapsuleQuickPreset> = listOf(
    "cta_sale" to CapsuleQuickPreset(
        CapsuleMode.CTA_BUTTON, 0xFFE53935.toInt(), 0xFFFFB300.toInt(), 0.5f,
        "BELI SEKARANG", 0xFFFFFFFF.toInt(), true, ButtonIcon.CART,
        IconPosition.LEFT_OF_TEXT, CapsuleMaterial.GLOSSY, 0.85f, false, 0xFF00E5FF.toInt()
    ),
    "medis" to CapsuleQuickPreset(
        CapsuleMode.TWO_TONE_PILL, 0xFFE53935.toInt(), 0xFFFFB300.toInt(), 0.5f,
        "OBAT", 0xFFFFFFFF.toInt(), true, ButtonIcon.NONE,
        IconPosition.LEFT_OF_TEXT, CapsuleMaterial.MATTE, 0.4f, false, 0xFF00E5FF.toInt()
    ),
    "cta_diskon" to CapsuleQuickPreset(
        CapsuleMode.CTA_BUTTON, 0xFFFFB300.toInt(), 0xFFE53935.toInt(), 0.5f,
        "DISKON 70%", 0xFF1A1A2E.toInt(), true, ButtonIcon.FLASH,
        IconPosition.RIGHT_OF_TEXT, CapsuleMaterial.GLOSSY, 0.9f, false, 0xFF00E5FF.toInt()
    ),
    "vip_gold" to CapsuleQuickPreset(
        CapsuleMode.CTA_BUTTON, 0xFFB8860B.toInt(), 0xFF000000.toInt(), 0.5f,
        "VIP", 0xFFFFFFFF.toInt(), true, ButtonIcon.STAR,
        IconPosition.LEFT_OF_TEXT, CapsuleMaterial.METALLIC, 1.1f, false, 0xFFFFD700.toInt()
    ),
    "cyber_neon" to CapsuleQuickPreset(
        CapsuleMode.CTA_BUTTON, 0xFF00E5FF.toInt(), 0xFF1769FF.toInt(), 0.5f,
        "GO MODERN", 0xFF1A1A2E.toInt(), true, ButtonIcon.ARROW_RIGHT,
        IconPosition.RIGHT_OF_TEXT, CapsuleMaterial.CYBER_NEON, 1.0f, true, 0xFF00E5FF.toInt()
    )
).associate { it.first to it.second }

/**
 * Compose bottom sheet untuk studio 3D Capsule / Kapsul Promo.
 *
 * Urutan kontrol: Preset Cepat ➔ Mode & Teks CTA ➔ Material Capsule ➔
 * Dimensi & Sudut 3D ➔ Pencahayaan & Bayangan ➔ Finishing Effects.
 */
@Composable
fun Capsule3DDetailPage(
    capsuleLength: Float,
    capsuleRadius: Float,
    rotationAngle: Float,
    mode: CapsuleMode,
    material: CapsuleMaterial,
    primaryColor: Int,
    secondaryColor: Int,
    splitRatio: Float,
    buttonText: String,
    textColor: Int,
    textSize: Float,
    isTextBold: Boolean,
    iconType: ButtonIcon,
    iconPosition: IconPosition,
    specularIntensity: Float,
    floatingElevation: Float,
    floorShadowEnabled: Boolean,
    floorShadowOpacityPct: Float,
    opacityPct: Float,
    onPresetClick: (String) -> Unit,
    onModeChange: (CapsuleMode) -> Unit,
    onPrimaryColorChange: (Int) -> Unit,
    onOpenPrimaryColorPicker: () -> Unit,
    onSecondaryColorChange: (Int) -> Unit,
    onOpenSecondaryColorPicker: () -> Unit,
    onSplitRatioChange: (Float) -> Unit,
    onButtonTextChange: (String) -> Unit,
    onTextColorChange: (Int) -> Unit,
    onOpenTextColorPicker: () -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onTextBoldChange: (Boolean) -> Unit,
    onIconTypeChange: (ButtonIcon) -> Unit,
    onIconPositionChange: (IconPosition) -> Unit,
    onMaterialChange: (CapsuleMaterial) -> Unit,
    onSpecularIntensityChange: (Float) -> Unit,
    onLengthChange: (Float) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onFloatingElevationChange: (Float) -> Unit,
    onFloorShadowEnabledChange: (Boolean) -> Unit,
    onFloorShadowOpacityChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 480
) {
    var lengthState by remember(capsuleLength) { mutableStateOf(capsuleLength) }
    var radiusState by remember(capsuleRadius) { mutableStateOf(capsuleRadius) }
    var rotationState by remember(rotationAngle) { mutableStateOf(rotationAngle) }
    var modeState by remember(mode) { mutableStateOf(mode) }
    var materialState by remember(material) { mutableStateOf(material) }
    var primaryColorState by remember(primaryColor) { mutableStateOf(primaryColor) }
    var secondaryColorState by remember(secondaryColor) { mutableStateOf(secondaryColor) }
    var splitRatioState by remember(splitRatio) { mutableStateOf(splitRatio) }
    var buttonTextState by remember(buttonText) { mutableStateOf(buttonText) }
    var textColorState by remember(textColor) { mutableStateOf(textColor) }
    var textSizeState by remember(textSize) { mutableStateOf(textSize) }
    var textBoldState by remember(isTextBold) { mutableStateOf(isTextBold) }
    var iconTypeState by remember(iconType) { mutableStateOf(iconType) }
    var iconPositionState by remember(iconPosition) { mutableStateOf(iconPosition) }
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

                        // ── 1. Preset Kapsul Cepat ──────────────────────────
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
                                "cta_sale" to "CTA Sale",
                                "medis" to "Medis",
                                "cta_diskon" to "Diskon",
                                "vip_gold" to "VIP Gold",
                                "cyber_neon" to "Cyber"
                            ).forEach { (key, label) ->
                                val preset = CapsulePresets.getValue(key)
                                val isActive = materialState == preset.materialType &&
                                    primaryColorState == preset.primaryColor &&
                                    buttonTextState == preset.buttonText
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
                                            modeState = preset.mode
                                            materialState = preset.materialType
                                            primaryColorState = preset.primaryColor
                                            secondaryColorState = preset.secondaryColor
                                            splitRatioState = preset.splitRatio
                                            buttonTextState = preset.buttonText
                                            textColorState = preset.textColor
                                            textBoldState = preset.isTextBold
                                            iconTypeState = preset.iconType
                                            iconPositionState = preset.iconPosition
                                            specularState = preset.specularIntensity
                                            onPresetClick(key)
                                        }
                                        .padding(vertical = 8.dp),
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

                        // ── 2. Mode & Teks CTA ──────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Mode Kapsul",
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
                                CapsuleMode.CTA_BUTTON to "Tombol CTA",
                                CapsuleMode.TWO_TONE_PILL to "Pil 2 Warna"
                            ).forEach { (m, label) ->
                                val isSelected = modeState == m
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
                                            modeState = m
                                            onModeChange(m)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        if (modeState == CapsuleMode.CTA_BUTTON) {
                            Text(
                                text = "Teks Tombol",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            OutlinedTextField(
                                value = buttonTextState,
                                onValueChange = { t ->
                                    buttonTextState = t
                                    onButtonTextChange(t)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                textStyle = MaterialTheme.typography.body2.copy(fontSize = 12.sp),
                                singleLine = true,
                                placeholder = {
                                    Text("Teks bebas mis. BELI SEKARANG, GO MODERN", fontSize = 11.sp)
                                }
                            )
                            Text(
                                text = "Ikon",
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
                                    ButtonIcon.NONE to "Tanpa",
                                    ButtonIcon.CART to "Cart",
                                    ButtonIcon.ARROW_RIGHT to "\u2192",
                                    ButtonIcon.FLASH to "\u26A1",
                                    ButtonIcon.CHECK to "\u2713",
                                    ButtonIcon.STAR to "\u2605"
                                ).forEach { (ic, label) ->
                                    val isSelected = iconTypeState == ic
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
                                                iconTypeState = ic
                                                onIconTypeChange(ic)
                                            }
                                            .padding(vertical = 7.dp),
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
                                    IconPosition.LEFT_OF_TEXT to "Ikon di Kiri",
                                    IconPosition.RIGHT_OF_TEXT to "Ikon di Kanan"
                                ).forEach { (ip, label) ->
                                    val isSelected = iconPositionState == ip
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
                                                iconPositionState = ip
                                                onIconPositionChange(ip)
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.caption,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryBlue else Color(0xFF1A1A2E),
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TEXT_COLORS.forEach { c ->
                                    val isPicked = textColorState == c
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
                                                textColorState = c
                                                onTextColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenTextColorPicker,
                                    buttonSize = 22.dp
                                )
                                Text(
                                    text = "Warna teks",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontSize = 10.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Teks Tebal",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(TextSecondary),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                )
                                Switch(
                                    checked = textBoldState,
                                    onCheckedChange = {
                                        textBoldState = it
                                        onTextBoldChange(it)
                                    },
                                    modifier = Modifier.scale(0.8f)
                                )
                            }
                            LabeledSliderRow(
                                label = "Ukuran Teks",
                                value = textSizeState,
                                onValueChange = { v ->
                                    textSizeState = v
                                    onTextSizeChange(v)
                                },
                                valueRange = 8f..28f,
                                suffix = " sp"
                            )
                        }

                        if (modeState == CapsuleMode.TWO_TONE_PILL) {
                            Text(
                                text = "Warna Sisi Kiri",
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
                                PRIMARY_COLORS.forEach { c ->
                                    val isPicked = primaryColorState == c
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
                                                primaryColorState = c
                                                onPrimaryColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenPrimaryColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            Text(
                                text = "Warna Sisi Kanan",
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
                                SECONDARY_COLORS.forEach { c ->
                                    val isPicked = secondaryColorState == c
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
                                                secondaryColorState = c
                                                onSecondaryColorChange(c)
                                            }
                                    ) {}
                                }
                                AddColorSwatchButton(
                                    onClick = onOpenSecondaryColorPicker,
                                    buttonSize = 22.dp
                                )
                            }
                            LabeledSliderRow(
                                label = "Posisi Pemisah",
                                value = splitRatioState,
                                onValueChange = { v ->
                                    splitRatioState = v
                                    onSplitRatioChange(v)
                                },
                                valueRange = 0.2f..0.8f,
                                suffix = ""
                            )
                        }

                        // ── 3. Material Capsule ─────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Material Capsule",
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
                                CapsuleMaterial.GLOSSY to "Glossy",
                                CapsuleMaterial.MATTE to "Matte",
                                CapsuleMaterial.METALLIC to "Metalik",
                                CapsuleMaterial.CYBER_NEON to "Neon"
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
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
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

                        // ── 4. Dimensi & Sudut 3D ───────────────────────────
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
                            label = "Panjang",
                            value = lengthState,
                            onValueChange = { v ->
                                lengthState = v
                                onLengthChange(v)
                            },
                            valueRange = 60f..500f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Ketebalan",
                            value = radiusState,
                            onValueChange = { v ->
                                radiusState = v
                                onRadiusChange(v)
                            },
                            valueRange = 12f..80f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Kemiringan",
                            value = rotationState,
                            onValueChange = { v ->
                                rotationState = v
                                onRotationChange(((v % 360f) + 360f) % 360f)
                            },
                            valueRange = 0f..360f,
                            suffix = "°"
                        )

                        // ── 5. Pencahayaan & Bayangan ───────────────────────
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
                            label = "Opacity Kapsul",
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
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Neon Glow", fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = "Tip: Drop Shadow membuat kapsul melayang realistis • Neon Glow membuat kapsul holografis berpendar.",
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