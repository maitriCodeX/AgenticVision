package com.agentic.vision.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentic.vision.fusion.RiskLevel
import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.Sector
import com.agentic.vision.pipeline.PipelineState

@Composable
fun MainScreen(
    state: PipelineState,
    onStartStopClick: () -> Unit,
    onGoalSubmit: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onBenchmarkClick: () -> Unit,
    onCalibrationClick: () -> Unit
) {
    var goalText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Agentic Vision Assistant",
                color = Color.Yellow,
                fontSize = 20.sp,
                modifier = Modifier.semantics { contentDescription = "Agentic Vision Assistant Header" }
            )
            Row {
                IconButton(onClick = onCalibrationClick) {
                    Text("CAL", color = Color.White, fontSize = 12.sp)
                }
                IconButton(onClick = onBenchmarkClick) {
                    Text("FPS", color = Color.White, fontSize = 12.sp)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Line
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "STATUS: ${if (state.isRunning) "ACTIVE (${String.format("%.1f", state.fps)} FPS)" else "STOPPED"}",
                    color = if (state.isRunning) Color.Green else Color.Red,
                    fontSize = 14.sp
                )
                Text(
                    text = "LAST SPEECH: ${state.lastUtterance?.text ?: "None"}",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Live Camera Preview & Debug Bounding Box Overlay Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F), shape = RoundedCornerShape(12.dp))
        ) {
            // Overlay Canvas
            if (state.isDebugOverlayEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw 3D Object Bounding Boxes & Distance Labels
                    for (obj in state.sceneObjects) {
                        val bbox = obj.bbox
                        val left = bbox[0] * canvasWidth
                        val top = bbox[1] * canvasHeight
                        val right = bbox[2] * canvasWidth
                        val bottom = bbox[3] * canvasHeight

                        val boxColor = when (obj.riskLevel) {
                            RiskLevel.CRITICAL -> Color.Red
                            RiskLevel.WARNING -> Color(0xFFFF9800)
                            RiskLevel.CAUTION -> Color.Yellow
                            RiskLevel.SAFE -> Color.Green
                        }

                        drawRect(
                            color = boxColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 4.dp.toPx())
                        )
                    }

                    // Draw 5-Sector Clearance Bar Visualization at Bottom of Overlay
                    state.pathAdvice?.let { advice ->
                        val sectorWidth = canvasWidth / 5.0f
                        val sectors = Sector.values()
                        for (i in sectors.indices) {
                            val clearance = advice.sectorClearanceM[sectors[i]] ?: 3.0f
                            val barHeight = (clearance / 5.0f).coerceIn(0.1f, 1.0f) * 60.dp.toPx()
                            val barColor = if (clearance < 1.2f) Color.Red else Color.Green

                            drawRect(
                                color = barColor.copy(alpha = 0.6f),
                                topLeft = Offset(i * sectorWidth + 4.dp.toPx(), canvasHeight - barHeight),
                                size = Size(sectorWidth - 8.dp.toPx(), barHeight)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Live Perception Feed Running\n(TalkBack Spoken Navigation Active)",
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Goal Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AssistChip(
                onClick = { onGoalSubmit("find a chair") },
                label = { Text("Find Chair", color = Color.Yellow) }
            )
            AssistChip(
                onClick = { onGoalSubmit("walk forward safely") },
                label = { Text("Walk Forward", color = Color.Yellow) }
            )
            AssistChip(
                onClick = { onGoalSubmit("find a person") },
                label = { Text("Find Person", color = Color.Yellow) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Goal Input Text Field & Voice Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Set Navigation Goal", color = Color.White) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.Yellow,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (goalText.isNotBlank()) {
                        onGoalSubmit(goalText)
                        goalText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input Goal", tint = Color.Yellow)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large High-Contrast Start/Stop Button
        Button(
            onClick = onStartStopClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = if (state.isRunning) "Stop Navigation" else "Start Navigation" },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) Color.Red else Color(0xFF00E676)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (state.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (state.isRunning) "STOP NAVIGATION" else "START NAVIGATION",
                color = Color.Black,
                fontSize = 20.sp
            )
        }
    }
}
