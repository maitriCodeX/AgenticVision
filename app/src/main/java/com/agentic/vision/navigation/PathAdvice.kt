package com.agentic.vision.navigation

enum class Sector {
    FAR_LEFT,  // -45° to -25°
    LEFT,      // -25° to -8°
    CENTER,    // -8° to +8°
    RIGHT,     // +8° to +25°
    FAR_RIGHT  // +25° to +45°
}

data class PathAdvice(
    val recommendedHeadingDeg: Float, // e.g. -20f means veer slightly left
    val sectorClearanceM: Map<Sector, Float>,
    val hazardWarning: String? = null,
    val isClearAhead: Boolean = true
)
