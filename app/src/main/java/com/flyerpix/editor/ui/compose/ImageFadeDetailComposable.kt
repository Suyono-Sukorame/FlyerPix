package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.ImageFadeType

private val FadeColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val FadeTextSecondary = 0xFF5F6B7A
private const val FadeDivider = 0xFFE4E8F0
private const val FadeAlt = 0xFFF4F6FA
private const val FadeHandle = 0xFFD0D4DE

/** Daftar mode fade + label yang ditampilkan di UI. */
private val FADE_MODES: List<Pair<ImageFadeType, String>> = listOf(
    ImageFadeType.LINEAR_LEFT to "Left",
    ImageFadeType.LINEAR_RIGHT to "Right",
    ImageFadeType.LINEAR_TOP to "Top",
    ImageFadeType.LINEAR_BOTTOM to "Bottom",
    ImageFadeType.ALL_EDGES_FEATHER to "All Edges",
    ImageFadeType.RADIAL to "Radial"
)

@Composable
private fun FadeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(FadeAlt))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(FadeTextSecondary)
        )
    }
}

/**
 * Compose bottom sheet untuk Soft Edge / Gradient Fade pada [ImageLayer].
 *
 * Memungkinkan foto melebur mulus ke latar belakang lewat mask gradasi
 * (directional linear, feather 4 sisi, atau vignette radial).
 */
@Composable
fun ImageFadeDetailPage(
    enabled: Boolean,
    fadeType: ImageFadeType,
    intensityPct: Float,
    curve: Float,
    onEnabledChange: (Boolean) -> Unit,
    onFadeTypeChange: (ImageFadeType) -> Unit,
    onIntensityChange: (Float) -> Unit,
    onCurveChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var enabledState by remember(enabled) { mutableStateOf(enabled) }
    var typeState by remember(fadeType) { mutableStateOf(fadeType) }
    var intensityState by remember(intensityPct) { mutableStateOf(intensityPct) }
    var curveState by remember(curve) { mutableStateOf(curve) }

    fun description(): String = when (typeState) {
        ImageFadeType.LINEAR_LEFT -> "Fades the left edge so the photo blends into the background."
        ImageFadeType.LINEAR_RIGHT -> "Fades the right edge of the photo smoothly."
        ImageFadeType.LINEAR_TOP -> "Fades the top edge of the photo smoothly."
        ImageFadeType.LINEAR_BOTTOM -> "Fades the bottom edge of the photo smoothly."
        ImageFadeType.ALL_EDGES_FEATHER -> "Feathers all four edges for a seamless cut-out look."
        ImageFadeType.RADIAL -> "Radial/oval vignette: solid center fading toward the edges."
    }

    MaterialTheme(colors = FadeColorScheme) {
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
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Drag Handle
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(FadeHandle))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: controls
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Soft Edge / Fade",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )
                                TextButton(
                                    onClick = {
                                        enabledState = false
                                        typeState = ImageFadeType.LINEAR_LEFT
                                        intensityState = 50f
                                        curveState = 1f
                                        onReset()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Reset",
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Enable Fade",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Switch(
                                    checked = enabledState,
                                    onCheckedChange = {
                                        enabledState = it
                                        onEnabledChange(it)
                                    }
                                )
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = Color(FadeAlt),
                                elevation = 0.dp
                            ) {
                                Text(
                                    text = description(),
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Divider(color = Color(FadeDivider), thickness = 0.75.dp)

                            Text(
                                text = "Direction",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = Color(FadeTextSecondary)
                            )

                            FADE_MODES.chunked(3).forEach { rowModes ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowModes.forEach { (type, label) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            FadeChip(
                                                label = label,
                                                selected = typeState == type,
                                                onClick = {
                                                    typeState = type
                                                    onFadeTypeChange(type)
                                                    if (!enabledState) {
                                                        enabledState = true
                                                        onEnabledChange(true)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    repeat(3 - rowModes.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Divider(color = Color(FadeDivider), thickness = 0.75.dp)

                            Text(
                                text = "Fade Depth: ${intensityState.toInt()}%",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = Color(FadeTextSecondary)
                            )
                            Slider(
                                value = intensityState.coerceIn(0f, 100f),
                                onValueChange = {
                                    intensityState = it
                                    onIntensityChange(it)
                                },
                                valueRange = 0f..100f
                            )

                            Text(
                                text = "Softness: ${"%.1f".format(curveState)}",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = Color(FadeTextSecondary)
                            )
                            Slider(
                                value = curveState.coerceIn(0.2f, 4f),
                                onValueChange = {
                                    curveState = it
                                    onCurveChange(it)
                                },
                                valueRange = 0.2f..4f
                            )
                        }

                        // Right Column: Cancel & Apply
                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "✕",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(FadeTextSecondary)
                                )
                            }

                            Button(
                                onClick = onApply,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary)
                            ) {
                                Text(
                                    text = "✓",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
