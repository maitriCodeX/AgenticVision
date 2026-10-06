package com.agentic.vision.reasoning

import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.PathAdvice

/**
 * Lightweight Rule-Based Fallback Reasoning Engine for low-RAM devices or fast unit testing.
 * Guarantees zero latency timeouts and zero JSON validation errors.
 */
class FallbackLlmEngine : LlmEngine {

    override val name: String = "Fallback Deterministic Reasoner"

    override fun generateInstruction(systemPrompt: String, userPrompt: String): LlmResponse {
        val startTime = System.currentTimeMillis()

        val isTargetReached = userPrompt.contains("GOAL_REACHED") || userPrompt.contains("\"dist\":1.") && userPrompt.contains("target_class")
        val isTargetClassFound = userPrompt.contains("target_class") && !userPrompt.contains("\"target_class\":\"none\"")

        val action: LlmAction
        val speech: String

        if (userPrompt.contains("\"CENTER\":0.") || userPrompt.contains("\"CENTER\":1.0")) {
            action = LlmAction.SLIGHT_LEFT
            speech = "Obstacle ahead. Move slightly left."
        } else if (isTargetReached) {
            action = LlmAction.GOAL_REACHED
            speech = "Target reached."
        } else if (isTargetClassFound && userPrompt.contains("\"bearing\":-")) {
            action = LlmAction.SLIGHT_LEFT
            speech = "Target visible on left. Move left."
        } else if (isTargetClassFound && userPrompt.contains("\"bearing\":") && !userPrompt.contains("\"bearing\":-")) {
            action = LlmAction.SLIGHT_RIGHT
            speech = "Target visible on right. Move right."
        } else {
            action = LlmAction.FORWARD
            speech = "Clear path. Walk forward safely."
        }

        val endTime = System.currentTimeMillis()
        return LlmResponse(
            action = action,
            speech = speech,
            latencyMs = endTime - startTime,
            isValidJson = true
        )
    }

    override fun close() {}
}
