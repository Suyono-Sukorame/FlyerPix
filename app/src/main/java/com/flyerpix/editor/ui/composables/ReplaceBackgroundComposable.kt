package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.CanvasBackground
import com.flyerpix.editor.canvas.model.CanvasBackgroundMode
import com.flyerpix.editor.canvas.model.GradientColor
import com.flyerpix.editor.canvas.model.GradientType

@Composable
fun ReplaceBackgroundComposable(
    currentBackground: CanvasBackground,
    onBackgroundChange: (CanvasBackground) -> Unit,
    onColorMatchToggle: (Boolean) -> Unit,
    onGalleryClick: () -> Unit,
    onClose: () -> Unit,
    autoColorMatchEnabled: Boolean = false
) {
    var selectedMode by remember { mutableStateOf(currentBackground.mode) }
    var autoColorMatch by remember { mutableStateOf(autoColorMatchEnabled) }
    var selectedSolidColor by remember { mutableStateOf(currentBackground.solidColor) }
    var selectedGradient by remember { mutableStateOf(currentBackground.gradient ?: GradientColor(colors = intArrayOf(0xFF0066FF.toInt(), 0xFF00AAFF.toInt()), type = GradientType.LINEAR)) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Replace Background", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            }
        }

        item {
            Text("Background Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB), modifier = Modifier.padding(top = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))
            
            val selectedColor = if (selectedMode == CanvasBackgroundMode.SOLID_COLOR) Color(0xFF0066FF) else Color(0xFF333333)
            val selectedColor2 = if (selectedMode == CanvasBackgroundMode.GRADIENT) Color(0xFF0066FF) else Color(0xFF333333)
            val selectedColor3 = if (selectedMode == CanvasBackgroundMode.IMAGE) Color(0xFF0066FF) else Color(0xFF333333)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { selectedMode = CanvasBackgroundMode.SOLID_COLOR }, modifier = Modifier.height(40.dp).weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = selectedColor)) {
                    Text("Solid", fontSize = 13.sp)
                }
                Button(onClick = { selectedMode = CanvasBackgroundMode.GRADIENT }, modifier = Modifier.height(40.dp).weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = selectedColor2)) {
                    Text("Gradient", fontSize = 13.sp)
                }
                Button(onClick = { selectedMode = CanvasBackgroundMode.IMAGE }, modifier = Modifier.height(40.dp).weight(1f), colors = ButtonDefaults.buttonColors(backgroundColor = selectedColor3)) {
                    Text("Gallery", fontSize = 13.sp)
                }
            }
        }

        if (selectedMode == CanvasBackgroundMode.SOLID_COLOR) {
            item {
                Text("Color Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB), modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                val presets = listOf(0xFFFFFFFF.toInt() to "White", 0xFF000000.toInt() to "Black", 0xFF666666.toInt() to "Gray", 0xFFFF6B6B.toInt() to "Red", 0xFF4ECDC4.toInt() to "Teal", 0xFFFFE66D.toInt() to "Yellow")
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in presets.indices step 3) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (j in 0..2) {
                                if (i + j < presets.size) {
                                    val (color, name) = presets[i + j]
                                    Column(modifier = Modifier.weight(1f).clickable { selectedSolidColor = color; onBackgroundChange(CanvasBackground.solid(color)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(color)).then(if (selectedSolidColor == color) Modifier.border(2.dp, Color(0xFF00FF00), CircleShape) else Modifier))
                                        Text(name, fontSize = 10.sp, color = Color(0xFFBBBBBB))
                                    }
                                } else { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        if (selectedMode == CanvasBackgroundMode.GRADIENT) {
            item {
                Text("Gradient Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB), modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { selectedGradient = GradientColor(colors = intArrayOf(0xFFFF6B35.toInt(), 0xFFFFA500.toInt()), type = GradientType.LINEAR); onBackgroundChange(CanvasBackground.gradient(selectedGradient)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFFF6B35), Color(0xFFFFA500))))); Text("Sunset", fontSize = 14.sp, color = Color.White) }
                    }
                    Button(onClick = { selectedGradient = GradientColor(colors = intArrayOf(0xFF667EEA.toInt(), 0xFF764BA2.toInt()), type = GradientType.LINEAR); onBackgroundChange(CanvasBackground.gradient(selectedGradient)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))); Text("Ocean", fontSize = 14.sp, color = Color.White) }
                    }
                    Button(onClick = { selectedGradient = GradientColor(colors = intArrayOf(0xFF84FAFF.toInt(), 0xFF00FFC6.toInt()), type = GradientType.LINEAR); onBackgroundChange(CanvasBackground.gradient(selectedGradient)) }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF84FAFF), Color(0xFF00FFC6))))); Text("Lime", fontSize = 14.sp, color = Color.White) }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Auto Color Match", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White); Text("Adjust layer colors", fontSize = 11.sp, color = Color(0xFF999999)) }
                    Switch(checked = autoColorMatch, onCheckedChange = { autoColorMatch = it; onColorMatchToggle(it) })
                }
            }
        }

        item {
            Button(onClick = { onClose() }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0066FF))) {
                Text("Apply", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}