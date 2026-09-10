package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import androidx.compose.ui.unit.dp

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
            .padding(vertical = 9.dp),
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
fun ThreeDShadowDetailPage(
    enabled: Boolean,
    depth: Int,
    color: Long,
    angle: Float,
    blur: Float,
    opacity: Float,
    viewType: String,
    onEnabledChange: (Boolean) -> Unit,
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
                    .heightIn(max = with(LocalDensity.current) { maxHeightPx.toDp() }),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                elevation = 8.dp,
                backgroundColor = cardColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
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
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "3D Shadow",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A2E),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Depth, blur, and opacity",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Enable 3D Shadow",
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = onEnabledChange,
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colors.primary)
                                )
                            }

                            if (enabled) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = Color(PanelDivider), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(PanelAlt),
                                    elevation = 0.dp
                                ) {
                                    Row(modifier = Modifier.padding(3.dp)) {
                                        ViewTypeSegment("Oblique", isOblique) { onViewTypeChange("OBLIQUE") }
                                        ViewTypeSegment("Isometric", !isOblique) { onViewTypeChange("ISOMETRIC") }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(text = "Depth: $depthState", style = MaterialTheme.typography.body2)
                                Slider(
                                    value = depthState.toFloat(),
                                    onValueChange = { new ->
                                        depthState = new.toInt()
                                        onDepthChange(new.toInt())
                                    },
                                    valueRange = 1f..50f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (isOblique) {
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(text = "Angle: ${angleState.toInt()}°", style = MaterialTheme.typography.body2)
                                    Slider(
                                        value = angleState,
                                        onValueChange = { new ->
                                            angleState = new
                                            onAngleChange(new)
                                        },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(text = "Blur: ${Math.round(blurState * 10f) / 10f}", style = MaterialTheme.typography.body2)
                                Slider(
                                    value = blurState,
                                    onValueChange = { new ->
                                        blurState = new
                                        onBlurChange(new)
                                    },
                                    valueRange = 0f..40f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(text = "Opacity: ${(opacityState * 100).toInt()}%", style = MaterialTheme.typography.body2)
                                Slider(
                                    value = opacityState,
                                    onValueChange = { new ->
                                        opacityState = new
                                        onOpacityChange(new)
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color(PanelDivider), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Shadow Color",
                                        style = MaterialTheme.typography.body2,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable(onClick = onColorPickRequested)
                                            .semantics { contentDescription = "Shadow color" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(colorArgb), RoundedCornerShape(18.dp))
                                                .border(1.dp, Color(PanelDivider), RoundedCornerShape(18.dp))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .width(64.dp)
                                .padding(start = 4.dp),
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
                            Spacer(modifier = Modifier.height(2.dp))
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