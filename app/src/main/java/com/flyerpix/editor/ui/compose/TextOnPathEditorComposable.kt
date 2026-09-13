package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Color as AndroidColor
import com.flyerpix.editor.canvas.model.TextOnPathLayer

private val TextPathColorScheme = lightColors(
    primary = Color(0xFF00BCD4),
    primaryVariant = Color(0xFF0097A7),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A2E),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A2E)
)

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0
private const val PanelHandle = 0xFFD0D4DE

/**
 * Text on Path Editor - render text along Bézier paths (Phase 5).
 */
@Composable
fun TextOnPathEditorPanel(
    textOnPathLayer: TextOnPathLayer,
    onTextChanged: () -> Unit,
    onExitEditMode: () -> Unit,
    maxHeightPx: Int = 600
) {
    MaterialTheme(colors = TextPathColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(androidx.compose.ui.platform.LocalDensity.current) { maxHeightPx.toDp() }),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                elevation = 8.dp,
                backgroundColor = MaterialTheme.colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    // Drag handle
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

                            // Title
                            Text(
                                text = "Text on Path",
                                style = MaterialTheme.typography.subtitle2,
                                color = Color(PanelTextSecondary),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── TEXT CONTENT ────────────────────────────────────
                            Text(
                                text = "Text",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            TextField(
                                value = textOnPathLayer.text,
                                onValueChange = { newText ->
                                    textOnPathLayer.text = newText
                                    onTextChanged()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                textStyle = androidx.compose.material.MaterialTheme.typography.body2,
                                singleLine = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = Color(0xFFE8EAED),
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color(0xFF00BCD4)
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── FONT SIZE ──────────────────────────────────────
                            Text(
                                text = "Font Size",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Slider(
                                    value = textOnPathLayer.fontSize,
                                    onValueChange = { newSize ->
                                        textOnPathLayer.fontSize = newSize
                                        onTextChanged()
                                    },
                                    valueRange = 8f..128f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00BCD4))
                                )
                                Text(
                                    text = "${textOnPathLayer.fontSize.toInt()}px",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── TEXT STYLE ─────────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                StyleButton("B", textOnPathLayer.isBold, Modifier.weight(1f)) {
                                    textOnPathLayer.isBold = it
                                    onTextChanged()
                                }
                                StyleButton("I", textOnPathLayer.isItalic, Modifier.weight(1f)) {
                                    textOnPathLayer.isItalic = it
                                    onTextChanged()
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── PATH OFFSET ────────────────────────────────────
                            Text(
                                text = "Path Offset",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Slider(
                                    value = textOnPathLayer.pathOffset,
                                    onValueChange = { newOffset ->
                                        textOnPathLayer.pathOffset = newOffset.coerceIn(0f, 1f)
                                        onTextChanged()
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00BCD4))
                                )
                                Text(
                                    text = "${(textOnPathLayer.pathOffset * 100).toInt()}%",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── LETTER SPACING ─────────────────────────────────
                            Text(
                                text = "Letter Spacing",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Slider(
                                    value = textOnPathLayer.letterSpacing,
                                    onValueChange = { newSpacing ->
                                        textOnPathLayer.letterSpacing = newSpacing
                                        onTextChanged()
                                    },
                                    valueRange = -5f..20f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp),
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00BCD4))
                                )
                                Text(
                                    text = "${textOnPathLayer.letterSpacing.toInt()}",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── TEXT ALIGNMENT ─────────────────────────────────
                            Text(
                                text = "Alignment",
                                style = MaterialTheme.typography.caption,
                                color = Color(PanelTextSecondary),
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AlignmentButton("L", textOnPathLayer.textAlignment == TextOnPathLayer.TextAlignment.LEFT, Modifier.weight(1f)) {
                                    textOnPathLayer.textAlignment = TextOnPathLayer.TextAlignment.LEFT
                                    onTextChanged()
                                }
                                AlignmentButton("C", textOnPathLayer.textAlignment == TextOnPathLayer.TextAlignment.CENTER, Modifier.weight(1f)) {
                                    textOnPathLayer.textAlignment = TextOnPathLayer.TextAlignment.CENTER
                                    onTextChanged()
                                }
                                AlignmentButton("R", textOnPathLayer.textAlignment == TextOnPathLayer.TextAlignment.RIGHT, Modifier.weight(1f)) {
                                    textOnPathLayer.textAlignment = TextOnPathLayer.TextAlignment.RIGHT
                                    onTextChanged()
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // ── ROTATION FOLLOW ────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Follow Rotation",
                                    style = MaterialTheme.typography.caption,
                                    color = Color(PanelTextSecondary),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Checkbox(
                                    checked = textOnPathLayer.followPathRotation,
                                    onCheckedChange = { checked ->
                                        textOnPathLayer.followPathRotation = checked
                                        onTextChanged()
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Right-side close button
                        Column(
                            modifier = Modifier
                                .width(60.dp)
                                .height(200.dp)
                                .padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                        ) {
                            Button(
                                onClick = onExitEditMode,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFFF6B6B)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Boolean) -> Unit
) {
    Button(
        onClick = { onClick(!isActive) },
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isActive) Color(0xFF00BCD4) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AlignmentButton(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isActive) Color(0xFF00BCD4) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
