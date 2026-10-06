package com.agentic.vision.reasoning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmEvalHarnessTest {

    @Test
    fun testGoalGrounder() {
        val grounder = GoalGrounder()

        // Supported goal
        val chairGoal = grounder.groundGoal("find a chair")
        assertEquals("chair", chairGoal.targetClass)
        assertTrue(chairGoal.isSupportedInDetector)

        // Unsupported goal (door)
        val doorGoal = grounder.groundGoal("find the exit door")
        assertTrue("Unsupported goal should be marked not supported", !doorGoal.isSupportedInDetector)
    }

    @Test
    fun testInstructionArbiterPreemption() {
        val arbiter = InstructionArbiter()

        val safetyAlert = SafetyAlert(
            priority = SafetyPriority.CRITICAL,
            spokenInstruction = "Stop! Car approaching!"
        )

        val llmResponse = LlmResponse(
            action = LlmAction.FORWARD,
            speech = "Walk forward.",
            latencyMs = 150
        )

        val pathAdvice = com.agentic.vision.navigation.PathAdvice(
            recommendedHeadingDeg = 0.0f,
            sectorClearanceM = mapOf(com.agentic.vision.navigation.Sector.CENTER to 0.8f),
            isClearAhead = false
        )

        val finalUtterance = arbiter.arbitrate(safetyAlert, llmResponse, pathAdvice)
        assertEquals(com.agentic.vision.speech.SpeechPriority.CRITICAL, finalUtterance.priority)
        assertEquals("Stop! Car approaching!", finalUtterance.text)
    }

    @Test
    fun testLlmEvalHarnessPassRate() {
        val harness = LlmEvalHarness()
        val result = harness.runEvaluation()

        assertEquals(22, result.totalScenarios)
        assertTrue("Pass rate should be above 90%", result.passRatePercent >= 90.0f)
    }
}
