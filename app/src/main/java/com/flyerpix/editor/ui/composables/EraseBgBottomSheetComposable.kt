package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flyerpix.editor.R

/**
 * Panel kontrol bawah layar untuk fitur Erase BG.
 *
 * - Mode Manual Erase: usap untuk menghapus background.
 * - Mode Auto Color: tap warna untuk hapus instan area sewarna.
 * - Mode Restore: kembalikan area foto yang tidak sengaja terhapus.
 *
 * Slider:
 * - Ukuran Penghapus (brushSize)
 * - Jarak Jari / Offset (fingerOffsetDp)
 * - Toleransi Warna / Threshold (hanya saat Auto Color)
 *
 * Color scheme & layout diambil IDENTIK dengan menu Gradient
 * (GradientDetailPage): light theme, handler bar, kolom aksi kanan
 * Batal/Selesai, chip segment dgn PanelChipBg, dan tinggi sheet sama.
 */
@Composable
fun EraseBgBottomSheetComposable(
    onBrushSizeChange: (Float) -> Unit,
    onFingerOffsetChange: (Float) -> Unit,
    onModeChange: (EraseBgMode) -> Unit,
    onAutoColorThresholdChange: (Int) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
    maxHeightPx: Int = 420
) {
    var brushSize by remember { mutableStateOf(40f) }
    var fingerOffsetDp by remember { mutableStateOf(55f) }
    var autoColorThreshold by remember { mutableStateOf(30) }
    var activeMode by remember { mutableStateOf(EraseBgMode.MANUAL) }

    LaunchedEffect(Unit) {
        onBrushSizeChange(brushSize)
        onFingerOffsetChange(fingerOffsetDp)
        onModeChange(activeMode)
        onAutoColorThresholdChange(autoColorThreshold)
    }

    MaterialTheme(colors = EraseBgColorScheme) {
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
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(4.dp)
                            .background(Color(EraseBgPanelHandle), RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(color = Color(EraseBgPanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            // ── Mode selector (chip gaya Gradient "Type") ─────────────
                            Text(
                                text = "Mode",
                                style = MaterialTheme.typography.caption,
                                color = Color(EraseBgPanelTextSecondary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EraseBgMode.values().forEach { mode ->
                                    val selected = activeMode == mode
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.caption,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color(EraseBgPanelTextSecondary),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected) MaterialTheme.colors.primary else Color(EraseBgPanelChipBg),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                activeMode = mode
                                                onModeChange(mode)
                                            }
                                            .padding(vertical = 6.dp)
                                    )
                                }
                            }

                            // ── Undo / Redo (link caption gaya Gradient "Reset") ───────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Undo",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable(onClick = onUndo)
                                        .padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "Redo",
                                    style = MaterialTheme.typography.caption,
                                    color = MaterialTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable(onClick = onRedo)
                                        .padding(vertical = 4.dp)
                                )
                            }

                            // ── Ukuran Penghapus (baris slider gaya Gradient "Angle") ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ukuran",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(EraseBgPanelTextSecondary),
                                    modifier = Modifier.width(84.dp)
                                )
                                Slider(
                                    value = brushSize,
                                    onValueChange = {
                                        brushSize = it
                                        onBrushSizeChange(it)
                                    },
                                    valueRange = 10f..120f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colors.primary,
                                        activeTrackColor = MaterialTheme.colors.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                Text(
                                    text = "${brushSize.toInt()}",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(EraseBgPanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(36.dp)
                                )
                            }

                            // ── Jarak Jari / Offset ─────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Jarak Jari",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(EraseBgPanelTextSecondary),
                                    modifier = Modifier.width(84.dp)
                                )
                                Slider(
                                    value = fingerOffsetDp,
                                    onValueChange = {
                                        fingerOffsetDp = it
                                        onFingerOffsetChange(it)
                                    },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colors.primary,
                                        activeTrackColor = MaterialTheme.colors.primary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                )
                                Text(
                                    text = "↑${fingerOffsetDp.toInt()}",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(EraseBgPanelTextSecondary),
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(44.dp)
                                )
                            }

                            // ── Toleransi Warna (hanya saat Auto Color) ─────────────
                            if (activeMode == EraseBgMode.AUTO_COLOR) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Toleransi",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(EraseBgPanelTextSecondary),
                                        modifier = Modifier.width(84.dp)
                                    )
                                    Slider(
                                        value = autoColorThreshold.toFloat(),
                                        onValueChange = {
                                            autoColorThreshold = it.toInt()
                                            onAutoColorThresholdChange(it.toInt())
                                        },
                                        valueRange = 5f..120f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colors.primary,
                                            activeTrackColor = MaterialTheme.colors.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                    )
                                    Text(
                                        text = "$autoColorThreshold",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(EraseBgPanelTextSecondary),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                                Text(
                                    text = "Lower = stricter color matching | Higher = more forgiving",
                                    fontSize = 10.sp,
                                    color = Color(EraseBgPanelTextSecondary)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .padding(start = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Text("Batal", style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = onFinish,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Text(
                                    text = "Selesai",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Color scheme identik dengan GradientDetailPage (menu Gradient). */
private val EraseBgColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val EraseBgPanelTextSecondary = 0xFF5F6B7A
private const val EraseBgPanelDivider = 0xFFE4E8F0
private const val EraseBgPanelHandle = 0xFFD0D4DE
private const val EraseBgPanelChipBg = 0xFFF1F4FA

/** Mode penghapusan yang dapat dipilih pengguna. */
enum class EraseBgMode(val label: String, val iconRes: Int) {
    MANUAL("Manual", R.drawable.ic_eraser_24px),
    AUTO_COLOR("Auto Warna", R.drawable.ic_sharp_palette_24px),
    RESTORE("Pulihkan", R.drawable.ic_sharp_brush_24px)
}