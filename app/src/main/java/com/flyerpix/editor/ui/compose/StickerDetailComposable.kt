package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.StickerCategory
import com.flyerpix.editor.canvas.model.StickerItem
import com.flyerpix.editor.ui.TabSticker

private val PrimaryBlue = Color(0xFF1769FF)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val SurfaceBg = Color(0xFFFFFFFF)
private val DragHandleColor = Color(0xFFCBD5E1)
private val ChipSelectedBg = Color(0xFFE8F0FE)
private val ChipUnselectedBg = Color(0xFFF1F5F9)

@Composable
fun StickerDetailPage(
    onStickerSelected: (StickerItem) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 540
) {
    var selectedCategory by remember { mutableStateOf<StickerCategory?>(null) }
    var lastAddedEmoji by remember { mutableStateOf<String?>(null) }

    val allStickers = remember { TabSticker.STICKER_DATA }
    val filteredStickers = remember(selectedCategory) {
        if (selectedCategory == null) allStickers
        else allStickers.filter { it.category == selectedCategory }
    }

    val categories = remember {
        listOf<Pair<StickerCategory?, String>>(
            null to "All",
            StickerCategory.SMILEYS to "😊 Smileys",
            StickerCategory.ANIMALS to "🐶 Animals",
            StickerCategory.FOOD to "🍔 Food",
            StickerCategory.ACTIVITIES to "⚽ Activities",
            StickerCategory.TRAVEL to "🚗 Travel",
            StickerCategory.OBJECTS to "💡 Objects",
            StickerCategory.SYMBOLS to "❤️ Symbols"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { maxHeightPx.toDp() }),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = 8.dp,
            backgroundColor = SurfaceBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 6.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(DragHandleColor, CircleShape)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Category chips + 6-column Grid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sharp_face_24px),
                                    contentDescription = "Sticker",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Stickers & Emojis",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (lastAddedEmoji != null) {
                                Text(
                                    text = "Added $lastAddedEmoji",
                                    color = PrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Category Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { (cat, label) ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) ChipSelectedBg else ChipUnselectedBg)
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryBlue else TextSecondary
                                    )
                                }
                            }
                        }

                        Divider(
                            color = Color(0xFFF1F5F9),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Emoji Grid (6 columns)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredStickers, key = { it.emoji + it.label }) { item ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .clickable {
                                            lastAddedEmoji = item.emoji
                                            onStickerSelected(item)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.emoji,
                                        fontSize = 24.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Right Column: Cancel / Apply, matching the 3D Text sheet
                    Column(
                        modifier = Modifier
                            .width(62.dp)
                            .fillMaxHeight()
                            .padding(start = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text("Cancel", style = MaterialTheme.typography.caption)
                        }
                        Button(
                            onClick = onApply,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                            elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                        ) {
                            Text("Apply", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
