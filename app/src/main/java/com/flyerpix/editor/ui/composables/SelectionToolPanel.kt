package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.canvas.PixelCanvasView

@Composable
fun SelectionToolPanel(
    hasSelection: Boolean,
    onToolChanged: (PixelCanvasView.SelectionTool) -> Unit,
    onApplyToMask: (feather: Int, inverted: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var activeTool by remember { mutableStateOf<PixelCanvasView.SelectionTool?>(null) }
    var feather by remember { mutableFloatStateOf(0f) }
    var inverted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222222), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Selection", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Rectangle" to PixelCanvasView.SelectionTool.RECT,
                "Ellipse" to PixelCanvasView.SelectionTool.ELLIPSE,
                "Lasso" to PixelCanvasView.SelectionTool.LASSO
            ).forEach { (label, tool) ->
                Button(
                    onClick = {
                        android.util.Log.d("FlyerPixMask", "Tool button $label clicked")
                        activeTool = tool
                        onToolChanged(tool)
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (activeTool == tool) Color(0xFF0066FF) else Color(0xFF3A3A3A)
                    )
                ) { Text(label, fontSize = 12.sp, color = Color.White) }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Feather", fontSize = 13.sp, color = Color.White)
            Text("${feather.toInt()} px", fontSize = 12.sp, color = Color(0xFF999999))
        }
        Slider(
            value = feather,
            onValueChange = { feather = it },
            valueRange = 0f..100f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Invert Mask", fontSize = 13.sp, color = Color.White)
            Switch(checked = inverted, onCheckedChange = { inverted = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    activeTool = null
                    onCancel()
                },
                modifier = Modifier.weight(1f).height(44.dp)
            ) { Text("Batal", color = Color.White) }
            Button(
                onClick = { onApplyToMask(feather.toInt(), inverted) },
                modifier = Modifier.weight(1f).height(44.dp),
                enabled = hasSelection,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF0066FF))
            ) { Text("To Mask", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}