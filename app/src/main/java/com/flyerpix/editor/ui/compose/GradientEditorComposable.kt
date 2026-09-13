package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
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
import android.graphics.Color as AndroidColor
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.canvas.model.PenLayer
import com.flyerpix.editor.canvas.model.ShapeLayer
import com.flyerpix.editor.canvas.model.CanvasLayer

private val GradientColorScheme = lightColors(
    primary = Color(0xFFFF7043),
    primaryVariant = Color(0xFFE64A19),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

/**
 * Gradient Editor Panel - advanced gradient & texture fills (Phase 6).
 */
@Composable
fun GradientEditorPanel(
    layer: CanvasLayer,
    onChanged: () -> Unit,
    onExitEditMode: () -> Unit,
    maxHeightPx: Int = 600
) {
    MaterialTheme(colors = GradientColorScheme) {
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
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Gradient & Texture",
                                style = MaterialTheme.typography.subtitle2,
                                color = Color(PanelTextSecondary),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── GRADIENT SECTION ────────────────────────────────
                            when (layer) {
                                is PenLayer -> {
                                    GradientControls(layer, onChanged)
                                }
                                is ShapeLayer -> {
                                    GradientControls(layer, onChanged)
                                }
                                else -> {
                                    Text(
                                        "Gradients available for Pen & Shape layers only",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Right-side close button
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

@Composable
private fun GradientControls(layer: Any, onChanged: () -> Unit) {
    val penLayer = layer as? PenLayer
    val shapeLayer = layer as? ShapeLayer
    
    val gradient = penLayer?.gradient ?: shapeLayer?.gradient
    val useGradient = gradient != null
    val gradientType = gradient?.type ?: GradientType.LINEAR

    // Toggle gradient
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Gradient Fill",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Checkbox(
            checked = useGradient,
            onCheckedChange = { checked ->
                val newGradient = if (checked) {
                    GradientColor(intArrayOf(AndroidColor.WHITE, AndroidColor.BLACK))
                } else {
                    null
                }
                when (layer) {
                    is PenLayer -> layer.gradient = newGradient
                    is ShapeLayer -> layer.gradient = newGradient
                }
                onChanged()
            }
        )
    }

    if (useGradient) {
        Spacer(modifier = Modifier.height(6.dp))

        // Gradient type selector
        Text(
            text = "Type",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            GradientTypeButton("Linear", GradientType.LINEAR, gradientType, modifier = Modifier.weight(1f)) {
                val grad = gradient?.copy(type = it) ?: GradientColor(intArrayOf(AndroidColor.WHITE, AndroidColor.BLACK), type = it)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
            GradientTypeButton("Radial", GradientType.RADIAL, gradientType, modifier = Modifier.weight(1f)) {
                val grad = gradient?.copy(type = it) ?: GradientColor(intArrayOf(AndroidColor.WHITE, AndroidColor.BLACK), type = it)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
            GradientTypeButton("Sweep", GradientType.SWEEP, gradientType, modifier = Modifier.weight(1f)) {
                val grad = gradient?.copy(type = it) ?: GradientColor(intArrayOf(AndroidColor.WHITE, AndroidColor.BLACK), type = it)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Color controls (simplified for speed - show gradient preview)
        val color1 = gradient?.colors?.getOrNull(0) ?: AndroidColor.WHITE
        val color2 = gradient?.colors?.getOrNull(1) ?: AndroidColor.BLACK
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(color1), Color(color2))
                    ),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Color 1 → Color 2",
                color = Color.White,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Presets
        Text(
            text = "Presets",
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
            PresetGradientButton("Sunset", intArrayOf(0xFFFF512F.toInt(), 0xFFDD2476.toInt()), Modifier.weight(1f)) {
                val grad = GradientColor(it, type = gradientType)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
            PresetGradientButton("Ocean", intArrayOf(0xFF2193B0.toInt(), 0xFF6DD5ED.toInt()), Modifier.weight(1f)) {
                val grad = GradientColor(it, type = gradientType)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
            PresetGradientButton("Lime", intArrayOf(0xFF11998E.toInt(), 0xFF38EF7D.toInt()), Modifier.weight(1f)) {
                val grad = GradientColor(it, type = gradientType)
                when (layer) {
                    is PenLayer -> layer.gradient = grad
                    is ShapeLayer -> layer.gradient = grad
                }
                onChanged()
            }
        }
    }
}

@Composable
private fun GradientTypeButton(
    label: String,
    type: GradientType,
    current: GradientType,
    modifier: Modifier = Modifier,
    onClick: (GradientType) -> Unit
) {
    Button(
        onClick = { onClick(type) },
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (current == type) Color(0xFFFF7043) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = if (current == type) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PresetGradientButton(
    label: String,
    colors: IntArray,
    modifier: Modifier = Modifier,
    onClick: (IntArray) -> Unit
) {
    Button(
        onClick = { onClick(colors) },
        modifier = modifier
            .height(32.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(colors[0]), Color(colors[1]))
                ),
                shape = RoundedCornerShape(6.dp)
            ),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}
