package com.flyerpix.editor.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RemoveBgComposable(
    onClose: () -> Unit,
    onGradientMaskClick: () -> Unit,
    onPaintMaskClick: () -> Unit,
    selectedLayerName: String = "Selected Layer"
) {
    var selectedMethod by remember { mutableStateOf<String?>(null) }
    var featherStrength by remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Remove Background",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        // Target Layer Info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            backgroundColor = Color(0xFF2A2A2A),
            shape = RoundedCornerShape(8.dp),
            elevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Layer",
                    tint = Color(0xFF0066FF),
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Layer:",
                        fontSize = 11.sp,
                        color = Color(0xFFBBBBBB)
                    )
                    Text(
                        selectedLayerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // Method Selection
        Text(
            "Select Method",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFBBBBBB)
        )

        // Gradient Mask Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            backgroundColor = if (selectedMethod == "gradient") Color(0xFF0066FF).copy(alpha = 0.2f) else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp
        ) {
            Button(
                onClick = { selectedMethod = "gradient" },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = null
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Gradient Mask (Automatic)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Best for skies & horizons. Creates smooth fade-out effect.",
                        fontSize = 11.sp,
                        color = Color(0xFFBBBBBB),
                        maxLines = 2
                    )
                }
            }
        }

        // Paint Mask Option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            backgroundColor = if (selectedMethod == "paint") Color(0xFF0066FF).copy(alpha = 0.2f) else Color(0xFF2A2A2A),
            shape = RoundedCornerShape(12.dp),
            elevation = 0.dp
        ) {
            Button(
                onClick = { selectedMethod = "paint" },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = null
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Paint Mask (Manual)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Precise control. Paint black to remove, white to restore.",
                        fontSize = 11.sp,
                        color = Color(0xFFBBBBBB),
                        maxLines = 2
                    )
                }
            }
        }

        // Feather Strength Slider
        if (selectedMethod != null) {
            Divider(color = Color(0xFF333333), thickness = 0.5.dp)

            Text(
                "Edge Smoothness",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFBBBBBB)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Slider(
                    value = featherStrength,
                    onValueChange = { featherStrength = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0066FF),
                        activeTrackColor = Color(0xFF0066FF),
                        inactiveTrackColor = Color(0xFF444444)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Soft", fontSize = 11.sp, color = Color(0xFF888888))
                    Text("Hard", fontSize = 11.sp, color = Color(0xFF888888))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF333333)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = Color.White, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    when (selectedMethod) {
                        "gradient" -> onGradientMaskClick()
                        "paint" -> onPaintMaskClick()
                        else -> onClose()
                    }
                    onClose()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                enabled = selectedMethod != null,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF0066FF),
                    disabledBackgroundColor = Color(0xFF555555)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Apply",
                    color = if (selectedMethod != null) Color.White else Color(0xFF888888),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
