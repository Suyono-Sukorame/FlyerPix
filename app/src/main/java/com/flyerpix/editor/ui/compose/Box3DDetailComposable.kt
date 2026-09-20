package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.Box3DPerspectiveMode

private val PrimaryBlue = Color(0xFF1769FF)
private val TextSecondaryColor = Color(0xFF5F6B7A)
private val DragHandleColor = Color(0xFFD0D4DE)
private const val PanelDivider = 0xFFE4E8F0
private const val TextSecondary = 0xFF5F6B7A

private val BOX3D_SWATCH_COLORS = listOf(
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
 * Compose bottom sheet untuk detail kontrol objek 3D Box.
 *
 * Urutan kontrol mengikuti alur natural:
 * Mode Proyeksi ➔ Dimensi X/Y/Z ➔ Rotasi Sumbu ➔ Warna & Shading ➔ Rusuk
 * (Wireframe) ➔ Effects.
 */
@Composable
fun Box3DDetailPage(
    mode: Box3DPerspectiveMode,
    widthD: Float,
    heightD: Float,
    depthD: Float,
    angleX: Float,
    angleY: Float,
    baseColor: Int,
    autoShading: Boolean,
    faceOpacity: Float,
    strokeWidth: Float,
    strokeColor: Int,
    strokeOpacity: Float,
    showHiddenEdges: Boolean,
    onModeChange: (Box3DPerspectiveMode) -> Unit,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onDepthChange: (Float) -> Unit,
    onAngleXChange: (Float) -> Unit,
    onAngleYChange: (Float) -> Unit,
    onBaseColorChange: (Int) -> Unit,
    onOpenBaseColorPicker: () -> Unit,
    onAutoShadingChange: (Boolean) -> Unit,
    onFaceOpacityChange: (Float) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onStrokeColorChange: (Int) -> Unit,
    onOpenStrokeColorPicker: () -> Unit,
    onStrokeOpacityChange: (Float) -> Unit,
    onShowHiddenEdgesChange: (Boolean) -> Unit,
    onOpenShadowEditor: () -> Unit,
    onOpenNeonEditor: () -> Unit,
    onOpenEmbossEditor: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 460
) {
    var modeState by remember(mode) { mutableStateOf(mode) }
    var widthState by remember(widthD) { mutableStateOf(widthD) }
    var heightState by remember(heightD) { mutableStateOf(heightD) }
    var depthState by remember(depthD) { mutableStateOf(depthD) }
    var angleXState by remember(angleX) { mutableStateOf(angleX) }
    var angleYState by remember(angleY) { mutableStateOf(angleY) }
    var baseColorState by remember(baseColor) { mutableStateOf(baseColor) }
    var autoShadingState by remember(autoShading) { mutableStateOf(autoShading) }
    var faceOpacityState by remember(faceOpacity) { mutableStateOf(faceOpacity) }
    var strokeWidthState by remember(strokeWidth) { mutableStateOf(strokeWidth) }
    var strokeColorState by remember(strokeColor) { mutableStateOf(strokeColor) }
    var strokeOpacityState by remember(strokeOpacity) { mutableStateOf(strokeOpacity) }
    var showHiddenEdgesState by remember(showHiddenEdges) { mutableStateOf(showHiddenEdges) }

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

                        // ── 1. Projection ────────────────────────────────────────
                        Text(
                            text = "Projection",
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
                                Box3DPerspectiveMode.ISOMETRIC to "Iso",
                                Box3DPerspectiveMode.ONE_POINT to "1-Point",
                                Box3DPerspectiveMode.TWO_POINT to "2-Point",
                                Box3DPerspectiveMode.FREE_VP to "Free"
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
                                        .padding(vertical = 7.dp),
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

                        // ── 2. Dimensions ───────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Dimensions",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box3DControlRow(
                            label = "Width X",
                            value = widthState,
                            onValueChange = { new -> widthState = new; onWidthChange(new) },
                            valueRange = 20f..800f,
                            suffix = " px"
                        )
                        Box3DControlRow(
                            label = "Height Y",
                            value = heightState,
                            onValueChange = { new -> heightState = new; onHeightChange(new) },
                            valueRange = 20f..800f,
                            suffix = " px"
                        )
                        Box3DControlRow(
                            label = "Depth Z",
                            value = depthState,
                            onValueChange = { new -> depthState = new; onDepthChange(new) },
                            valueRange = 20f..800f,
                            suffix = " px"
                        )

                        // ── 3. Rotation ──────────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Rotation",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box3DControlRow(
                            label = "Axis X",
                            value = angleXState,
                            onValueChange = { new -> angleXState = new; onAngleXChange(new) },
                            valueRange = -180f..180f,
                            suffix = "°"
                        )
                        Box3DControlRow(
                            label = "Axis Y",
                            value = angleYState,
                            onValueChange = { new -> angleYState = new; onAngleYChange(new) },
                            valueRange = -180f..180f,
                            suffix = "°"
                        )

                        // ── 4. Color & Shading ───────────────────────────────────
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BOX3D_SWATCH_COLORS.forEach { colorInt ->
                                val isSelected = baseColorState == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            baseColorState = colorInt
                                            onBaseColorChange(colorInt)
                                        }
                                )
                            }
                            if (baseColorState !in BOX3D_SWATCH_COLORS) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(baseColorState))
                                        .border(2.5.dp, PrimaryBlue, CircleShape)
                                )
                            }
                            AddColorSwatchButton(onClick = onOpenBaseColorPicker, buttonSize = 32.dp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Auto Shade",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp
                            )
                            Switch(
                                checked = autoShadingState,
                                onCheckedChange = { autoShadingState = it; onAutoShadingChange(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        Box3DControlRow(
                            label = "Face Opacity",
                            value = faceOpacityState,
                            onValueChange = { new -> faceOpacityState = new; onFaceOpacityChange(new) },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )

                        // ── 5. Wireframe ─────────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Wireframe",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box3DControlRow(
                            label = "Thickness",
                            value = strokeWidthState,
                            onValueChange = { new -> strokeWidthState = new; onStrokeWidthChange(new) },
                            valueRange = 0f..20f,
                            suffix = " px"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BOX3D_SWATCH_COLORS.forEach { colorInt ->
                                val isSelected = strokeColorState == colorInt
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryBlue else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            strokeColorState = colorInt
                                            onStrokeColorChange(colorInt)
                                        }
                                )
                            }
                            if (strokeColorState !in BOX3D_SWATCH_COLORS) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(strokeColorState))
                                        .border(2.5.dp, PrimaryBlue, CircleShape)
                                )
                            }
                            AddColorSwatchButton(onClick = onOpenStrokeColorPicker, buttonSize = 32.dp)
                        }
                        Box3DControlRow(
                            label = "Line Opacity",
                            value = strokeOpacityState,
                            onValueChange = { new -> strokeOpacityState = new; onStrokeOpacityChange(new) },
                            valueRange = 0f..100f,
                            suffix = "%"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hidden Edges",
                                style = MaterialTheme.typography.caption,
                                color = Color(TextSecondary),
                                modifier = Modifier.weight(1f),
                                fontSize = 11.sp
                            )
                            Switch(
                                checked = showHiddenEdgesState,
                                onCheckedChange = { showHiddenEdgesState = it; onShowHiddenEdgesChange(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // ── 6. Effects ─────────────────────────────────────────
                        Divider(color = Color(PanelDivider), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Effects",
                            style = MaterialTheme.typography.caption,
                            color = Color(TextSecondary),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onOpenShadowEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF0F7FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Shadow", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenEmbossEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFF0F7FF),
                                    contentColor = PrimaryBlue
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Emboss", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenNeonEditor,
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFE8F5FF),
                                    contentColor = Color(0xFF00A8FF)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Neon", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
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

@Composable
private fun Box3DControlRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            modifier = Modifier.width(60.dp),
            fontSize = 10.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = 99,
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        )
        Text(
            text = "${value.toInt()}$suffix",
            style = MaterialTheme.typography.caption,
            color = Color(TextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(38.dp),
            fontSize = 10.sp
        )
    }
}