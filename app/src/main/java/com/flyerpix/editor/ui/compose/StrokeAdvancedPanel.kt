package com.flyerpix.editor.ui.compose

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import com.flyerpix.editor.canvas.model.PenLayer

private const val PanelTextSecondary = 0xFF5F6B7A
private const val PanelDivider = 0xFFE4E8F0

/**
 * Panel untuk advanced stroke & fill options (Phase 3).
 */
@Composable
fun StrokeAdvancedPanel(
    penLayer: PenLayer,
    onChanged: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Divider(color = Color(PanelDivider), thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))

        // ── STROKE SECTION ──────────────────────────────────────────
        Text(
            text = "Stroke Settings",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold,
            color = Color(PanelTextSecondary),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Stroke Opacity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Opacity",
                style = MaterialTheme.typography.caption,
                color = Color(PanelTextSecondary),
                modifier = Modifier.width(50.dp)
            )
            Slider(
                value = penLayer.strokeOpacity,
                onValueChange = { newOpacity ->
                    penLayer.strokeOpacity = newOpacity
                    onChanged()
                },
                valueRange = 0f..1f,
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFF1769FF))
            )
            Text(
                text = "${(penLayer.strokeOpacity * 100).toInt()}%",
                style = MaterialTheme.typography.caption,
                color = Color(PanelTextSecondary),
                modifier = Modifier.width(35.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stroke Cap
        Text(
            text = "Line Cap",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StrokeCapButton("Butt", Paint.Cap.BUTT, penLayer.strokeCap, { penLayer.strokeCap = it; onChanged() }, Modifier.weight(1f))
            StrokeCapButton("Round", Paint.Cap.ROUND, penLayer.strokeCap, { penLayer.strokeCap = it; onChanged() }, Modifier.weight(1f))
            StrokeCapButton("Square", Paint.Cap.SQUARE, penLayer.strokeCap, { penLayer.strokeCap = it; onChanged() }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stroke Join
        Text(
            text = "Line Join",
            style = MaterialTheme.typography.caption,
            color = Color(PanelTextSecondary),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StrokeJoinButton("Miter", Paint.Join.MITER, penLayer.strokeJoin, { penLayer.strokeJoin = it; onChanged() }, Modifier.weight(1f))
            StrokeJoinButton("Round", Paint.Join.ROUND, penLayer.strokeJoin, { penLayer.strokeJoin = it; onChanged() }, Modifier.weight(1f))
            StrokeJoinButton("Bevel", Paint.Join.BEVEL, penLayer.strokeJoin, { penLayer.strokeJoin = it; onChanged() }, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── FILL SECTION ───────────────────────────────────────────
        Text(
            text = "Fill Settings",
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold,
            color = Color(PanelTextSecondary),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Fill Enabled Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Enabled",
                style = MaterialTheme.typography.caption,
                color = Color(PanelTextSecondary)
            )
            Spacer(modifier = Modifier.weight(1f))
            Checkbox(
                checked = penLayer.fillEnabled,
                onCheckedChange = { checked ->
                    penLayer.fillEnabled = checked
                    onChanged()
                }
            )
        }

        if (penLayer.fillEnabled) {
            Spacer(modifier = Modifier.height(4.dp))

            // Fill Opacity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Opacity",
                    style = MaterialTheme.typography.caption,
                    color = Color(PanelTextSecondary),
                    modifier = Modifier.width(50.dp)
                )
                Slider(
                    value = penLayer.fillOpacity,
                    onValueChange = { newOpacity ->
                        penLayer.fillOpacity = newOpacity
                        onChanged()
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF1769FF))
                )
                Text(
                    text = "${(penLayer.fillOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.caption,
                    color = Color(PanelTextSecondary),
                    modifier = Modifier.width(35.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun StrokeCapButton(
    label: String,
    cap: Paint.Cap,
    current: Paint.Cap,
    onClick: (Paint.Cap) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(cap) },
        modifier = modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (current == cap) Color(0xFF1769FF) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = if (current == cap) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun StrokeJoinButton(
    label: String,
    join: Paint.Join,
    current: Paint.Join,
    onClick: (Paint.Join) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(join) },
        modifier = modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (current == join) Color(0xFF1769FF) else Color(0xFFE8EAED)
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            color = if (current == join) Color.White else Color(PanelTextSecondary),
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.sp
        )
    }
}
