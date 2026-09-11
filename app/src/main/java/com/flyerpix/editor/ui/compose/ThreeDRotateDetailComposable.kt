package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.lightColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val ThreeDRotateColorScheme = lightColors(
    primary = Color(0xFF1769FF),
    primaryVariant = Color(0xFF4A8EFF),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

/**
 * Compose bottom sheet untuk 3D Rotate (X / Y / Z), gayanya sama persis dengan
 * halaman 3D Text: card bawah + kolom pengaturan (label + slider + nilai) di kiri
 * dan tombol Cancel/Apply di kanan.
 */
@Composable
fun ThreeDRotateDetailPage(
    rotateX: Float,
    rotateY: Float,
    rotateZ: Float,
    onRotateXChange: (Float) -> Unit,
    onRotateYChange: (Float) -> Unit,
    onRotateZChange: (Float) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 420
) {
    var rotateXState by remember(rotateX) { mutableStateOf(rotateX) }
    var rotateYState by remember(rotateY) { mutableStateOf(rotateY) }
    var rotateZState by remember(rotateZ) { mutableStateOf(rotateZ) }

    MaterialTheme(colors = ThreeDRotateColorScheme) {
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
                            .background(Color(PanelHandle), RoundedCornerShape(50))
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
                            Divider(color = Color(PanelDivider), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(4.dp))

                            RotateRow(
                                label = "X Axis",
                                value = rotateXState,
                                onValueChange = { new ->
                                    rotateXState = new
                                    onRotateXChange(new)
                                }
                            )

                            RotateRow(
                                label = "Y Axis",
                                value = rotateYState,
                                onValueChange = { new ->
                                    rotateYState = new
                                    onRotateYChange(new)
                                }
                            )

                            RotateRow(
                                label = "Z Axis",
                                value = rotateZState,
                                onValueChange = { new ->
                                    rotateZState = new
                                    onRotateZChange(new)
                                }
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        rotateXState = 0f
                                        rotateYState = 0f
                                        rotateZState = 0f
                                        onRotateXChange(0f)
                                        onRotateYChange(0f)
                                        onRotateZChange(0f)
                                    }
                                    .padding(vertical = 4.dp)
                            )
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
                                Text("Cancel", style = MaterialTheme.typography.caption)
                            }
                            Button(
                                onClick = onApply,
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
                                    text = "Apply",
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

@Composable
private fun RotateRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            modifier = Modifier.width(52.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -180f..180f,
            steps = 359,
            modifier = Modifier
                .weight(1f)
                .height(30.dp)
        )
        Text(
            text = "${value.toInt()}°",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}