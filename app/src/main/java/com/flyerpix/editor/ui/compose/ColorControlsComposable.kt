package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.R

/**
 * Lightweight Compose POC for Color Controls.
 * - Shows preview + hex
 * - Horizontal swatch palette
 * - Buttons to open full color picker / gradient
 */
@Composable
fun ColorControls(
    initialColor: Int = 0xFFFFFFFF.toInt(),
    onColorSelected: (Int) -> Unit = {},
    onOpenColorPicker: () -> Unit = {},
    onOpenGradient: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val selected = remember { mutableStateOf(initialColor) }

    Surface(color = MaterialTheme.colors.surface) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(28.dp)
                    .background(color = Color(selected.value), shape = CircleShape))

                Spacer(modifier = Modifier.width(8.dp))

                Text(text = String.format("#%08X", selected.value), modifier = Modifier.weight(1f))

                Button(onClick = { onOpenColorPicker() }, modifier = Modifier.height(36.dp)) {
                    Icon(painter = painterResource(id = R.drawable.ic_sharp_palette_24px), contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Pick", style = MaterialTheme.typography.button)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(onClick = { onOpenGradient() }, modifier = Modifier.height(36.dp)) {
                    Text(text = "Gradient")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val scroll = rememberScrollState()
            Row(modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scroll)) {
                val palette = listOf(
                    0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF9E9E9E.toInt(), 0xFFD32F2F.toInt(),
                    0xFFF57C00.toInt(), 0xFFFBC02D.toInt(), 0xFF388E3C.toInt(), 0xFF0288D1.toInt(),
                    0xFF1976D2.toInt(), 0xFF7B1FA2.toInt(), 0xFFC2185B.toInt(), 0xFF607D8B.toInt()
                )
                for (c in palette) {
                    Box(modifier = Modifier
                        .size(36.dp)
                        .padding(end = 8.dp)
                        .background(color = Color(c), shape = CircleShape)
                        .clickable {
                            selected.value = c
                            onColorSelected(c)
                        })
                }
            }
        }
    }
}
