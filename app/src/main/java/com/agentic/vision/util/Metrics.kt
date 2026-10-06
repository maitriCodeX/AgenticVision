package com.agentic.vision.util

import com.agentic.vision.perception.LatencyMetrics

data class StageMetrics(
    val stageName: String,
    val avgMs: Float,
    val p95Ms: Float
)

/**
 * Latency metrics calculator computing p50, p95, and average latency per pipeline stage.
 */
class Metrics {

    private val yoloLatencies = mutableListOf<Long>()
    private val midasLatencies = mutableListOf<Long>()
    private val fusionLatencies = mutableListOf<Long>()
    private val llmLatencies = mutableListOf<Long>()
    private val totalLatencies = mutableListOf<Long>()

    fun recordFrame(
        yoloMs: Long,
        midasMs: Long,
        fusionMs: Long,
        llmMs: Long,
        totalMs: Long
    ) {
        synchronized(this) {
            yoloLatencies.add(yoloMs)
            midasLatencies.add(midasMs)
            fusionLatencies.add(fusionMs)
            llmLatencies.add(llmMs)
            totalLatencies.add(totalMs)

            if (yoloLatencies.size > 100) {
                yoloLatencies.removeAt(0)
                midasLatencies.removeAt(0)
                fusionLatencies.removeAt(0)
                llmLatencies.removeAt(0)
                totalLatencies.removeAt(0)
            }
        }
    }

    fun getStageMetrics(stageName: String): StageMetrics {
        synchronized(this) {
            val list = when (stageName.lowercase()) {
                "yolo" -> yoloLatencies
                "midas" -> midasLatencies
                "fusion" -> fusionLatencies
                "llm" -> llmLatencies
                else -> totalLatencies
            }

            if (list.isEmpty()) {
                return StageMetrics(stageName, 0.0f, 0.0f)
            }

            val avg = list.average().toFloat()
            val sorted = list.sorted()
            val p95Index = (sorted.size * 0.95).toInt().coerceIn(0, sorted.size - 1)
            val p95 = sorted[p95Index].toFloat()

            return StageMetrics(stageName, avg, p95)
        }
    }
}
