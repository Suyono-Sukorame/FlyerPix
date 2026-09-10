package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.ui.dialog.RecentEntry

@Composable
fun RecentColorChip(
    entry: RecentEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val fillModifier: Modifier = when (entry) {
        is RecentEntry.Solid -> Modifier.background(Color(entry.color))
        is RecentEntry.Gradient -> Modifier.background(
            Brush.linearGradient(colors = entry.gradient.colors.map { Color(it) })
        )
    }
    val borderColor = if (selected) MaterialTheme.colors.primary else Color(0xFFD0D4DE)
    val borderWidth = if (selected) 2.dp else 1.dp
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(fillModifier)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
    )
}