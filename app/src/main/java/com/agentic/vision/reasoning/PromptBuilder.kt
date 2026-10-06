package com.agentic.vision.reasoning

import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.navigation.PathAdvice

/**
 * Builds compact, constrained prompt input (under 300 tokens) for Phi-3 Mini.
 */
class PromptBuilder {

    fun buildSystemPrompt(): String {
        return """
            You are an Agentic Vision Navigation Assistant for visually impaired users.
            Output strict JSON with fields:
            {
              "action": "FORWARD" | "SLIGHT_LEFT" | "LEFT" | "SLIGHT_RIGHT" | "RIGHT" | "STOP" | "WAIT" | "TURN_AROUND" | "GOAL_REACHED",
              "speech": "concise guidance under 12 words"
            }
            Rules:
            1. Only refer to objects present in the input scene.
            2. Never recommend turning into a blocked sector.
        """.trimIndent()
    }

    fun buildSceneUserPrompt(
        goal: GroundedGoal,
        sceneObjects: List<SceneObject>,
        pathAdvice: PathAdvice,
        previousInstruction: String?
    ): String {
        val topObjects = sceneObjects.take(8).map { obj ->
            mapOf(
                "label" to obj.label,
                "dist_m" to String.format("%.1f", obj.distanceM),
                "bearing_deg" to obj.bearingDeg.toInt(),
                "state" to obj.approachState.name,
                "risk" to obj.riskLevel.name
            )
        }

        val sectors = pathAdvice.sectorClearanceM.mapKeys { it.key.name }

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{")
        jsonBuilder.append("\"goal\":\"${goal.rawGoalText}\",")
        jsonBuilder.append("\"target_class\":\"${goal.targetClass ?: "none"}\",")
        jsonBuilder.append("\"objects\":[")
        jsonBuilder.append(topObjects.joinToString(",") { obj ->
            "{\"label\":\"${obj["label"]}\",\"dist\":${obj["dist_m"]},\"bearing\":${obj["bearing_deg"]},\"risk\":\"${obj["risk"]}\"}"
        })
        jsonBuilder.append("],")
        jsonBuilder.append("\"clearances\":{")
        jsonBuilder.append(sectors.entries.joinToString(",") { "\"${it.key}\":${String.format("%.1f", it.value)}" })
        jsonBuilder.append("},")
        jsonBuilder.append("\"prev_instruction\":\"${previousInstruction ?: "none"}\"")
        jsonBuilder.append("}")

        return jsonBuilder.toString()
    }
}
