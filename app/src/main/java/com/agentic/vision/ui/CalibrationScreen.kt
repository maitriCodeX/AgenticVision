package com.agentic.vision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agentic.vision.perception.DistanceEvaluator
import com.agentic.vision.perception.EvaluationReport

@Composable
fun CalibrationScreen(
    onBackClick: () -> Unit
) {
    var groundTruthText by remember { mutableStateOf("2.0") }
    var estimatedText by remember { mutableStateOf("2.2") }
    val evaluator = remember { DistanceEvaluator() }
    var report by remember { mutableStateOf(EvaluationReport(0, 0f, 0f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text("Distance Calibration & MAE Evaluator", color = Color.Yellow, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = groundTruthText,
            onValueChange = { groundTruthText = it },
            label = { Text("Ground Truth Distance (m)", color = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = estimatedText,
            onValueChange = { estimatedText = it },
            label = { Text("Estimated Distance (m)", color = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val gt = groundTruthText.toFloatOrNull() ?: 2.0f
                val est = estimatedText.toFloatOrNull() ?: 2.0f
                evaluator.recordSample("object", gt, est)
                report = evaluator.computeReport()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
        ) {
            Text("Record Calibration Sample", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("EVALUATION METRICS:", color = Color.Yellow, fontSize = 18.sp)
        Text("Sample Count: ${report.sampleCount}", color = Color.White)
        Text("Mean Absolute Error (MAE): ${String.format("%.2f", report.maeMeters)} m", color = Color.White)
        Text("Mean Absolute % Error (MAPE): ${String.format("%.1f", report.mapePercent)} %", color = Color.White)

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
