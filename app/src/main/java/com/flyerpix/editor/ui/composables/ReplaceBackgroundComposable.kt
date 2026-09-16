package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType

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

private val ReplaceSolidPresetColors = intArrayOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFF212121.toInt(),
    0xFFB0BEC5.toInt(),
    0xFF00E5FF.toInt(),
    0xFF00B0FF.toInt(),
    0xFF2979FF.toInt(),
    0xFF651FFF.toInt(),
    0xFFAA00FF.toInt(),
    0xFFF50057.toInt(),
    0xFFE53935.toInt(),
    0xFFFF6D00.toInt(),
    0xFFFFD600.toInt(),
    0xFF00E676.toInt(),
    0xFF00BFA5.toInt(),
    0xFFFFF9C4.toInt(),
    0xFFBBDEFB.toInt(),
    0xFFC8E6C9.toInt(),
    0xFFFFCCBC.toInt()
)

private val ReplaceGradientPresets = listOf(
    Triple("Sunset", intArrayOf(0xFFFF6B35.toInt(), 0xFFFFA500.toInt()), listOf(Color(0xFFFF6B35), Color(0xFFFFA500))),
    Triple("Ocean", intArrayOf(0xFF667EEA.toInt(), 0xFF764BA2.toInt()), listOf(Color(0xFF667EEA), Color(0xFF764BA2))),
    Triple("Lime", intArrayOf(0xFF84FAFF.toInt(), 0xFF00FFC6.toInt()), listOf(Color(0xFF84FAFF), Color(0xFF00FFC6))),
    Triple("Fire", intArrayOf(0xFFE53935.toInt(), 0xFFFF6D00.toInt()), listOf(Color(0xFFE53935), Color(0xFFFF6D00))),
    Triple("Sky", intArrayOf(0xFF2979FF.toInt(), 0xFF00E5FF.toInt()), listOf(Color(0xFF2979FF), Color(0xFF00E5FF))),
    Triple("Purple", intArrayOf(0xFF651FFF.toInt(), 0xFFF50057.toInt()), listOf(Color(0xFF651FFF), Color(0xFFF50057)))
)

@Composable
fun ReplaceBackgroundComposable(
    currentBackground: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
    onColorMatchToggle: (Boolean) -> Unit,
    onGalleryClick: () -> Unit,
    onReset: () -> Unit,
    onApply: (autoColorMatch: Boolean, blendToBackground: Boolean) -> Unit,
    onCancel: () -> Unit,
    autoColorMatchEnabled: Boolean = false,
    maxHeightPx: Int = 420,
    sessionKey: Int = 0
) {
    var selectedMode by remember { mutableStateOf(currentBackground.mode) }
    var autoColorMatch by remember { mutableStateOf(autoColorMatchEnabled) }
    var blendToBackground by remember { mutableStateOf(false) }
    var selectedSolidColor by remember { mutableStateOf(currentBackground.solidColor) }
    var selectedGradient by remember {
        mutableStateOf(
            currentBackground.gradient ?: GradientColor.PRESETS.first()
        )
    }
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val modes = listOf(
                                    CanvasBackgroundMode.SOLID_COLOR to "Solid",
                                    CanvasBackgroundMode.GRADIENT to "Gradient",
                                    CanvasBackgroundMode.IMAGE to "Gallery"
                                )
                                for ((m, label) in modes) {
                                    val isSelected = selectedMode == m
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colors.primary
                                                else Color(PanelAlt)
                                            )
                                            .clickable {
                                                selectedMode = m
                                                if (m == CanvasBackgroundMode.IMAGE) {
                                                    onGalleryClick()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(PanelTextSecondary)
                                        )
                                    }
                                }
                            }

                            when (selectedMode) {
                                CanvasBackgroundMode.TRANSPARENT -> {}
                                CanvasBackgroundMode.SOLID_COLOR -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE8EEF5))
                                                .border(1.dp, Color(0xFFCCD6E0), CircleShape)
                                                .clickable { },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Custom Color",
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))

                                        ReplaceSolidPresetColors.forEach { c ->
                                            val isSelected = (selectedSolidColor and 0x00FFFFFF) == (c and 0x00FFFFFF)
                                            val isLight = (c == 0xFFFFFFFF.toInt() || c == 0xFFFFF9C4.toInt())
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(c))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else if (isLight) 1.dp else 0.dp,
                                                        color = if (isSelected) MaterialTheme.colors.primary
                                                        else if (isLight) Color(0xFFCCD6E0)
                                                        else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedSolidColor = c
                                                        onBackgroundChange(CanvasBackground.solid(c))
                                                    }
                                            )
                                        }
                                    }
                                }

                                CanvasBackgroundMode.GRADIENT -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE8EEF5))
                                                .border(1.dp, Color(0xFFCCD6E0), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Custom Gradient",
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))

                                        ReplaceGradientPresets.forEach { (name, colors, brushColors) ->
                                            val isSel = selectedGradient.colors.contentEquals(colors)
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.horizontalGradient(brushColors))
                                                    .border(
                                                        width = if (isSel) 2.5.dp else 1.dp,
                                                        color = if (isSel) MaterialTheme.colors.primary else Color(0x33000000),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        val grad = GradientColor(
                                                            colors = colors,
                                                            type = GradientType.LINEAR
                                                        )
                                                        selectedGradient = grad
                                                        onBackgroundChange(CanvasBackground.gradient(grad))
                                                    }
                                            )
                                        }
                                    }
                                }

                                CanvasBackgroundMode.IMAGE -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .border(1.dp, Color(PanelDivider), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(
                                                    com.flyerpix.editor.R.drawable.ic_outline_photo_24px
                                                ),
                                                contentDescription = null,
                                                tint = MaterialTheme.colors.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "Pick from Gallery",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colors.onSurface
                                                )
                                                Text(
                                                    text = "Select an image to use as canvas background.",
                                                    fontSize = 10.sp,
                                                    color = Color(PanelTextSecondary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Divider(color = Color(PanelDivider), thickness = 0.5.dp)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Auto Color Match",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            "Adjust selected layer colors to match new background",
                                            fontSize = 10.sp,
                                            color = Color(PanelTextSecondary)
                                        )
                                    }
                                    Switch(
                                        checked = autoColorMatch,
                                        onCheckedChange = {
                                            autoColorMatch = it
                                            onColorMatchToggle(it)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colors.primary,
                                            checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                backgroundColor = Color(0xFFF8FAFC),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Blend to Background",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colors.onSurface
                                        )
                                        Text(
                                            "Automatic gradient bottom-to-top mask on selected layer",
                                            fontSize = 10.sp,
                                            color = Color(PanelTextSecondary)
                                        )
                                    }
                                    Switch(
                                        checked = blendToBackground,
                                        onCheckedChange = { blendToBackground = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colors.primary,
                                            checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.4f)
                                        )
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
                                        selectedMode = CanvasBackgroundMode.SOLID_COLOR
                                        selectedSolidColor = 0xFFFFFFFF.toInt()
                                        autoColorMatch = false
                                        blendToBackground = false
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
                                Icon(
                                    painter = painterResource(R.drawable.ic_sharp_clear_24px),
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(PanelTextSecondary)
                                )
                            }
                            Button(
                                onClick = { onApply(autoColorMatch, blendToBackground) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = "Apply",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
