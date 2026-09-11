package com.flyerpix.editor.ui.compose

import android.text.Layout
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

private val AlignColorScheme = lightColors(
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
private fun AlignPresetChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else Color(PanelAlt))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
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
 * Compose bottom sheet untuk Text Alignment dan Wrap Text.
 */
@Composable
fun AlignDetailPage(
    alignment: Layout.Alignment,
    justifyEnabled: Boolean,
    wrapTextEnabled: Boolean,
    wrapWidth: Float,
    onAlignChange: (Layout.Alignment, Boolean) -> Unit,
    onWrapTextChange: (Boolean) -> Unit,
    onWrapWidthChange: (Float) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 380
) {
    var alignState by remember(alignment) { mutableStateOf(alignment) }
    var justifyState by remember(justifyEnabled) { mutableStateOf(justifyEnabled) }
    var wrapEnabledState by remember(wrapTextEnabled) { mutableStateOf(wrapTextEnabled) }
    var wrapWidthState by remember(wrapWidth) { mutableStateOf(wrapWidth.coerceIn(60f, 1600f)) }

    MaterialTheme(colors = AlignColorScheme) {
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
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Header row
                            Text(
                                text = "Text Alignment",
                                style = MaterialTheme.typography.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface
                            )

                            // Alignment Chips: Left, Center, Right, Justify
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AlignPresetChip(
                                    label = "Left",
                                    selected = !justifyState && alignState == Layout.Alignment.ALIGN_NORMAL,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    alignState = Layout.Alignment.ALIGN_NORMAL
                                    justifyState = false
                                    onAlignChange(Layout.Alignment.ALIGN_NORMAL, false)
                                }
                                AlignPresetChip(
                                    label = "Center",
                                    selected = !justifyState && alignState == Layout.Alignment.ALIGN_CENTER,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    alignState = Layout.Alignment.ALIGN_CENTER
                                    justifyState = false
                                    onAlignChange(Layout.Alignment.ALIGN_CENTER, false)
                                }
                                AlignPresetChip(
                                    label = "Right",
                                    selected = !justifyState && alignState == Layout.Alignment.ALIGN_OPPOSITE,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    alignState = Layout.Alignment.ALIGN_OPPOSITE
                                    justifyState = false
                                    onAlignChange(Layout.Alignment.ALIGN_OPPOSITE, false)
                                }
                                AlignPresetChip(
                                    label = "Justify",
                                    selected = justifyState,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    alignState = Layout.Alignment.ALIGN_NORMAL
                                    justifyState = true
                                    onAlignChange(Layout.Alignment.ALIGN_NORMAL, true)
                                }
                            }

                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Wrap Text Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Wrap Text",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colors.onSurface
                                )
                                Switch(
                                    checked = wrapEnabledState,
                                    onCheckedChange = { checked ->
                                        wrapEnabledState = checked
                                        onWrapTextChange(checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colors.primary,
                                        checkedTrackColor = MaterialTheme.colors.primaryVariant
                                    )
                                )
                            }

                            // Wrap Width Slider (if Wrap Text is enabled)
                            if (wrapEnabledState) {
                                SettingSliderRow(
                                    label = "Width",
                                    value = wrapWidthState,
                                    range = 60f..1600f,
                                    steps = 154,
                                    valueText = "${wrapWidthState.toInt()}px",
                                    onValueChange = { v ->
                                        wrapWidthState = v
                                        onWrapWidthChange(v)
                                    }
                                )
                            }
                        }

                        // Right column: Cancel & Apply
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
                                Text("Cancel", style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = onApply,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "Apply",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
    }
}
