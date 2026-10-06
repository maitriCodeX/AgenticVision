package com.agentic.vision.reasoning

import com.agentic.vision.fusion.RiskLevel
import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.PathAdvice

enum class SafetyPriority {
    CRITICAL, // Emergency immediate stop / avoidance
    WARNING,  // Caution / slowdown warning
    NONE      // Environment safe
}

data class SafetyAlert(
    val priority: SafetyPriority,
    val spokenInstruction: String,
    val targetObject: SceneObject? = null
)

/**
 * Tier 1 Rule-Based Safety Layer (< 100 ms deterministic execution).
 * Preempts LLM recommendations to guarantee collision avoidance.
 */
class SafetyLayer {

    fun evaluate(
        sceneObjects: List<SceneObject>,
        pathAdvice: PathAdvice
    ): SafetyAlert {
        // 1. Check for CRITICAL collision risks first
        val criticalObj = sceneObjects.find { it.riskLevel == RiskLevel.CRITICAL }
        if (criticalObj != null) {
            val label = criticalObj.label.lowercase()
            val distanceStr = String.format("%.1f", criticalObj.distanceM)
            val bearingStr = if (criticalObj.bearingDeg < 0) "on your left" else "on your right"

            val alertText = when {
                criticalObj.distanceM < 0.9f -> "Stop! $label very close!"
                criticalObj.bearingDeg < -15.0f -> "Stop! $label approaching from left!"
                criticalObj.bearingDeg > 15.0f -> "Stop! $label approaching from right!"
                else -> "Stop! $label $distanceStr meters ahead!"
            }

            return SafetyAlert(
                priority = SafetyPriority.CRITICAL,
                spokenInstruction = alertText,
                targetObject = criticalObj
            )
        }

        // 2. Check for WARNING collision risks
        val warningObj = sceneObjects.find { it.riskLevel == RiskLevel.WARNING }
        if (warningObj != null) {
            val label = warningObj.label.lowercase()
            val distanceStr = String.format("%.1f", warningObj.distanceM)
            val dirStr = if (warningObj.bearingDeg < 0) "left" else "right"

            return SafetyAlert(
                priority = SafetyPriority.WARNING,
                spokenInstruction = "Caution. $label $distanceStr metres $dirStr.",
                targetObject = warningObj
            )
        }

        // 3. Check for immediate path blockages
        if (!pathAdvice.isClearAhead && pathAdvice.hazardWarning != null) {
            return SafetyAlert(
                priority = SafetyPriority.WARNING,
                spokenInstruction = pathAdvice.hazardWarning,
                targetObject = null
            )
        }

        return SafetyAlert(
            priority = SafetyPriority.NONE,
            spokenInstruction = "",
            targetObject = null
        )
    }
}
