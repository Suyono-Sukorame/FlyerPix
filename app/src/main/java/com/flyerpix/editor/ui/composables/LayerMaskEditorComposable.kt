package com.flyerpix.editor.ui.composables

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.model.CanvasLayer
import com.flyerpix.editor.canvas.model.GradientType

@Composable
fun LayerMaskEditorComposable(
    layer: CanvasLayer,
    onMaskChange: () -> Unit,
    onClose: () -> Unit,
    onGradientMaskApply: (GradientType, FloatArray) -> Unit,
    onFeatherApply: (Float) -> Unit,
    onBrushConfig: (brushSize: Float, brushOpacity: Int, color: Int, isEraser: Boolean) -> Unit,
    onEnableMaskPaint: () -> Unit,
    onFinish: () -> Unit
) {
    var brushSize by remember { mutableStateOf(20f) }
    var brushOpacity by remember { mutableStateOf(1f) }
    var isInverted by remember { mutableStateOf(layer.maskInverted) }
    var isEraser by remember { mutableStateOf(false) }
    var selectedBrushColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }

    fun fillColor(): Int = when (selectedBrushColor) {
        0xFF000000.toInt() -> 0x00000000.toInt()
        0xFF808080.toInt() -> 0xFF808080.toInt()
        else               -> 0xFFFFFFFF.toInt()
    }

    LaunchedEffect(Unit) {
        onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser)
        onEnableMaskPaint()
    }

    val hasMask = layer.hasMask()

    LazyColumn(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF1A1A1A)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Layer Mask Editor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            }
        }

        item {
            if (!hasMask) {
                Button(onClick = {
                    val (w, h) = layer.getUnwarpedDimensions()
                    if (w > 0 && h > 0) {
                        layer.createMask(w.toInt(), h.toInt())
                        onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser)
                        onEnableMaskPaint()
                        onMaskChange()
                    }
                }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0066FF))) {
                    Text("Create Mask", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mask Active", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Switch(checked = layer.maskEnabled, onCheckedChange = { layer.maskEnabled = it; onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser); onMaskChange() })
                    }
                }
            }
        }

        if (hasMask) {
            item {
                Text("Brush Tool", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Size: ${brushSize.toInt()}px", fontSize = 12.sp, color = Color(0xFF999999))
                Slider(value = brushSize, onValueChange = { brushSize = it; onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser) }, valueRange = 5f..100f)

                Text("Opacity: ${(brushOpacity * 100).toInt()}%", fontSize = 12.sp, color = Color(0xFF999999))
                Slider(value = brushOpacity, onValueChange = { brushOpacity = it; onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser) }, valueRange = 0f..1f)
            }

            item {
                Text("Brush Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        0xFFFFFFFF.toInt() to "White (Show)",
                        0xFF000000.toInt() to "Black (Hide)",
                        0xFF808080.toInt() to "Gray (Partial)"
                    ).forEach { (color, label) ->
                        Column(modifier = Modifier.weight(1f).clickable { selectedBrushColor = color; isEraser = false; onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(color)).then(
                                if (selectedBrushColor == color && !isEraser) Modifier.border(2.dp, Color(0xFF00FF00), CircleShape) else Modifier
                            ))
                            Text(label, fontSize = 10.sp, color = Color(0xFFBBBBBB))
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { isEraser = !isEraser; onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser) },
                        modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = if (isEraser) Color(0xFF0066FF) else Color(0xFF333333))) {
                        Text(if (isEraser) "Eraser On" else "Eraser Off", fontSize = 12.sp)
                    }
                }
            }

            item {
                Text("Mask Actions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { layer.maskInverted = !layer.maskInverted; isInverted = layer.maskInverted; onMaskChange() },
                        modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = if (isInverted) Color(0xFF0066FF) else Color(0xFF333333))) {
                        Text("Invert", fontSize = 12.sp)
                    }
                    Button(onClick = {
                        layer.maskBitmap?.let { bmp ->
                            val px = IntArray(bmp.width * bmp.height) { 0xFFFFFFFF.toInt() }
                            bmp.setPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
                        }
                        onMaskChange()
                    },
                        modifier = Modifier.weight(1f).height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF666666))) {
                        Text("Reset", fontSize = 12.sp)
                    }
                }
            }

            item {
                Text("Auto Masks", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFBBBBBB))
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onGradientMaskApply(GradientType.LINEAR, floatArrayOf(0f, 1f)) },
                        modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Text("Gradient Mask (Top->Bottom)", fontSize = 12.sp)
                    }
                    Button(onClick = { onGradientMaskApply(GradientType.RADIAL, floatArrayOf(0.5f, 0.5f, 0.5f)) },
                        modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Text("Gradient Mask (Radial)", fontSize = 12.sp)
                    }
                    Button(onClick = { onFeatherApply(10f) },
                        modifier = Modifier.fillMaxWidth().height(40.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333))) {
                        Text("Feather (10px)", fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    onBrushConfig(brushSize, (brushOpacity * 255).toInt().coerceIn(0, 255), fillColor(), isEraser)
                    onFinish()
                }, modifier = Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00CC66))) {
                    Text("Selesai", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
