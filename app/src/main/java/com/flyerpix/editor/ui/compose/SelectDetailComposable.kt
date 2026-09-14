package com.flyerpix.editor.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flyerpix.editor.canvas.model.ShapeType

private val SelectColorScheme = lightColors(
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

@Composable
fun SelectDetailPage(
    selectedShape: ShapeType,
    onShapeSelect: (ShapeType) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    maxHeightPx: Int = 380
) {
    var shapeState by remember(selectedShape) { mutableStateOf(selectedShape) }

    MaterialTheme(colors = SelectColorScheme) {
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

                            // Header
                            Text(
                                text = "Select Shape Type",
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )

                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            // Shape options grid (2 columns)
                            val shapeOptions = listOf(
                                ShapeType.RECTANGLE to "Rectangle",
                                ShapeType.ROUNDED_RECTANGLE to "Rounded",
                                ShapeType.CIRCLE to "Circle",
                                ShapeType.TRIANGLE to "Triangle",
                                ShapeType.STAR to "Star",
                                ShapeType.ARC to "Arc"
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (i in shapeOptions.indices step 2) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(60.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // First shape button
                                        val (shapeType, label) = shapeOptions[i]
                                        ShapeOptionButton(
                                            label = label,
                                            shapeType = shapeType,
                                            isSelected = shapeState == shapeType,
                                            onClick = { shapeState = shapeType; onShapeSelect(shapeType) },
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Second shape button (if exists)
                                        if (i + 1 < shapeOptions.size) {
                                            val (shapeType2, label2) = shapeOptions[i + 1]
                                            ShapeOptionButton(
                                                label = label2,
                                                shapeType = shapeType2,
                                                isSelected = shapeState == shapeType2,
                                                onClick = { shapeState = shapeType2; onShapeSelect(shapeType2) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // Live Preview
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = Color(PanelDivider), thickness = 1.dp)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(PanelDivider), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                                shape = when (shapeState) {
                                                    ShapeType.RECTANGLE -> RoundedCornerShape(2.dp)
                                                    ShapeType.ROUNDED_RECTANGLE -> RoundedCornerShape(8.dp)
                                                    ShapeType.CIRCLE -> CircleShape
                                                    else -> RoundedCornerShape(4.dp)
                                                }
                                            )
                                            .border(2.dp, MaterialTheme.colors.primary, 
                                                when (shapeState) {
                                                    ShapeType.RECTANGLE -> RoundedCornerShape(2.dp)
                                                    ShapeType.ROUNDED_RECTANGLE -> RoundedCornerShape(8.dp)
                                                    ShapeType.CIRCLE -> CircleShape
                                                    else -> RoundedCornerShape(4.dp)
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Preview: ${when (shapeState) {
                                            ShapeType.RECTANGLE -> "Rectangle"
                                            ShapeType.ROUNDED_RECTANGLE -> "Rounded Rectangle"
                                            ShapeType.CIRCLE -> "Circle"
                                            ShapeType.TRIANGLE -> "Triangle"
                                            ShapeType.STAR -> "Star"
                                            ShapeType.ARC -> "Arc"
                                        }}",
                                        style = MaterialTheme.typography.caption,
                                        color = Color(PanelTextSecondary),
                                        fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    )
                                }
                            }
                        }

                        // Right column: Cancel & Apply
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
private fun ShapeOptionButton(
    label: String,
    shapeType: ShapeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxHeight()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF1769FF) else Color(0xFFE4E8F0),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected) Color(0xFFE3EEFC) else Color.White,
            contentColor = if (isSelected) Color(0xFF1769FF) else Color(0xFF5F6B7A)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Shape icon representation
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = if (isSelected) Color(0xFF1769FF) else Color(0xFF5F6B7A),
                        shape = when (shapeType) {
                            ShapeType.RECTANGLE -> RoundedCornerShape(2.dp)
                            ShapeType.ROUNDED_RECTANGLE -> RoundedCornerShape(6.dp)
                            ShapeType.CIRCLE -> CircleShape
                            else -> RoundedCornerShape(3.dp)
                        }
                    )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                fontSize = androidx.compose.ui.unit.TextUnit(9f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
