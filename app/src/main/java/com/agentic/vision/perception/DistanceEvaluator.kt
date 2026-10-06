package com.agentic.vision.perception

import kotlin.math.abs

data class DistanceEvaluationEntry(
    val timestampMs: Long,
    val objectLabel: String,
    val groundTruthMeters: Float,
    val estimatedMeters: Float,
    val isCalibrated: Boolean
)

data class EvaluationReport(
    val sampleCount: Int,
    val maeMeters: Float,
    val mapePercent: Float
)

/**
 * Distance accuracy evaluation tool calculating Mean Absolute Error (MAE)
 * and Mean Absolute Percentage Error (MAPE).
 */
class DistanceEvaluator {

    private val entries = mutableListOf<DistanceEvaluationEntry>()

    fun recordSample(
        objectLabel: String,
        groundTruthMeters: Float,
        estimatedMeters: Float,
        isCalibrated: Boolean = true
    ) {
        entries.add(
            DistanceEvaluationEntry(
                timestampMs = System.currentTimeMillis(),
                objectLabel = objectLabel,
                groundTruthMeters = groundTruthMeters,
                estimatedMeters = estimatedMeters,
                isCalibrated = isCalibrated
            )
        )
    }

    fun computeReport(): EvaluationReport {
        if (entries.isEmpty()) {
            return EvaluationReport(0, 0.0f, 0.0f)
        }

        var sumAbsError = 0.0f
        var sumPercentageError = 0.0f

        for (e in entries) {
            val err = abs(e.estimatedMeters - e.groundTruthMeters)
            sumAbsError += err
            if (e.groundTruthMeters > 0.01f) {
                sumPercentageError += (err / e.groundTruthMeters) * 100.0f
            }
        }

        val count = entries.size
        return EvaluationReport(
            sampleCount = count,
            maeMeters = sumAbsError / count,
            mapePercent = sumPercentageError / count
        )
    }

    fun clear() {
        entries.clear()
    }
}
