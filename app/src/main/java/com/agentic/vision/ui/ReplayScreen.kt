package com.agentic.vision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReplayScreen(
    onBackClick: () -> Unit
) {
    var statusText by remember { mutableStateOf("No replay file loaded.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("Replay & Session Telemetry Exporter", color = Color.Yellow, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Select recorded MP4 session file to replay offline pipeline:", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { statusText = "Loaded test_session.mp4. Replay ready." },
            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
        ) {
            Text("Load Video File", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Status: $statusText", color = Color.Green)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { statusText = "Telemetry exported to /sdcard/Download/pipeline_telemetry.csv" },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Export Session Telemetry (CSV)", color = Color.Black)
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Back to Main Screen", color = Color.Black)
        }
    }
}
