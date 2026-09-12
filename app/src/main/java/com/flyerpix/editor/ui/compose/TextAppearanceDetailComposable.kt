package com.flyerpix.editor.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType
import com.flyerpix.editor.ui.dialog.RecentEntry

private val TextAppearanceColors = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    background = Color.White,
    onBackground = Color(0xFF1A1A2E)
)

private val AppearanceSecondary = Color(0xFF5F6B7A)
private val AppearanceAlt = Color(0xFFF4F6FA)
private val AppearanceDivider = Color(0xFFE4E8F0)
private val AppearanceHandle = Color(0xFFD0D4DE)

@Composable
private fun AppearanceSheet(
    maxHeightPx: Int,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    MaterialTheme(colors = TextAppearanceColors) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { maxHeightPx.toDp() }),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                elevation = 8.dp,
                backgroundColor = Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(AppearanceHandle, RoundedCornerShape(50))
                    )
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            content = content
                        )
                        Column(
                            modifier = Modifier.width(60.dp).padding(start = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
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
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text("Apply", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = AppearanceSecondary,
        modifier = Modifier.width(58.dp)
    )
}

@Composable
private fun AppearanceRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun RecentChip(entry: RecentEntry, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    val brush = when (entry) {
        is RecentEntry.Solid -> Brush.linearGradient(listOf(Color(entry.color), Color(entry.color)))
        is RecentEntry.Gradient -> Brush.linearGradient(entry.gradient.colors.map(::Color))
    }
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 30.dp)
            .clip(shape)
            .background(brush)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colors.primary else AppearanceDivider, shape)
            .clickable(onClick = onClick)
    )
}

