package com.agentic.vision.reasoning

import com.agentic.vision.navigation.PathAdvice
import com.agentic.vision.navigation.Sector
import com.agentic.vision.speech.SpeechPriority
import com.agentic.vision.speech.SpeechUtterance

/**
 * Instruction Arbiter ensuring safety preemption over LLM outputs
 * and validating spatial sector clearances before speech dispatch.
 */
class InstructionArbiter {

    fun arbitrate(
        safetyAlert: SafetyAlert,
        llmResponse: LlmResponse?,
        pathAdvice: PathAdvice
    ): SpeechUtterance {
        // 1. Tier 1 Safety Layer Preemption (Highest Priority)
        if (safetyAlert.priority == SafetyPriority.CRITICAL) {
            return SpeechUtterance(
                priority = SpeechPriority.CRITICAL,
                text = safetyAlert.spokenInstruction
            )
        }

        if (safetyAlert.priority == SafetyPriority.WARNING) {
            return SpeechUtterance(
                priority = SpeechPriority.WARNING,
                text = safetyAlert.spokenInstruction
            )
        }

        // 2. LLM Instruction Validation against Free-Space Sector Clearances
        if (llmResponse != null) {
            val validatedAction = validateLlmAction(llmResponse.action, pathAdvice)
            val speechText = if (validatedAction != llmResponse.action) {
                // LLM proposed an unsafe direction; override with safe path guidance
                getSafeFallbackSpeech(validatedAction)
            } else {
                llmResponse.speech
            }

            return SpeechUtterance(
                priority = SpeechPriority.GUIDANCE,
                text = speechText
            )
        }

        // 3. Fallback General Path Advice
        val defaultText = when {
            pathAdvice.recommendedHeadingDeg < -15.0f -> "Slight left."
            pathAdvice.recommendedHeadingDeg > 15.0f -> "Slight right."
            pathAdvice.isClearAhead -> "Clear path, walk forward."
            else -> "Obstacle ahead. Proceed with caution."
        }

        return SpeechUtterance(
            priority = SpeechPriority.INFO,
            text = defaultText
        )
    }

    private fun validateLlmAction(action: LlmAction, pathAdvice: PathAdvice): LlmAction {
        val clearances = pathAdvice.sectorClearanceM
        val leftClear = clearances[Sector.LEFT] ?: 2.0f
        val rightClear = clearances[Sector.RIGHT] ?: 2.0f
        val centerClear = clearances[Sector.CENTER] ?: 2.0f

        return when (action) {
            LlmAction.FORWARD -> if (centerClear < 1.2f) LlmAction.SLIGHT_LEFT else LlmAction.FORWARD
            LlmAction.LEFT, LlmAction.SLIGHT_LEFT -> if (leftClear < 1.2f) LlmAction.SLIGHT_RIGHT else action
            LlmAction.RIGHT, LlmAction.SLIGHT_RIGHT -> if (rightClear < 1.2f) LlmAction.SLIGHT_LEFT else action
            else -> action
        }
    }

    private fun getSafeFallbackSpeech(action: LlmAction): String {
        return when (action) {
            LlmAction.SLIGHT_LEFT -> "Path right blocked. Move slightly left."
            LlmAction.SLIGHT_RIGHT -> "Path left blocked. Move slightly right."
            LlmAction.FORWARD -> "Clear path, walk forward."
            else -> "Proceed with caution."
        }
    }
}
