package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.ui.dialog.RecentEntry

private val ThreeDShadowColorScheme = lightColors(
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
private fun RowScope.ViewTypeSegment(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .shadow(if (selected) 1.dp else 0.dp, shape)
            .clip(shape)
            .background(if (selected) MaterialTheme.colors.surface else Color.Transparent, shape)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colors.primary else Color(PanelTextSecondary)
        )
    }
}

/**
 * Compose bottom sheet untuk 3D Shadow, gayanya identik 100% dengan 3D Rotate:
 * Card bawah, drag handle, kolom pengaturan di kiri (label + slider + nilai per baris horizontal)
 * dengan tombol Reset dan pilihan warna, serta kolom tombol Cancel/Apply di kanan.
 */
@Composable
fun ThreeDShadowDetailPage(
    depth: Int,
    color: Long,
    angle: Float,
    blur: Float,
    opacity: Float,
    viewType: String,
    recents: List<RecentEntry> = emptyList(),
    onRecentPicked: (RecentEntry) -> Unit = {},
    onDepthChange: (Int) -> Unit,
    onColorPickRequested: () -> Unit,
    onAngleChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onViewTypeChange: (String) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    val cardColor = MaterialTheme.colors.surface
    val isOblique = viewType == "OBLIQUE"
    val colorArgb = (color and 0xFFFFFFFFL).toInt()

    var depthState by remember(depth) { mutableStateOf(depth) }
    var angleState by remember(angle) { mutableStateOf(angle) }
    var blurState by remember(blur) { mutableStateOf(blur) }
    var opacityState by remember(opacity) { mutableStateOf(opacity) }

    MaterialTheme(colors = ThreeDShadowColorScheme) {
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
                backgroundColor = cardColor
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
                        // Kolom Kiri: Scrollable controls (Identik 3D Rotate)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            // Segmented View Type (Oblique vs Isometric)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(PanelAlt),
                                elevation = 0.dp
                            ) {
                                Row(modifier = Modifier.padding(3.dp)) {
                                    ViewTypeSegment("Oblique", isOblique) { onViewTypeChange("OBLIQUE") }
                                    ViewTypeSegment("Isometric", !isOblique) { onViewTypeChange("ISOMETRIC") }
                                }
                            }

                            // 1. Depth Row
                            ShadowRow(
                                label = "Depth",
                                value = depthState.toFloat(),
                                valueRange = 1f..50f,
                                steps = 49,
                                displayValue = "$depthState",
                                onValueChange = { new ->
                                    depthState = new.toInt()
                                    onDepthChange(new.toInt())
                                }
                            )

                            // 2. Angle Row (hanya jika Oblique)
                            if (isOblique) {
                                ShadowRow(
                                    label = "Angle",
                                    value = angleState,
                                    valueRange = 0f..360f,
                                    steps = 359,
                                    displayValue = "${angleState.toInt()}°",
                                    onValueChange = { new ->
                                        angleState = new
                                        onAngleChange(new)
                                    }
                                )
                            }

                            // 3. Blur Row
                            ShadowRow(
                                label = "Blur",
                                value = blurState,
                                valueRange = 0f..40f,
                                steps = 40,
                                displayValue = "${blurState.toInt()}",
                                onValueChange = { new ->
                                    blurState = new
                                    onBlurChange(new)
                                }
                            )

                            // 4. Opacity Row
                            ShadowRow(
                                label = "Opacity",
                                value = opacityState,
                                valueRange = 0f..1f,
                                steps = 100,
                                displayValue = "${(opacityState * 100).toInt()}%",
                                onValueChange = { new ->
                                    opacityState = new
                                    onOpacityChange(new)
                                }
                            )

                            // Reset Button (Identik 3D Rotate)
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        depthState = 10
                                        angleState = 45f
                                        blurState = 0f
                                        opacityState = 0.6f
                                        onDepthChange(10)
                                        onAngleChange(45f)
                                        onBlurChange(0f)
                                        onOpacityChange(0.6f)
                                    }
                                    .padding(vertical = 4.dp)
                            )

                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Shadow Color Picker Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Shadow Color",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onColorPickRequested,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Pick shadow color",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (recents.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    recents.forEachIndexed { index, entry ->
                                        if (entry is RecentEntry.Solid) {
                                            RecentColorChip(entry, entry.color == colorArgb) { onRecentPicked(entry) }
                                            if (index < recents.lastIndex) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Kolom Kanan: Tombol Cancel & Apply (Identik 3D Rotate)
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

/**
 * Baris item kontrol slider horizontal (Label | Slider | Nilai), identik 100% dengan RotateRow di 3D Rotate.
 */
@Composable
private fun ShadowRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    displayValue: String,
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
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}