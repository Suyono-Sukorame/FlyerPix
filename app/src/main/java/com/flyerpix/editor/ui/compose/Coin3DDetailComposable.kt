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
import com.flyerpix.editor.canvas.model.CoinContentMode
import com.flyerpix.editor.canvas.model.CoinMaterial
import com.flyerpix.editor.canvas.model.CoinSymbol

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val COIN_SWATCH_COLORS = listOf(
    0xFFFFD700.toInt(),
    0xFFC0C0C0.toInt(),
    0xFFB76E79.toInt(),
    0xFF00E5FF.toInt(),
    0xFF1769FF.toInt(),
    0xFFE53935.toInt(),
    0xFF2E7D32.toInt(),
    0xFF8E24AA.toInt()
)

private val EMBLEM_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF1A1A2E.toInt(),
    0xFFFFD700.toInt(),
    0xFFB8860B.toInt(),
    0xFFC62828.toInt()
)

/** Preset cepat koin — satu sumber kebenaran untuk kartu preset panel & controller. */
data class CoinQuickPreset(
    val symbolType: CoinSymbol,
    val customText: String,
    val materialType: CoinMaterial,
    val baseColor: Int,
    val useCustomColor: Boolean,
    val emblemColor: Int,
    val reededEdgeEnabled: Boolean,
    val sparkleEnabled: Boolean,
    val neonEnabled: Boolean
)

val CoinPresets: Map<String, CoinQuickPreset> = listOf(
    "gold_sale" to CoinQuickPreset(
        CoinSymbol.PERCENT, "50%", CoinMaterial.GOLD, 0xFFFFD700.toInt(), false,
        0xFFFFFFFF.toInt(), true, true, false
    ),
    "gold_star" to CoinQuickPreset(
        CoinSymbol.STAR, "50%", CoinMaterial.GOLD, 0xFFFFD700.toInt(), false,
        0xFFFFFFFF.toInt(), true, true, false
    ),
    "rupiah" to CoinQuickPreset(
        CoinSymbol.RUPIAH, "Rp", CoinMaterial.BRONZE, 0xFFD49354.toInt(), false,
        0xFFFFFFFF.toInt(), true, true, false
    ),
    "silver_dollar" to CoinQuickPreset(
        CoinSymbol.DOLLAR, "$", CoinMaterial.SILVER, 0xFFE7EBF3.toInt(), false,
        0xFF1A1A2E.toInt(), true, true, false
    ),
    "cyber_token" to CoinQuickPreset(
        CoinSymbol.DOLLAR, "$", CoinMaterial.NEON_CYBER, 0xFF00E5FF.toInt(), false,
        0xFFFFFFFF.toInt(), true, true, true
    )
).associate { it.first to it.second }

/**
 * Compose bottom sheet untuk studio Koin 3D Promo.
 *
 * Urutan kontrol: Preset Cepat ➔ Ukiran Tengah ➔ Material & Warna ➔
 * Dimensi & Sudut 3D ➔ Detail Koin Otentik ➔ Finishing Effects.
 */
