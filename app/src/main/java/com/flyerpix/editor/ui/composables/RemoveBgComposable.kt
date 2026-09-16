package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R

private val PanelColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF1F4FA
private const val PanelHandle = 0xFFD0D4DE

/** Chip ikon (tanpa teks) untuk aksi. Aktif = primary. */
@Composable
private fun RemoveIconChip(
    iconRes: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(32.dp)
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

/** Baris slider: ikon + slider + nilai angka. */
@Composable
private fun RemoveIconSliderRow(
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
        Spacer(modifier = Modifier.width(8.dp))
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
                .height(28.dp)
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

@Composable
fun RemoveBgComposable(
    selectedLayerName: String = "Selected Layer",
    onReset: () -> Unit,
    onApply: (method: String?, featherStrength: Float) -> Unit,
    onPaintMaskDirect: () -> Unit = {},
    onCancel: () -> Unit,
    maxHeightPx: Int = 420,
    sessionKey: Int = 0
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var featherStrength by remember { mutableStateOf(0.5f) }
    val scrollState = remember(sessionKey) { ScrollState(0) }
    val sheetCapDp = with(LocalDensity.current) { maxHeightPx.toDp() }
        .coerceAtMost((LocalConfiguration.current.screenHeightDp * 0.45f).dp)

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

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_layers_24px),
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = selectedLayerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val gradientSelected = selectedMethod == "gradient"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Gradient Mask
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (gradientSelected) 1.5.dp else 1.dp,
                                            color = if (gradientSelected) MaterialTheme.colors.primary else Color(PanelDivider),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedMethod = "gradient" },
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = if (gradientSelected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                    elevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_gradient_24px),
                                            contentDescription = "Gradient Mask",
                                            tint = if (gradientSelected) MaterialTheme.colors.primary else Color(PanelTextSecondary),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                // Paint Mask: langsung buka editor tanpa tombol Apply.
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(
                                            width = 1.dp,
                                            color = Color(PanelDivider),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onPaintMaskDirect() },
                                    shape = RoundedCornerShape(10.dp),
                                    backgroundColor = Color(0xFFF8FAFC),
                                    elevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_mask_24px),
                                            contentDescription = "Paint Mask",
                                            tint = MaterialTheme.colors.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            if (selectedMethod != null) {
                                Divider(color = Color(PanelDivider), thickness = 0.5.dp)

                                RemoveIconSliderRow(
                                    iconRes = R.drawable.ic_merge_layers_24px,
                                    iconDesc = "Edge Smoothness",
                                    value = featherStrength,
                                    onValueChange = { featherStrength = it },
                                    valueRange = 0f..1f,
                                    valueText = "${(featherStrength * 100).toInt()}%"
                                )
                            }

                            RemoveIconChip(
                                iconRes = R.drawable.ic_sharp_restart_24px,
                                modifier = Modifier.fillMaxWidth(),
                                contentDescription = "Reset",
                                onClick = {
                                    selectedMethod = null
                                    featherStrength = 0.5f
                                    onReset()
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .padding(start = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sharp_clear_24px),
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(PanelTextSecondary)
                                )
                            }
                            Button(
                                onClick = { onApply(selectedMethod, featherStrength) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp),
                                enabled = selectedMethod != null
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = "Apply",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
