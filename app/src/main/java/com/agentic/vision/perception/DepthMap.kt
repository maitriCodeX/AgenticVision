package com.agentic.vision.perception

/**
 * Encapsulates neural inverse depth map and metric spatial distance map.
 */
data class DepthMap(
    val width: Int,
    val height: Int,
    val rawInverseDepth: FloatArray,
    val metricDepthMeters: FloatArray,
    val isScaleCalibrated: Boolean = false,
    val scaleFactor: Float = 1.0f,
    val shiftFactor: Float = 0.0f
) {
    fun getMetricDepthAt(x: Int, y: Int): Float {
        val clampedX = x.coerceIn(0, width - 1)
        val clampedY = y.coerceIn(0, height - 1)
        return metricDepthMeters[clampedY * width + clampedX]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DepthMap
        return width == other.width && height == other.height && metricDepthMeters.contentEquals(other.metricDepthMeters)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + metricDepthMeters.contentHashCode()
        return result
    }
}