@Composable
fun Coin3DDetailPage(
    diameter: Float,
    thickness: Float,
    tiltAngle: Float,
    spinAngle: Float,
    contentMode: CoinContentMode,
    symbolType: CoinSymbol,
    customText: String,
    embossDepth: Float,
    emblemColor: Int,
    reededEdgeEnabled: Boolean,
    reedCount: Int,
    materialType: CoinMaterial,
    baseColor: Int,
    useCustomColor: Boolean,
    sparkleEnabled: Boolean,
    sparkleAngle: Float,
    sparkleSize: Float,
    floorShadowEnabled: Boolean,
    floorShadowOpacityPct: Float,
    opacityPct: Float,
    onPresetClick: (String) -> Unit,
    onContentModeChange: (CoinContentMode) -> Unit,
    onSymbolChange: (CoinSymbol) -> Unit,
    onCustomTextChange: (String) -> Unit,
    onEmbossDepthChange: (Float) -> Unit,
    onEmblemColorChange: (Int) -> Unit,
    onOpenEmblemColorPicker: () -> Unit,
    onMaterialChange: (CoinMaterial) -> Unit,
    onCustomColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onDiameterChange: (Float) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onTiltChange: (Float) -> Unit,
    onSpinChange: (Float) -> Unit,
    onReededChange: (Boolean) -> Unit,
    onSparkleEnabledChange: (Boolean) -> Unit,
    onSparkleAngleChange: (Float) -> Unit,
    onSparkleSizeChange: (Float) -> Unit,
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
    var diameterState by remember(diameter) { mutableStateOf(diameter) }
    var thicknessState by remember(thickness) { mutableStateOf(thickness) }
    var tiltState by remember(tiltAngle) { mutableStateOf(tiltAngle) }
    var spinState by remember(spinAngle) { mutableStateOf(spinAngle) }
    var contentModeState by remember(contentMode) { mutableStateOf(contentMode) }
    var symbolState by remember(symbolType) { mutableStateOf(symbolType) }
    var customTextState by remember(customText) { mutableStateOf(customText) }
    var embossState by remember(embossDepth) { mutableStateOf(embossDepth) }
    var emblemColorState by remember(emblemColor) { mutableStateOf(emblemColor) }
    var reededState by remember(reededEdgeEnabled) { mutableStateOf(reededEdgeEnabled) }
    var materialState by remember(materialType) { mutableStateOf(materialType) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var useCustomColorState by remember(useCustomColor) { mutableStateOf(useCustomColor) }
    var sparkleEnabledState by remember(sparkleEnabled) { mutableStateOf(sparkleEnabled) }
    var sparkleAngleState by remember(sparkleAngle) { mutableStateOf(sparkleAngle) }
    var sparkleSizeState by remember(sparkleSize) { mutableStateOf(sparkleSize) }
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

                        // ── 1. Preset Koin Cepat ─────────────────────────────
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
                                "gold_sale" to "Gold 50%",
                                "gold_star" to "Gold Star",
                                "rupiah" to "Rupiah",
                                "silver_dollar" to "Silver \$",
                                "cyber_token" to "Cyber Token"
                            ).forEach { (key, label) ->
                                val preset = CoinPresets.getValue(key)
                                val isActive = materialState == preset.materialType &&
                                    baseColorState == preset.baseColor &&
                                    symbolState == preset.symbolType &&
                                    contentModeState == CoinContentMode.PRESET_SYMBOL
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
                                            contentModeState = CoinContentMode.PRESET_SYMBOL
                                            symbolState = preset.symbolType
                                            customTextState = preset.customText
                                            materialState = preset.materialType
                                            baseColorState = preset.baseColor
                                            useCustomColorState = preset.useCustomColor
                                            emblemColorState = preset.emblemColor
                                            reededState = preset.reededEdgeEnabled
                                            sparkleEnabledState = preset.sparkleEnabled
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

                        // ── 2. Ukiran Tengah ─────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Ukiran Tengah",
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
                                CoinSymbol.PERCENT to "%",
                                CoinSymbol.RUPIAH to "Rp",
                                CoinSymbol.DOLLAR to "$",
                                CoinSymbol.STAR to "★",
                                CoinSymbol.CROWN to "Crown",
                                CoinSymbol.DIAMOND to "Gems"
                            ).forEach { (s, label) ->
                                val isSelected = contentModeState == CoinContentMode.PRESET_SYMBOL && symbolState == s
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
                                            contentModeState = CoinContentMode.PRESET_SYMBOL
                                            onContentModeChange(CoinContentMode.PRESET_SYMBOL)
                                            symbolState = s
                                            onSymbolChange(s)
                                        }
                                        .padding(vertical = 6.dp),
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
                        OutlinedTextField(
                            value = customTextState,
                            onValueChange = { t ->
                                customTextState = t
                                contentModeState = CoinContentMode.CUSTOM_TEXT
                                onContentModeChange(CoinContentMode.CUSTOM_TEXT)
                                onCustomTextChange(t)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            textStyle = MaterialTheme.typography.body2.copy(fontSize = 12.sp),
                            singleLine = true,
                            placeholder = {
                                Text("Teks bebas mis. SALE, VOUCHER, 10K", fontSize = 11.sp)
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            EMBLEM_COLORS.forEach { c ->
                                val isPicked = emblemColorState == c
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
                                            emblemColorState = c
                                            onEmblemColorChange(c)
                                        }
                                ) {}
                            }
                            AddColorSwatchButton(
                                onClick = onOpenEmblemColorPicker,
                                buttonSize = 22.dp
                            )
                            Text(
                                text = "Warna ukiran",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontSize = 10.sp
                            )
                        }
                        LabeledSliderRow(
                            label = "Kedalaman Timbul",
                            value = embossState,
                            onValueChange = { v ->
                                embossState = v
                                onEmbossDepthChange(v)
                            },
                            valueRange = 0f..12f,
                            suffix = ""
                        )

                        // ── 3. Material & Warna Logam ────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Material Logam",
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
                                CoinMaterial.GOLD to "Emas",
                                CoinMaterial.SILVER to "Perak",
                                CoinMaterial.BRONZE to "Perunggu",
                                CoinMaterial.ROSE_GOLD to "Rose",
                                CoinMaterial.NEON_CYBER to "Neon"
                            ).forEach { (m, label) ->
                                val isSelected = materialState == m && !useCustomColorState
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
                                            useCustomColorState = false
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
                        Text(
                            text = "Warna Kustom",
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
                            COIN_SWATCH_COLORS.forEach { c ->
                                val isPicked = baseColorState == c && useCustomColorState
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
                                            useCustomColorState = true
                                            onCustomColorChange(c)
                                        }
                                ) {}
                            }
                            AddColorSwatchButton(
                                onClick = onOpenBaseColorPicker,
                                buttonSize = 22.dp
                            )
                        }

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
                            label = "Ukuran Koin",
                            value = diameterState,
                            onValueChange = { v ->
                                diameterState = v
                                onDiameterChange(v)
                            },
                            valueRange = 40f..480f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Ketebalan Koin",
                            value = thicknessState,
                            onValueChange = { v ->
                                thicknessState = v
                                onThicknessChange(v)
                            },
                            valueRange = 4f..80f,
                            suffix = " px"
                        )
                        LabeledSliderRow(
                            label = "Kemiringan 3D",
                            value = tiltState,
                            onValueChange = { v ->
                                tiltState = v
                                onTiltChange(v)
                            },
                            valueRange = 0f..75f,
                            suffix = "°"
                        )
                        LabeledSliderRow(
                            label = "Putaran Koin",
                            value = ((spinState % 360f) + 360f) % 360f,
                            onValueChange = { v ->
                                spinState = v
                                onSpinChange(((v % 360f) + 360f) % 360f)
                            },
                            valueRange = 0f..360f,
                            suffix = "°"
                        )

                        // ── 5. Detail Koin Otentik ───────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gerigi Tepi Koin (Reeded)",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = reededState,
                                onCheckedChange = {
                                    reededState = it
                                    onReededChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bintang Kilau (Sparkle)",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                            )
                            Switch(
                                checked = sparkleEnabledState,
                                onCheckedChange = {
                                    sparkleEnabledState = it
                                    onSparkleEnabledChange(it)
                                },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                        if (sparkleEnabledState) {
                            LabeledSliderRow(
                                label = "Posisi Kilau",
                                value = sparkleAngleState,
                                onValueChange = { v ->
                                    sparkleAngleState = v
                                    onSparkleAngleChange(v)
                                },
                                valueRange = -180f..180f,
                                suffix = "°"
                            )
                            LabeledSliderRow(
                                label = "Ukuran Kilau",
                                value = sparkleSizeState,
                                onValueChange = { v ->
                                    sparkleSizeState = v
                                    onSparkleSizeChange(v)
                                },
                                valueRange = 6f..60f,
                                suffix = " px"
                            )
                        }

                        // ── 6. Bayangan Lantai ───────────────────────────────
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
                            label = "Opacity Koin",
                            value = opacityState,
                            onValueChange = { v ->
                                opacityState = v
                                onOpacityChange(v)
                            },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 7. Finishing Effects ─────────────────────────────
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
                            text = "Tip: Drop Shadow membuat koin melayang realistis • Neon Glow membuat koin holografis berpendar.",
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