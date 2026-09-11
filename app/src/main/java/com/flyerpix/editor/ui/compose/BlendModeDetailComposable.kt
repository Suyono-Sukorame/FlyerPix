package com.flyerpix.editor.ui.compose

import android.graphics.PorterDuff
import android.os.Build
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.canvas.model.ExtendedBlendMode

private val BlendColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF4F6FA
private const val PanelHandle = 0xFFD0D4DE

@Composable
private fun BlendChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(PanelTextSecondary)
        )
    }
}

/**
 * Compose bottom sheet untuk Text Blend Mode.
 */
@Composable
fun BlendModeDetailPage(
    currentMode: PorterDuff.Mode,
    currentExtra: ExtendedBlendMode?,
    onModeSelect: (PorterDuff.Mode) -> Unit,
    onExtraSelect: (ExtendedBlendMode) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var modeState by remember(currentMode) { mutableStateOf(currentMode) }
    var extraState by remember(currentExtra) { mutableStateOf(currentExtra) }

    fun descriptionText(): String {
        return when {
            extraState == ExtendedBlendMode.HARD_LIGHT -> "Hard Light: Combines Multiply & Screen with harsher contrast."
            extraState == ExtendedBlendMode.SOFT_LIGHT -> "Soft Light: Diffuses light with subtle gentle contrast."
            extraState == ExtendedBlendMode.COLOR_BURN -> "Color Burn: Increases contrast to darken the background."
            extraState == ExtendedBlendMode.COLOR_DODGE -> "Color Dodge: Brightens background to reflect text color."
            extraState == ExtendedBlendMode.DIFFERENCE -> "Difference: Subtracts colors to invert contrast."
            extraState == ExtendedBlendMode.EXCLUSION -> "Exclusion: Softer inverted contrast than Difference."
            modeState == PorterDuff.Mode.SRC_OVER -> "Normal: Shows the layer's standard colors over the background."
            modeState == PorterDuff.Mode.MULTIPLY -> "Multiply: Multiplies colors for darker blending."
            modeState == PorterDuff.Mode.SCREEN -> "Screen: Inverts and multiplies for bright glowing text."
            modeState == PorterDuff.Mode.OVERLAY -> "Overlay: Combines Multiply and Screen based on the background."
            modeState == PorterDuff.Mode.DARKEN -> "Darken: Retains the darker pixels of text and background."
            modeState == PorterDuff.Mode.LIGHTEN -> "Lighten: Retains the brighter pixels of text and background."
            modeState == PorterDuff.Mode.ADD -> "Add: Adds color values for strong luminescence."
            else -> "Mode: ${modeState.name}"
        }
    }

    val standardModes = listOf(
        "Normal" to PorterDuff.Mode.SRC_OVER,
        "Multiply" to PorterDuff.Mode.MULTIPLY,
        "Screen" to PorterDuff.Mode.SCREEN,
        "Overlay" to PorterDuff.Mode.OVERLAY,
        "Darken" to PorterDuff.Mode.DARKEN,
        "Lighten" to PorterDuff.Mode.LIGHTEN,
        "Add" to PorterDuff.Mode.ADD
    )

    val advancedModes = listOf(
        "Hard Light" to ExtendedBlendMode.HARD_LIGHT,
        "Soft Light" to ExtendedBlendMode.SOFT_LIGHT,
        "Color Burn" to ExtendedBlendMode.COLOR_BURN,
        "Color Dodge" to ExtendedBlendMode.COLOR_DODGE,
        "Difference" to ExtendedBlendMode.DIFFERENCE,
        "Exclusion" to ExtendedBlendMode.EXCLUSION
    )

    MaterialTheme(colors = BlendColorScheme) {
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
                                .background(Color(PanelHandle))
                        )
                    }

                    // Main 2-column layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Column: Scrollable Blend Mode controls
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Header Row: Title + Reset
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Blend Mode",
                                    style = MaterialTheme.typography.subtitle2,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colors.onSurface
                                )

                                TextButton(
                                    onClick = {
                                        modeState = PorterDuff.Mode.SRC_OVER
                                        extraState = null
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

                            // Dynamic Mode Description Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = Color(PanelAlt),
                                elevation = 0.dp
                            ) {
                                Text(
                                    text = descriptionText(),
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Divider(color = Color(PanelDivider), thickness = 0.75.dp)

                            // Standard Modes Section
                            Text(
                                text = "Standard Modes",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.Bold,
                                color = Color(PanelTextSecondary)
                            )

                            // Chips Row 1 & 2
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 0 until minOf(4, standardModes.size)) {
                                    val (label, mode) = standardModes[i]
                                    val isSelected = extraState == null && modeState == mode
                                    Box(modifier = Modifier.weight(1f)) {
                                        BlendChip(
                                            label = label,
                                            selected = isSelected,
                                            onClick = {
                                                modeState = mode
                                                extraState = null
                                                onModeSelect(mode)
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in 4 until standardModes.size) {
                                    val (label, mode) = standardModes[i]
                                    val isSelected = extraState == null && modeState == mode
                                    Box(modifier = Modifier.weight(1f)) {
                                        BlendChip(
                                            label = label,
                                            selected = isSelected,
                                            onClick = {
                                                modeState = mode
                                                extraState = null
                                                onModeSelect(mode)
                                            }
                                        )
                                    }
                                }
                                // Filler box for alignment
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Advanced Modes Section (Android 10+)
                            if (Build.VERSION.SDK_INT >= 29) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Advanced Modes",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(PanelTextSecondary)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 0..2) {
                                        val (label, extra) = advancedModes[i]
                                        val isSelected = extraState == extra
                                        Box(modifier = Modifier.weight(1f)) {
                                            BlendChip(
                                                label = label,
                                                selected = isSelected,
                                                onClick = {
                                                    extraState = extra
                                                    modeState = PorterDuff.Mode.SRC_OVER
                                                    onExtraSelect(extra)
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (i in 3..5) {
                                        val (label, extra) = advancedModes[i]
                                        val isSelected = extraState == extra
                                        Box(modifier = Modifier.weight(1f)) {
                                            BlendChip(
                                                label = label,
                                                selected = isSelected,
                                                onClick = {
                                                    extraState = extra
                                                    modeState = PorterDuff.Mode.SRC_OVER
                                                    onExtraSelect(extra)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right Column: Vertical Action Buttons (Cancel & Apply)
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
                                    color = Color(PanelTextSecondary)
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