@Composable
fun TextColorDetailPage(
    color: Int,
    currentGradient: GradientColor?,
    recents: List<RecentEntry>,
    onRecentPicked: (RecentEntry) -> Unit,
    onPickColor: () -> Unit,
    onOpenGradient: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int
) {
    AppearanceSheet(maxHeightPx, onApply, onCancel) {
        AppearanceRow {
            AppearanceLabel("Color")
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(Color(color))
                    .border(1.dp, AppearanceDivider, CircleShape)
            )
            Text(String.format("#%08X", color), color = AppearanceSecondary, style = MaterialTheme.typography.caption, modifier = Modifier.weight(1f).padding(start = 8.dp))
            IconButton(onClick = onPickColor, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Pick color", tint = MaterialTheme.colors.primary)
            }
        }
        AppearanceRow {
            AppearanceLabel("Recent")
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                recents.forEach { entry ->
                    val selected = when (entry) {
                        is RecentEntry.Solid -> entry.color == color
                        is RecentEntry.Gradient -> gradientsEqual(entry.gradient, currentGradient)
                    }
                    RecentChip(entry, selected) { onRecentPicked(entry) }
                }
            }
        }
        AppearanceRow {
            AppearanceLabel("Fill")
            Button(onClick = onPickColor, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                Text("Pick Color", style = MaterialTheme.typography.caption)
            }
            Spacer(Modifier.width(6.dp))
            Button(onClick = onOpenGradient, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                Text("Gradient", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun TextGradientDetailPage(
    enabled: Boolean,
    gradient: GradientColor?,
    onEnabledChange: (Boolean) -> Unit,
    onTypeChange: (GradientType) -> Unit,
    onPresetPicked: (GradientColor) -> Unit,
    onAngleChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int
) {
    var angle by remember(gradient?.angle) { mutableStateOf(gradient?.angle ?: 0f) }
    AppearanceSheet(maxHeightPx, onApply, onCancel) {
        AppearanceRow {
            AppearanceLabel("Gradient")
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) { Text("Reset", style = MaterialTheme.typography.caption) }
        }
        if (enabled) {
            AppearanceRow {
                AppearanceLabel("Type")
                Row(modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(AppearanceAlt).padding(2.dp)) {
                    GradientTypeButton("Linear", gradient?.type == GradientType.LINEAR) { onTypeChange(GradientType.LINEAR) }
                    GradientTypeButton("Radial", gradient?.type == GradientType.RADIAL) { onTypeChange(GradientType.RADIAL) }
                    GradientTypeButton("Sweep", gradient?.type == GradientType.SWEEP) { onTypeChange(GradientType.SWEEP) }
                }
            }
            AppearanceRow {
                AppearanceLabel("Presets")
                Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GradientColor.PRESETS.forEach { preset ->
                        RecentChip(preset.toRecentEntry(), gradientsEqual(preset, gradient)) { onPresetPicked(preset) }
                    }
                }
            }
            if (gradient?.type == GradientType.LINEAR) {
                AppearanceRow {
                    AppearanceLabel("Angle")
                    Slider(
                        value = angle,
                        onValueChange = { angle = it; onAngleChange(it) },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f).height(30.dp)
                    )
                    Text("${angle.toInt()}°", style = MaterialTheme.typography.caption, color = AppearanceSecondary, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun RowScope.GradientTypeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) Color.White else Color.Transparent).clickable(onClick = onClick).padding(vertical = 6.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.caption,
        color = if (selected) MaterialTheme.colors.primary else AppearanceSecondary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
    )
}

@Composable
fun TextTextureDetailPage(
    title: String = "Texture",
    enabled: Boolean,
    bitmap: Bitmap?,
    scale: Float,
    rotation: Float,
    onEnabledChange: (Boolean) -> Unit,
    onChoose: () -> Unit,
    onRemove: () -> Unit,
    onScaleChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int
) {
    var scaleState by remember(scale) { mutableStateOf(scale) }
    var rotationState by remember(rotation) { mutableStateOf(rotation) }
    AppearanceSheet(maxHeightPx, onApply, onCancel) {
        AppearanceRow {
            AppearanceLabel(title)
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onReset) { Text("Reset", style = MaterialTheme.typography.caption) }
        }
        if (enabled) {
            AppearanceRow {
                if (bitmap != null && !bitmap.isRecycled) {
                    androidx.compose.foundation.Image(bitmap.asImageBitmap(), contentDescription = "Texture preview", modifier = Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)))
                } else {
                    Box(modifier = Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)).background(AppearanceAlt), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "No texture", tint = AppearanceSecondary)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Button(onClick = onChoose, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (bitmap == null) "Choose Photo" else "Change Photo", style = MaterialTheme.typography.caption)
                }
                if (bitmap != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove texture", tint = Color(0xFFD32F2F))
                    }
                }
            }
            AppearanceRow {
                AppearanceLabel("Scale")
                Slider(value = scaleState, onValueChange = { scaleState = it; onScaleChange(it) }, valueRange = 0.1f..3f, modifier = Modifier.weight(1f).height(30.dp))
                Text("${(scaleState * 100).toInt()}%", style = MaterialTheme.typography.caption, color = AppearanceSecondary, modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
            }
            AppearanceRow {
                AppearanceLabel("Rotation")
                Slider(value = rotationState, onValueChange = { rotationState = it; onRotationChange(it) }, valueRange = 0f..360f, modifier = Modifier.weight(1f).height(30.dp))
                Text("${rotationState.toInt()}°", style = MaterialTheme.typography.caption, color = AppearanceSecondary, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
fun TextStyleDetailPage(
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    strikeThrough: Boolean,
    weight: Int,
    onBoldChange: () -> Unit,
    onItalicChange: () -> Unit,
    onUnderlineChange: () -> Unit,
    onStrikeThroughChange: () -> Unit,
    onWeightChange: (Int) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int
) {
    val weights = listOf(100 to "Thin", 300 to "Light", 400 to "Regular", 500 to "Medium", 600 to "SemiBold", 700 to "Bold", 800 to "ExtraBold", 900 to "Black")
    AppearanceSheet(maxHeightPx, onApply, onCancel) {
        AppearanceRow {
            AppearanceLabel("Style")
            StyleToggle("B", bold, onBoldChange)
            StyleToggle("I", italic, onItalicChange)
            StyleToggle("U", underline, onUnderlineChange)
            StyleToggle("S", strikeThrough, onStrikeThroughChange)
            TextButton(onClick = onReset) { Text("Reset", style = MaterialTheme.typography.caption) }
        }
        AppearanceRow {
            AppearanceLabel("Weight")
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weights.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { (value, label) ->
                            StyleWeight(label, weight == value) { onWeightChange(value) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StyleToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else AppearanceAlt)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else AppearanceSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RowScope.StyleWeight(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colors.primary else AppearanceAlt)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.caption, color = if (selected) Color.White else AppearanceSecondary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
fun TextCurveDetailPage(
    curvePercent: Int,
    onCurvePercentChange: (Int) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int
) {
    var curveState by remember(curvePercent) { mutableStateOf(curvePercent.coerceIn(-100, 100)) }
    fun label(value: Int) = when {
        value == 0 -> "0% (Flat)"
        value > 0 -> "+$value%"
        else -> "$value%"
    }
    AppearanceSheet(maxHeightPx, onApply, onCancel) {
        AppearanceRow {
            AppearanceLabel("Curve")
            Slider(
                value = curveState.toFloat(),
                onValueChange = { value -> curveState = value.toInt(); onCurvePercentChange(curveState) },
                valueRange = -100f..100f,
                steps = 199,
                modifier = Modifier.weight(1f).height(30.dp)
            )
            Text(label(curveState), style = MaterialTheme.typography.caption, color = AppearanceSecondary, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            TextButton(onClick = { curveState = 0; onReset() }) { Text("Reset", style = MaterialTheme.typography.caption) }
        }
        AppearanceRow {
            AppearanceLabel("Presets")
            listOf(-100, -50, 0, 50, 100).forEach { preset ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (curveState == preset) MaterialTheme.colors.primary else AppearanceAlt)
                        .clickable { curveState = preset; onCurvePercentChange(preset) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (preset == 0) "Flat" else "${if (preset > 0) "+" else ""}$preset%", style = MaterialTheme.typography.caption, color = if (curveState == preset) Color.White else AppearanceSecondary)
                }
            }
        }
    }
}

private fun GradientColor.toRecentEntry(): RecentEntry.Gradient = RecentEntry.Gradient(this)

private fun gradientsEqual(a: GradientColor?, b: GradientColor?): Boolean =
    a != null && b != null && a.colors.contentEquals(b.colors) && a.type == b.type && a.angle == b.angle
