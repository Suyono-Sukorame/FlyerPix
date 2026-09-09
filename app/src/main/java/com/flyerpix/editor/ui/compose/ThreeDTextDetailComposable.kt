package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ThreeDTextDetailPage(
    depth: Int,
    depthColor: Long,
    angle: Float,
    onDepthChange: (Int) -> Unit,
    onDepthPickRequested: () -> Unit,
    onAngleChange: (Float) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    val cardColor = MaterialTheme.colors.surface

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = with(LocalDensity.current) { maxHeightPx.toDp() })
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = 8.dp,
            backgroundColor = cardColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(0xFFB8C2D1), RoundedCornerShape(50))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "3D Text",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onApply) { Text("Apply") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Depth: $depth", style = MaterialTheme.typography.body2)
                Slider(
                    value = depth.toFloat(),
                    onValueChange = { new -> onDepthChange(new.toInt()) },
                    valueRange = 1f..50f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "Angle: ${angle.toInt()}°", style = MaterialTheme.typography.body2)
                Slider(
                    value = angle,
                    onValueChange = onAngleChange,
                    valueRange = 0f..360f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Depth Color", modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(depthColor.toULong()), shape = RoundedCornerShape(17.dp))
                            .clickable { onDepthPickRequested() }
                    )
                }
            }
        }
    }
}
