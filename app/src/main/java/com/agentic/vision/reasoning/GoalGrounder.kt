package com.agentic.vision.reasoning

import com.agentic.vision.perception.YoloDetector

data class GroundedGoal(
    val rawGoalText: String,
    val targetClass: String?,
    val isSupportedInDetector: Boolean,
    val guidanceMessage: String
)

/**
 * Maps spoken or typed user goals to detectable COCO classes.
 * Prevents LLM object hallucinations when targets are undetectable.
 */
class GoalGrounder {

    private val goalToClassMap = mapOf(
        "chair" to "chair",
        "seat" to "chair",
        "bench" to "bench",
        "couch" to "couch",
        "sofa" to "couch",
        "table" to "dining table",
        "person" to "person",
        "friend" to "person",
        "human" to "person",
        "car" to "car",
        "vehicle" to "car",
        "bus" to "bus",
        "bike" to "bicycle",
        "bicycle" to "bicycle",
        "motorcycle" to "motorcycle",
        "dog" to "dog",
        "cat" to "cat",
        "stop sign" to "stop sign",
        "traffic light" to "traffic light",
        "bag" to "backpack",
        "backpack" to "backpack",
        "suitcase" to "suitcase"
    )

    fun groundGoal(rawInput: String): GroundedGoal {
        val normalized = rawInput.lowercase().trim()

        if (normalized.isEmpty() || normalized.contains("walk") || normalized.contains("navigate") || normalized.contains("forward")) {
            return GroundedGoal(
                rawGoalText = rawInput,
                targetClass = null,
                isSupportedInDetector = true,
                guidanceMessage = "Navigation mode active. Walking forward safely."
            )
        }

        // Try matching target class
        for ((keyword, targetClass) in goalToClassMap) {
            if (normalized.contains(keyword)) {
                return GroundedGoal(
                    rawGoalText = rawInput,
                    targetClass = targetClass,
                    isSupportedInDetector = true,
                    guidanceMessage = "Searching for $targetClass."
                )
            }
        }

        // Check for known unsupported goals (e.g. door, stairs, exit)
        return GroundedGoal(
            rawGoalText = rawInput,
            targetClass = null,
            isSupportedInDetector = false,
            guidanceMessage = "'$rawInput' cannot be directly detected. Standard obstacle navigation active."
        )
    }
}
