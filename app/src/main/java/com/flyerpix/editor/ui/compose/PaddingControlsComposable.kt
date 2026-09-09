package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PaddingControls(
    linked: Boolean = true,
    top: Float = 0f,
    bottom: Float = 0f,
    left: Float = 0f,
    right: Float = 0f,
    onLinkedChanged: (Boolean) -> Unit = {},
    onTopChanged: (Float) -> Unit = {},
    onBottomChanged: (Float) -> Unit = {},
    onLeftChanged: (Float) -> Unit = {},
    onRightChanged: (Float) -> Unit = {},
    onApplyAll: (Float) -> Unit = {},
    onReset: () -> Unit = {}
) {
    var linkedState by remember { mutableStateOf(linked) }
    var topState by remember { mutableStateOf(top) }
    var bottomState by remember { mutableStateOf(bottom) }
    var leftState by remember { mutableStateOf(left) }
    var rightState by remember { mutableStateOf(right) }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Text Padding", style = MaterialTheme.typography.subtitle1)
            Row {
                Text(text = "Connected", style = MaterialTheme.typography.body2)
                Switch(checked = linkedState, onCheckedChange = {
                    linkedState = it
                    onLinkedChanged(it)
                })
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onReset(); topState = 0f; bottomState = 0f; leftState = 0f; rightState = 0f }) { Text("Reset") }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Top & Bottom
        Text(text = "Top: ${topState.toInt()} px")
        Slider(value = topState, onValueChange = {
            topState = it
            if (linkedState) {
                bottomState = it
                onApplyAll(it)
            } else {
                onTopChanged(it)
            }
        }, valueRange = 0f..200f)

        Text(text = "Bottom: ${bottomState.toInt()} px")
        Slider(value = bottomState, onValueChange = {
            bottomState = it
            if (linkedState) {
                topState = it
                onApplyAll(it)
            } else {
                onBottomChanged(it)
            }
        }, valueRange = 0f..200f)

        Spacer(modifier = Modifier.height(8.dp))

        // Left & Right
        Text(text = "Left: ${leftState.toInt()} px")
        Slider(value = leftState, onValueChange = {
            leftState = it
            if (linkedState) {
                rightState = it
                onApplyAll(it)
            } else {
                onLeftChanged(it)
            }
        }, valueRange = 0f..200f)

        Text(text = "Right: ${rightState.toInt()} px")
        Slider(value = rightState, onValueChange = {
            rightState = it
            if (linkedState) {
                leftState = it
                onApplyAll(it)
            } else {
                onRightChanged(it)
            }
        }, valueRange = 0f..200f)
    }
}
