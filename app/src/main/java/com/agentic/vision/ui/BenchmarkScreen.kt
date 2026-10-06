package com.agentic.vision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentic.vision.util.Metrics

@Composable
fun BenchmarkScreen(
    metrics: Metrics,
    thermalStateName: String,
    onBackClick: () -> Unit
) {
    val yoloMetrics = metrics.getStageMetrics("yolo")
    val midasMetrics = metrics.getStageMetrics("midas")
    val fusionMetrics = metrics.getStageMetrics("fusion")
    val llmMetrics = metrics.getStageMetrics("llm")
    val totalMetrics = metrics.getStageMetrics("total")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("Stage Latency & Benchmark Metrics", color = Color.Yellow, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Hardware Execution Provider: ONNX Runtime + NNAPI / XNNPACK", color = Color.Green)
        Text("Thermal Status: $thermalStateName", color = Color.LightGray)

        Spacer(modifier = Modifier.height(16.dp))

        BenchmarkRow("YOLOv10 Detection", yoloMetrics.avgMs, yoloMetrics.p95Ms)
        BenchmarkRow("MiDaS Depth Estimation", midasMetrics.avgMs, midasMetrics.p95Ms)
        BenchmarkRow("Scene Fusion & Tracking", fusionMetrics.avgMs, fusionMetrics.p95Ms)
        BenchmarkRow("Agentic LLM Reasoning", llmMetrics.avgMs, llmMetrics.p95Ms)
        BenchmarkRow("Total End-to-End Latency", totalMetrics.avgMs, totalMetrics.p95Ms)

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

@Composable
private fun BenchmarkRow(stageName: String, avgMs: Float, p95Ms: Float) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(stageName, color = Color.White, fontSize = 16.sp)
        Text("Avg: ${String.format("%.1f", avgMs)} ms | p95: ${String.format("%.1f", p95Ms)} ms", color = Color.LightGray, fontSize = 14.sp)
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
    }
}
