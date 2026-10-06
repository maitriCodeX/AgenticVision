package com.agentic.vision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    isDebugOverlay: Boolean,
    onToggleDebugOverlay: () -> Unit,
    isHindi: Boolean,
    onToggleHindi: () -> Unit,
    isFallbackLlm: Boolean,
    onToggleFallbackLlm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("Settings & Configuration", color = Color.Yellow, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Debug Bounding Box & Sector Overlay", color = Color.White)
            Switch(checked = isDebugOverlay, onCheckedChange = { onToggleDebugOverlay() })
        }
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speech Language (Hindi Mode)", color = Color.White)
            Switch(checked = isHindi, onCheckedChange = { onToggleHindi() })
        }
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Use Fast Fallback LLM Engine", color = Color.White)
            Switch(checked = isFallbackLlm, onCheckedChange = { onToggleFallbackLlm() })
        }
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

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
