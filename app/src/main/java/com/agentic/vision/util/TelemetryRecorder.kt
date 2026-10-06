package com.agentic.vision.util

import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.PathAdvice
import com.agentic.vision.perception.LatencyMetrics
import com.agentic.vision.speech.SpeechUtterance
import java.io.File

data class TelemetryFrameRecord(
    val frameId: Long,
    val timestampMs: Long,
    val objectCount: Int,
    val topObjectLabel: String?,
    val topObjectDistM: Float?,
    val recommendedHeadingDeg: Float,
    val spokenInstruction: String?,
    val totalLatencyMs: Long
)

/**
 * Telemetry recorder capturing per-frame perception, tracking, navigation decisions,
 * and stage latencies into CSV and JSON export formats.
 */
class TelemetryRecorder {

    private val records = mutableListOf<TelemetryFrameRecord>()

    fun recordFrame(
        frameId: Long,
        timestampMs: Long,
        sceneObjects: List<SceneObject>,
        pathAdvice: PathAdvice,
        utterance: SpeechUtterance?,
        latencyMetrics: LatencyMetrics
    ) {
        val topObj = sceneObjects.minByOrNull { it.distanceM }
        records.add(
            TelemetryFrameRecord(
                frameId = frameId,
                timestampMs = timestampMs,
                objectCount = sceneObjects.size,
                topObjectLabel = topObj?.label,
                topObjectDistM = topObj?.distanceM,
                recommendedHeadingDeg = pathAdvice.recommendedHeadingDeg,
                spokenInstruction = utterance?.text,
                totalLatencyMs = latencyMetrics.totalMs
            )
        )
    }

    fun exportCsv(outputFile: File): Boolean {
        return try {
            val sb = StringBuilder()
            sb.append("FrameId,TimestampMs,ObjectCount,TopLabel,TopDistM,RecommendedHeadingDeg,SpokenInstruction,TotalLatencyMs\n")
            for (r in records) {
                sb.append("${r.frameId},${r.timestampMs},${r.objectCount},${r.topObjectLabel ?: ""},${r.topObjectDistM ?: ""},${r.recommendedHeadingDeg},\"${r.spokenInstruction ?: ""}\",${r.totalLatencyMs}\n")
            }
            outputFile.writeText(sb.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clear() {
        records.clear()
    }
}
