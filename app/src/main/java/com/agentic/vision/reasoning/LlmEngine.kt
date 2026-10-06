package com.agentic.vision.reasoning

enum class LlmAction {
    FORWARD,
    SLIGHT_LEFT,
    LEFT,
    SLIGHT_RIGHT,
    RIGHT,
    STOP,
    WAIT,
    TURN_AROUND,
    GOAL_REACHED
}

data class LlmResponse(
    val action: LlmAction,
    val speech: String,
    val latencyMs: Long,
    val isValidJson: Boolean = true
)

interface LlmEngine {
    val name: String
    fun generateInstruction(systemPrompt: String, userPrompt: String): LlmResponse
    fun close()
}
