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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.AnchorType
import com.flyerpix.editor.canvas.model.PenLayer

private val BezierColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

/**
 * Compose bottom sheet untuk editor anchor Bézier (Phase 1).
 * Memungkinkan user mengedit posisi anchor, handle, dan tipe node (SMOOTH/CORNER).
 */
@Composable
fun BezierAnchorEditorPanel(
    penLayer: PenLayer,
    selectedAnchorIndex: Int,
    onAnchorChanged: () -> Unit,
    onExitEditMode: () -> Unit,
    maxHeightPx: Int = 500
) {
    if (selectedAnchorIndex < 0 || selectedAnchorIndex >= penLayer.anchors.size) {
        // No anchor selected - show message
        BezierAnchorEditorEmptyState(
            onExitEditMode = onExitEditMode,
            maxHeightPx = maxHeightPx
        )
        return
    }

    val anchor = penLayer.anchors[selectedAnchorIndex]
    
    var posX by remember { mutableStateOf(anchor.x) }
    var posY by remember { mutableStateOf(anchor.y) }
    var anchorType by remember { mutableStateOf(anchor.type) }
    
    // Update internal state when anchor changes externally
    LaunchedEffect(anchor.x, anchor.y, anchor.type) {
        posX = anchor.x
        posY = anchor.y
        anchorType = anchor.type
    }

    MaterialTheme(colors = BezierColorScheme) {
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
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )

                    // Tab selector (Phase 3)
                    var activeTab by remember { mutableStateOf("edit") }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton("Edit", activeTab == "edit", { activeTab = "edit" }, modifier = Modifier.weight(1f))
                        TabButton("Stroke", activeTab == "stroke", { activeTab = "stroke" }, modifier = Modifier.weight(1f))
                    }

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
                            if (activeTab == "edit") {
                                // ── EDIT TAB (original anchor editing content) ──
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            // Anchor index display
                            Text(
                                text = "Anchor ${selectedAnchorIndex + 1} of ${penLayer.anchors.size}",
                                style = MaterialTheme.typography.subtitle2,
                                color = Color(PanelTextSecondary),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Position X
                            AnchorPositionRow(
                                label = "X",
                                value = posX,
                                displayValue = "${posX.toInt()} px",
                                onValueChange = { newX ->
                                    posX = newX
                                    anchor.x = newX
                                    onAnchorChanged()
                                }
                            )

                            // Position Y
                            AnchorPositionRow(
                                label = "Y",
                                value = posY,
                                displayValue = "${posY.toInt()} px",
                                onValueChange = { newY ->
                                    posY = newY
                                    anchor.y = newY
                                    onAnchorChanged()
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Node Type Toggle
                            Text(
                                text = "Node Type",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NodeTypeButton(
                                    label = "Corner",
                                    isSelected = anchorType == AnchorType.CORNER,
                                    onClick = {
                                        anchorType = AnchorType.CORNER
                                        anchor.type = AnchorType.CORNER
                                        onAnchorChanged()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                NodeTypeButton(
                                    label = "Smooth",
                                    isSelected = anchorType == AnchorType.SMOOTH,
                                    onClick = {
                                        anchorType = AnchorType.SMOOTH
                                        anchor.type = AnchorType.SMOOTH
                                        // Mirror handles if switching to SMOOTH
                                        if (anchor.hasActiveHandles()) {
                                            anchor.handleOutX = 2 * anchor.x - anchor.handleInX
                                            anchor.handleOutY = 2 * anchor.y - anchor.handleInY
                                        }
                                        onAnchorChanged()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Handle In (if visible)
                            if (anchor.hasActiveHandles()) {
                                Text(
                                    text = "Handle In",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold
                                )
                                HandleDistanceDisplay(
                                    label = "In",
                                    handleX = anchor.handleInX,
                                    handleY = anchor.handleInY,
                                    anchorX = anchor.x,
                                    anchorY = anchor.y,
                                    onUpdate = { hx, hy ->
                                        penLayer.moveHandleIn(selectedAnchorIndex, hx, hy)
                                        onAnchorChanged()
                                    }
                                )
                            }

                            // Handle Out (if visible)
                            if (anchor.hasActiveHandles()) {
                                Text(
                                    text = "Handle Out",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold
                                )
                                HandleDistanceDisplay(
                                    label = "Out",
                                    handleX = anchor.handleOutX,
                                    handleY = anchor.handleOutY,
                                    anchorX = anchor.x,
                                    anchorY = anchor.y,
                                    onUpdate = { hx, hy ->
                                        penLayer.moveHandleOut(selectedAnchorIndex, hx, hy)
                                        onAnchorChanged()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Path Operations (Phase 2)
                            Text(
                                text = "Path Operations",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SmallIconButton(
                                    label = "Reverse",
                                    icon = "↻",
                                    onClick = {
                                        penLayer.reversePath()
                                        onAnchorChanged()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SmallIconButton(
                                    label = "Close",
                                    icon = "◯",
                                    onClick = {
                                        if (penLayer.isClosed) penLayer.openPath()
                                        else penLayer.closePath()
                                        onAnchorChanged()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Anchor operations
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        penLayer.insertAnchor(selectedAnchorIndex, anchor.x, anchor.y)
                                        onAnchorChanged()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF4CAF50)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Button(
                                    onClick = {
                                        if (penLayer.removeAnchor(selectedAnchorIndex)) {
                                            onAnchorChanged()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFFF5252)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        } // Close edit tab
                        else if (activeTab == "stroke") {
                            // ── STROKE TAB (stroke & fill settings) ──
                            StrokeAdvancedPanel(penLayer = penLayer, onChanged = onAnchorChanged)
                        }
                        }

                        // Right-side button
                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .height(200.dp)
                                .padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                        ) {
                            Button(
                                onClick = onExitEditMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFFF6B6B)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Panel kosong ketika tidak ada anchor yang dipilih.
 */
@Composable
private fun BezierAnchorEditorEmptyState(
    onExitEditMode: () -> Unit,
    maxHeightPx: Int = 500
) {
    MaterialTheme(colors = BezierColorScheme) {
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
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(PanelHandle), RoundedCornerShape(50))
                    )

                    Text(
                        text = "Tap an anchor on the path to edit",
                        style = MaterialTheme.typography.body2,
                        color = Color(PanelTextSecondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )

                    Button(
                        onClick = onExitEditMode,
                        modifier = Modifier
                            .width(60.dp)
                            .height(40.dp)
                            .padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFFF6B6B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

/**
 * Node type button untuk memilih CORNER atau SMOOTH.
 */
@Composable
private fun NodeTypeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color(0xFF1769FF) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

/**
 * Display handle distance/position (relative to anchor).
 */
@Composable
private fun HandleDistanceDisplay(
    label: String,
    handleX: Float,
    handleY: Float,
    anchorX: Float,
    anchorY: Float,
    onUpdate: (Float, Float) -> Unit
) {
    val distX = handleX - anchorX
    val distY = handleY - anchorY
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Distance X slider
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ΔX",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(20.dp)
            )
            Slider(
                value = distX,
                onValueChange = { newDistX ->
                    onUpdate(anchorX + newDistX, anchorY + distY)
                },
                valueRange = -200f..200f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1769FF),
                    activeTrackColor = Color(0xFF1769FF)
                )
            )
            Text(
                text = "${distX.toInt()}",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.End
            )
        }

        // Distance Y slider
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ΔY",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(20.dp)
            )
            Slider(
                value = distY,
                onValueChange = { newDistY ->
                    onUpdate(anchorX + distX, anchorY + newDistY)
                },
                valueRange = -200f..200f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF1769FF),
                    activeTrackColor = Color(0xFF1769FF)
                )
            )
            Text(
                text = "${distY.toInt()}",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Tombol kecil untuk operasi path (icon + label).
 */
@Composable
private fun SmallIconButton(
    label: String,
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$icon $label",
            color = Color.White,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

/**
 * Baris pengaturan posisi anchor (Label | Slider | Nilai).
 */
@Composable
private fun AnchorPositionRow(
    label: String,
    value: Float,
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
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(20.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -1000f..2000f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF1769FF),
                activeTrackColor = Color(0xFF1769FF)
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
            modifier = Modifier.width(50.dp)
        )
    }
}

/**
 * Tab button untuk switch antara Edit dan Stroke tabs.
 */
@Composable
private fun TabButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isActive) Color(0xFF1769FF) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}
