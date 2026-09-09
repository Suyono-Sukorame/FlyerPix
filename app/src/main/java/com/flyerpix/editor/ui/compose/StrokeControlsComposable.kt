package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StrokeControls(
    initialEnabled: Boolean = false,
    initialWidth: Float = 4f,
    initialOpacityPct: Float = 100f,
    initialColor: Int = 0xFF000000.toInt(),
    onEnabledChanged: (Boolean) -> Unit = {},
    onWidthChanged: (Float) -> Unit = {},
    onOpacityChanged: (Float) -> Unit = {},
    onPickColor: () -> Unit = {},
    onReset: () -> Unit = {}
) {
    var enabled by remember { mutableStateOf(initialEnabled) }
    var width by remember { mutableStateOf(initialWidth) }
    var opacity by remember { mutableStateOf(initialOpacityPct) }

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                onEnabledChanged(it)
            })
            Spacer(Modifier.width(8.dp))
            Text(text = "Outline / Stroke", modifier = Modifier.weight(1f))
            TextButton(onClick = { onReset() }) { Text("Reset") }
        }

        if (enabled) {
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(28.dp)
                    .background(color = Color(initialColor), shape = CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(text = String.format("#%08X", initialColor), modifier = Modifier.weight(1f))
                Button(onClick = { onPickColor() }) { Text("Pick") }
            }

            Spacer(Modifier.height(10.dp))

            Text(text = String.format("Width: %.1f px", width))
            Slider(value = width, onValueChange = {
                width = it
                onWidthChanged(it)
            }, valueRange = 0f..60f)

            Spacer(Modifier.height(8.dp))

            Text(text = "Opacity: ${opacity.toInt()}%")
            Slider(value = opacity, onValueChange = {
                opacity = it
                onOpacityChanged(it)
            }, valueRange = 0f..100f)
        }
    }
}
