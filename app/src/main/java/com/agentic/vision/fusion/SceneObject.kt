package com.agentic.vision.fusion

enum class ApproachState {
    APPROACHING,
    RECEDING,
    CROSSING,
    STATIC
}

enum class RiskLevel {
    SAFE,
    CAUTION,
    WARNING,
    CRITICAL
}

/**
 * 3D Scene object containing metric geometry, spatial bearing, tracking state, and collision risk.
 */
data class SceneObject(
    val trackId: Int,
    val label: String,
    val confidence: Float,
    val distanceM: Float,
    val bearingDeg: Float, // Negative = left, Positive = right
    val lateralOffsetM: Float = 0.0f,
    val positionX: Float = 0.0f, // Lateral ground offset (+ right, - left)
    val positionZ: Float = distanceM, // Forward distance
    val velocityVx: Float = 0.0f, // Lateral velocity (m/s)
    val velocityVz: Float = 0.0f, // Longitudinal velocity (m/s, negative means approaching)
    val speed: Float = 0.0f, // Magnitude of velocity vector
    val approachState: ApproachState = ApproachState.STATIC,
    val ttcSeconds: Float? = null,
    val riskLevel: RiskLevel = RiskLevel.SAFE,
    val bbox: FloatArray = floatArrayOf(0f, 0f, 0f, 0f) // Normalized [x1, y1, x2, y2]
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SceneObject
        return trackId == other.trackId && label == other.label
    }

    override fun hashCode(): Int {
        var result = trackId
        result = 31 * result + label.hashCode()
        return result
    }

    fun toSummaryString(): String {
        val bearingStr = if (bearingDeg < 0) "${Math.abs(bearingDeg).toInt()}° left" else "${bearingDeg.toInt()}° right"
        val stateStr = approachState.name.lowercase()
        return "$label ${String.format("%.1f", distanceM)}m ahead, $bearingStr, $stateStr"
    }
}
