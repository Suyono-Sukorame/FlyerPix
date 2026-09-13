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
import com.flyerpix.editor.canvas.model.CanvasLayer

private val ShadowColorScheme = lightColors(
    primary = Color(0xFF5E35B1),
    primaryVariant = Color(0xFF3F2C70),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

/**
 * Shadow Editor Panel - advanced shadow effects (Phase 7).
 */
@Composable
fun ShadowEditorPanel(
    layer: CanvasLayer,
    onChanged: () -> Unit,
    onExitEditMode: () -> Unit,
    maxHeightPx: Int = 600
) {
    MaterialTheme(colors = ShadowColorScheme) {
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
                                text = "Shadow & Lighting",
                                style = MaterialTheme.typography.subtitle2,
                                color = Color(PanelTextSecondary),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── DROP SHADOW ─────────────────────────────────
                            ShadowTypeSection(
                                title = "Drop Shadow",
                                enabled = layer.shadowEnabled,
                                onEnabledChange = { layer.shadowEnabled = it; onChanged() },
                                content = {
                                    SliderRow("Radius", layer.shadowRadius, 0f, 50f) { v ->
                                        layer.shadowRadius = v
                                        onChanged()
                                    }
                                    SliderRow("Offset X", layer.shadowDx, -30f, 30f) { v ->
                                        layer.shadowDx = v
                                        onChanged()
                                    }
                                    SliderRow("Offset Y", layer.shadowDy, -30f, 30f) { v ->
                                        layer.shadowDy = v
                                        onChanged()
                                    }
                                    SliderRow("Opacity", layer.shadowOpacity, 0f, 1f) { v ->
                                        layer.shadowOpacity = v
                                        onChanged()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── INNER SHADOW ────────────────────────────────
                            ShadowTypeSection(
                                title = "Inner Shadow",
                                enabled = layer.innerShadowEnabled,
                                onEnabledChange = { layer.innerShadowEnabled = it; onChanged() },
                                content = {
                                    SliderRow("Radius", layer.innerShadowRadius, 0f, 20f) { v ->
                                        layer.innerShadowRadius = v
                                        onChanged()
                                    }
                                    SliderRow("Offset X", layer.innerShadowDx, -15f, 15f) { v ->
                                        layer.innerShadowDx = v
                                        onChanged()
                                    }
                                    SliderRow("Offset Y", layer.innerShadowDy, -15f, 15f) { v ->
                                        layer.innerShadowDy = v
                                        onChanged()
                                    }
                                    SliderRow("Opacity", layer.innerShadowOpacity, 0f, 1f) { v ->
                                        layer.innerShadowOpacity = v
                                        onChanged()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── LONG SHADOW ─────────────────────────────────
                            ShadowTypeSection(
                                title = "Long Shadow",
                                enabled = layer.longShadowEnabled,
                                onEnabledChange = { layer.longShadowEnabled = it; onChanged() },
                                content = {
                                    SliderRow("Length", layer.longShadowLength, 10f, 200f) { v ->
                                        layer.longShadowLength = v
                                        onChanged()
                                    }
                                    SliderRow("Angle", layer.longShadowAngle, 0f, 360f) { v ->
                                        layer.longShadowAngle = v
                                        onChanged()
                                    }
                                    SliderRow("Blur", layer.longShadowBlur, 0f, 10f) { v ->
                                        layer.longShadowBlur = v
                                        onChanged()
                                    }
                                    SliderRow("Opacity", layer.longShadowOpacity, 0f, 1f) { v ->
                                        layer.longShadowOpacity = v
                                        onChanged()
                                    }
                                }
                            )

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
private fun ShadowTypeSection(
    title: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = Color(PanelTextSecondary),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }

        if (enabled) {
            Divider(color = Color(PanelDivider), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            modifier = Modifier.width(50.dp),
            fontSize = 9.sp
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = minValue..maxValue,
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFF5E35B1))
        )
        Text(
            text = "${value.toInt()}",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            modifier = Modifier.width(25.dp),
            textAlign = TextAlign.End,
            fontSize = 9.sp
        )
    }
}
