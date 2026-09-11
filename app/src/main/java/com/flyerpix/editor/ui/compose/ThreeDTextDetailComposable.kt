package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.ui.dialog.RecentEntry

private val ThreeDTextColorScheme = lightColors(
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
            .padding(vertical = 5.dp),
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

@Composable
fun ThreeDTextDetailPage(
    depth: Int,
    depthColor: Long,
    angle: Float,
    viewType: String,
    recents: List<RecentEntry> = emptyList(),
    currentGradient: GradientColor? = null,
    onRecentPicked: (RecentEntry) -> Unit = {},
    onDepthChange: (Int) -> Unit,
    onDepthPickRequested: () -> Unit,
    onAngleChange: (Float) -> Unit,
    onViewTypeChange: (String) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    val cardColor = MaterialTheme.colors.surface
    val isOblique = viewType == "OBLIQUE"
    val depthColorArgb = (depthColor and 0xFFFFFFFFL).toInt()

    var depthState by remember(depth) { mutableStateOf(depth) }
    var angleState by remember(angle) { mutableStateOf(angle) }

    MaterialTheme(colors = ThreeDTextColorScheme) {
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Style: 1 baris
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Style",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(52.dp)
                                )
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(9.dp),
                                    color = Color(PanelAlt),
                                    elevation = 0.dp
                                ) {
                                    Row(modifier = Modifier.padding(2.dp)) {
                                        ViewTypeSegment("Oblique", isOblique) { onViewTypeChange("OBLIQUE") }
                                        ViewTypeSegment("Isometric", !isOblique) { onViewTypeChange("ISOMETRIC") }
                                    }
                                }
                            }

                            // Depth: 1 baris
                            SettingRow(
                                label = "Depth",
                                valueText = "$depthState",
                                value = depthState.toFloat(),
                                range = 1f..50f,
                                onValueChange = { new ->
                                    depthState = new.toInt()
                                    onDepthChange(new.toInt())
                                }
                            )

                            // Angle (hanya oblique): 1 baris
                            if (isOblique) {
                                SettingRow(
                                    label = "Angle",
                                    valueText = "${angleState.toInt()}°",
                                    value = angleState,
                                    range = 0f..360f,
                                    onValueChange = { new ->
                                        angleState = new
                                        onAngleChange(new)
                                    }
                                )
                            }

                            // Color: 1 baris (label + swatch + tombol tambah)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Color",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(52.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    recents.forEachIndexed { index, entry ->
                                        val selected = when (entry) {
                                            is RecentEntry.Solid -> entry.color == depthColorArgb
                                            is RecentEntry.Gradient -> gradientsEqual(entry.gradient, currentGradient)
                                        }
                                        RecentColorChip(entry, selected) { onRecentPicked(entry) }
                                        if (index < recents.lastIndex) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                    }
                                    if (recents.isEmpty()) {
                                        Text(
                                            text = "No recent colors",
                                            style = MaterialTheme.typography.caption,
                                            color = Color(PanelTextSecondary)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDepthPickRequested,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Pick depth color",
                                        tint = MaterialTheme.colors.primary
                                    )
                                }
                            }
                        }

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

private fun gradientsEqual(a: GradientColor?, b: GradientColor?): Boolean =
    a != null && b != null &&
        a.colors.contentEquals(b.colors) &&
        a.type == b.type &&
        a.angle == b.angle

@Composable
private fun SettingRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
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
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}