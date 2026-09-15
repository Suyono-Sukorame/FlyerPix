package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R

private val PanelColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelAlt = 0xFFF0F3F8
private const val PanelHandle = 0xFFD0D4DE

@Composable
fun RemoveBgComposable(
    selectedLayerName: String = "Selected Layer",
    onReset: () -> Unit,
    onApply: (method: String?, featherStrength: Float) -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420,
    sessionKey: Int = 0
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var featherStrength by remember { mutableStateOf(0.5f) }
    val scrollState = remember(sessionKey) { ScrollState(0) }

    MaterialTheme(colors = PanelColorScheme) {
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
                                .verticalScroll(scrollState)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(2.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_layers_24px),
                                        contentDescription = null,
                                        tint = MaterialTheme.colors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Target Layer",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = Color(PanelTextSecondary)
                                        )
                                        Text(
                                            text = selectedLayerName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Select Method",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            val gradientSelected = selectedMethod == "gradient"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (gradientSelected) 1.5.dp else 1.dp,
                                        color = if (gradientSelected) MaterialTheme.colors.primary else Color(PanelDivider),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedMethod = "gradient" },
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (gradientSelected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_gradient_24px),
                                        contentDescription = null,
                                        tint = if (gradientSelected) MaterialTheme.colors.primary else Color(PanelTextSecondary),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Gradient Mask (Automatic)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "Best for skies & horizons. Creates smooth fade-out effect.",
                                            fontSize = 10.sp,
                                            color = Color(PanelTextSecondary)
                                        )
                                    }
                                }
                            }

                            val paintSelected = selectedMethod == "paint"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (paintSelected) 1.5.dp else 1.dp,
                                        color = if (paintSelected) MaterialTheme.colors.primary else Color(PanelDivider),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedMethod = "paint" },
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = if (paintSelected) MaterialTheme.colors.primary.copy(alpha = 0.08f) else Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mask_24px),
                                        contentDescription = null,
                                        tint = if (paintSelected) MaterialTheme.colors.primary else Color(PanelTextSecondary),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Paint Mask (Manual)",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            text = "Precise control. Paint black to remove, white to restore.",
                                            fontSize = 10.sp,
                                            color = Color(PanelTextSecondary)
                                        )
                                    }
                                }
                            }

                            if (selectedMethod != null) {
                                Divider(color = Color(PanelDivider), thickness = 0.5.dp)

                                Text(
                                    text = "Edge Smoothness",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Slider(
                                    value = featherStrength,
                                    onValueChange = { featherStrength = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colors.primary,
                                        activeTrackColor = MaterialTheme.colors.primary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Soft",
                                        fontSize = 10.sp,
                                        color = Color(PanelTextSecondary)
                                    )
                                    Text(
                                        "Hard",
                                        fontSize = 10.sp,
                                        color = Color(PanelTextSecondary)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMethod = null
                                        featherStrength = 0.5f
                                        onReset()
                                    }
                                    .padding(vertical = 4.dp)
                            )
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
                                onClick = { onApply(selectedMethod, featherStrength) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp),
                                enabled = selectedMethod != null
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
