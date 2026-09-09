package com.flyerpix.editor.ui.compose

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
// Using simple text buttons for arrows/visibility/lock to avoid extra icon deps
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp

data class LayerItem(
    val id: String,
    val title: String,
    val isVisible: Boolean,
    val isLocked: Boolean
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LayerList(
    items: List<LayerItem>,
    onMoveUp: (LayerItem) -> Unit,
    onMoveDown: (LayerItem) -> Unit,
    onToggleVisibility: (LayerItem) -> Unit,
    onToggleLock: (LayerItem) -> Unit,
    onSelect: (LayerItem) -> Unit,
    onDelete: (LayerItem) -> Unit
) {
    val state = rememberLazyListState()
    LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
            Card(modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .animateItemPlacement()
                .clickable { onSelect(item) }
            ) {
                Row(modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (item.isVisible) "Visible" else "Hidden", style = MaterialTheme.typography.caption)
                            Spacer(Modifier.width(8.dp))
                            Text(if (item.isLocked) "Locked" else "Unlocked", style = MaterialTheme.typography.caption)
                        }
                    }
                    Row {
                        TextButton(onClick = { onMoveUp(item) }) { Text("↑") }
                        TextButton(onClick = { onMoveDown(item) }) { Text("↓") }
                        TextButton(onClick = { onToggleVisibility(item) }) { Text(if (item.isVisible) "👁" else "🚫") }
                        TextButton(onClick = { onToggleLock(item) }) { Text(if (item.isLocked) "🔒" else "🔓") }
                        TextButton(onClick = { onDelete(item) }) { Text("🗑") }
                    }
                }
            }
        }
    }
}
