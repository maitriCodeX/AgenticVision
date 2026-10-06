package com.agentic.vision.perception

/**
 * Parsed 2D Object Detection result from YOLOv10 end-to-end ONNX inference.
 */
data class Detection(
    val label: String,
    val classId: Int,
    val confidence: Float,
    val boundingBox: FloatArray, // Normalized [x1, y1, x2, y2] in [0, 1]
    val isNavRelevant: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Detection
        return classId == other.classId && confidence == other.confidence && boundingBox.contentEquals(other.boundingBox)
    }

    override fun hashCode(): Int {
        var result = classId
        result = 31 * result + confidence.hashCode()
        result = 31 * result + boundingBox.contentHashCode()
        return result
    }
}

/**
 * Latency metrics for per-stage tracking.
 */
data class LatencyMetrics(
    val preprocessMs: Long = 0,
    val inferenceMs: Long = 0,
    val postprocessMs: Long = 0,
    val totalMs: Long = 0
)
