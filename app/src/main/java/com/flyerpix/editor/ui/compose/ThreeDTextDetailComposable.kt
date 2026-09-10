package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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

private val LABEL_W  = 72.dp   // lebar label tetap agar slider rata kiri
private val ACTION_W = 62.dp   // lebar kolom Cancel / Apply

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
 * Panel 3D Text — layout kompak 3-kolom sesuai wireframe:
 *
 *  [ Oblique ]  [ Isometric ]
 *  ─────────────────────────────────────  Cancel
 *  Depth: 10  [═══O═══════════════════]
 *  Angle: 45° [══O════════════════════]   Apply
 *  ─────────────────────────────────────
 *  Depth Color                      [ + ]
 *
 * Card tidak memakai height/heightIn — tinggi wrap content (kompak).
 * Box tidak memakai fillMaxHeight — container FrameLayout mengontrol
 * tinggi melalui PanelHeightManager.
 */
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
    val isOblique      = viewType == "OBLIQUE"
    val depthColorArgb = (depthColor and 0xFFFFFFFFL).toInt()

    var depthState by remember(depth) { mutableStateOf(depth) }
    var angleState by remember(angle) { mutableStateOf(angle) }

    MaterialTheme(colors = ThreeDTextColorScheme) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                elevation = 8.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(1.dp))

                    // Body: [controls column] + [action column]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Kolom kontrol (weight=1f, wrap height)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        ) {
                            // ViewType toggle: Oblique / Isometric
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(PanelAlt),
                                elevation = 0.dp
                            ) {
                                Row(modifier = Modifier.padding(3.dp)) {
                                    ViewTypeSegment("Oblique",   isOblique)  { onViewTypeChange("OBLIQUE") }
                                    ViewTypeSegment("Isometric", !isOblique) { onViewTypeChange("ISOMETRIC") }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = Color(PanelDivider))

                            // Depth: label (fixed) + slider (flex)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Depth: $depthState",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(LABEL_W)
                                )
                                Slider(
                                    value = depthState.toFloat(),
                                    onValueChange = { v -> depthState = v.toInt(); onDepthChange(v.toInt()) },
                                    valueRange = 1f..50f,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Angle: label (fixed) + slider (flex) — hanya Oblique
                            if (isOblique) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Angle: ${angleState.toInt()}°",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        modifier = Modifier.width(LABEL_W)
                                    )
                                    Slider(
                                        value = angleState,
                                        onValueChange = { v -> angleState = v; onAngleChange(v) },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Divider(color = Color(PanelDivider))
                            Spacer(modifier = Modifier.height(2.dp))

                            // Depth Color: label (flex) + [+] button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Depth Color",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDepthPickRequested,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Pick depth color",
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Recent color chips (horizontal scroll bila ada)
                            if (recents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    recents.forEachIndexed { idx, entry ->
                                        val sel = when (entry) {
                                            is RecentEntry.Solid    -> entry.color == depthColorArgb
                                            is RecentEntry.Gradient -> gradientsEqual(entry.gradient, currentGradient)
                                        }
                                        RecentColorChip(entry, sel) { onRecentPicked(entry) }
                                        if (idx < recents.lastIndex) Spacer(modifier = Modifier.width(6.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        // Kolom aksi: Cancel + Apply (vertikal center)
                        Column(
                            modifier = Modifier.width(ACTION_W),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
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
