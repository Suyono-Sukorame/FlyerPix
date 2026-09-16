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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
    val sheetCapDp = with(LocalDensity.current) { maxHeightPx.toDp() }
        .coerceAtMost((LocalConfiguration.current.screenHeightDp * 0.42f).dp)

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
                    .heightIn(max = sheetCapDp),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                elevation = 8.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(32.dp)
                            .height(3.dp)
                            .background(Color(EraseBgPanelHandle), RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Divider(color = Color(EraseBgPanelDivider), thickness = 0.5.dp)

                            // ── Mode selector (ikon: eraser / palette / brush) ────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                EraseBgMode.values().forEach { mode ->
                                    val selected = activeMode == mode
                                    EraseIconChip(
                                        iconRes = mode.iconRes,
                                        modifier = Modifier.weight(1f),
                                        active = selected,
                                        contentDescription = mode.name,
                                        onClick = {
                                            activeMode = mode
                                            onModeChange(mode)
                                        }
                                    )
                                }
                            }

                            // ── Undo / Redo (ikon) ───────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EraseIconChip(
                                    iconRes = R.drawable.ic_sharp_undo_24px,
                                    contentDescription = "Undo",
                                    onClick = onUndo
                                )
                                EraseIconChip(
                                    iconRes = R.drawable.ic_sharp_redo_24px,
                                    contentDescription = "Redo",
                                    onClick = onRedo
                                )
                            }

                            // ── Ukuran Penghapus ─────────────────────────────────────
                            EraseIconSliderRow(
                                iconRes = R.drawable.ic_size_24px,
                                iconDesc = "Ukuran",
                                value = brushSize,
                                onValueChange = {
                                    brushSize = it
                                    onBrushSizeChange(it)
                                },
                                valueRange = 10f..120f,
                                valueText = "${brushSize.toInt()}"
                            )

                            // ── Jarak Jari / Offset ─────────────────────────────────
                            EraseIconSliderRow(
                                iconRes = R.drawable.ic_move_pad_24px,
                                iconDesc = "Jarak Jari",
                                value = fingerOffsetDp,
                                onValueChange = {
                                    fingerOffsetDp = it
                                    onFingerOffsetChange(it)
                                },
                                valueRange = 0f..100f,
                                valueText = "↑${fingerOffsetDp.toInt()}",
                                valueWidth = 44.dp
                            )

                            // ── Toleransi Warna (hanya saat Auto Color) ─────────────
                            if (activeMode == EraseBgMode.AUTO_COLOR) {
                                EraseIconSliderRow(
                                    iconRes = R.drawable.ic_sharp_colorize_24px,
                                    iconDesc = "Toleransi",
                                    value = autoColorThreshold.toFloat(),
                                    onValueChange = {
                                        autoColorThreshold = it.toInt()
                                        onAutoColorThresholdChange(it.toInt())
                                    },
                                    valueRange = 5f..120f,
                                    valueText = "$autoColorThreshold"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .padding(start = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_sharp_clear_24px),
                                    contentDescription = "Cancel",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color(EraseBgPanelTextSecondary)
                                )
                            }
                            Button(
                                onClick = onFinish,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                elevation = ButtonDefaults.elevation(defaultElevation = 1.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24px),
                                    contentDescription = "Done",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
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
enum class EraseBgMode(val iconRes: Int) {
    MANUAL(R.drawable.ic_eraser_24px),
    AUTO_COLOR(R.drawable.ic_sharp_palette_24px),
    RESTORE(R.drawable.ic_sharp_brush_24px)
}

/** Chip ikon (tanpa teks) untuk mode & aksi. Aktif = primary. */
@Composable
private fun EraseIconChip(
    iconRes: Int,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colors.primary else Color(EraseBgPanelChipBg))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (active) Color.White else Color(EraseBgPanelTextSecondary),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Baris slider: ikon + slider + nilai angka. */
@Composable
private fun EraseIconSliderRow(
    iconRes: Int,
    iconDesc: String?,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    valueWidth: Dp = 44.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = iconDesc,
            tint = Color(EraseBgPanelTextSecondary),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colors.primary,
                activeTrackColor = MaterialTheme.colors.primary
            ),
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.caption,
            color = Color(EraseBgPanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(valueWidth)
        )
    }
}