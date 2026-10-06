package com.agentic.vision.navigation

import com.agentic.vision.fusion.ApproachState
import com.agentic.vision.fusion.RiskLevel
import com.agentic.vision.fusion.SceneObject
import com.agentic.vision.perception.DepthMap
import com.agentic.vision.reasoning.SafetyLayer
import com.agentic.vision.reasoning.SafetyPriority
import com.agentic.vision.speech.SpeechPriority
import com.agentic.vision.speech.SpeechPrioritiser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyAndFreeSpaceTest {

    @Test
    fun testFreeSpaceAnalyzerSectorsAndHeading() {
        val analyzer = FreeSpaceAnalyzer()
        val rawBuffer = FloatArray(256 * 256) { 0.5f } // raw inverse depth 0.5
        val metricBuffer = FloatArray(256 * 256) { 2.0f } // 2.0m metric depth
        val depthMap = DepthMap(256, 256, rawBuffer, metricBuffer)

        // Center blocked by obstacle at 0.8m
        val obstacleCenter = SceneObject(
            trackId = 1,
            label = "chair",
            confidence = 0.9f,
            distanceM = 0.8f,
            bearingDeg = 0.0f // CENTER sector
        )

        val advice = analyzer.analyze(depthMap, listOf(obstacleCenter))
        assertEquals(5, advice.sectorClearanceM.size)
        assertEquals(0.8f, advice.sectorClearanceM[Sector.CENTER]!!, 0.01f)
        // Recommended heading should veer left (-20° or -40°) away from center obstacle
        assertTrue("Recommended heading should veer left away from center obstacle", advice.recommendedHeadingDeg < 0.0f)
    }

    @Test
    fun testSafetyLayerCriticalPreemption() {
        val safetyLayer = SafetyLayer()

        val criticalMotorbike = SceneObject(
            trackId = 2,
            label = "motorbike",
            confidence = 0.92f,
            distanceM = 1.2f,
            bearingDeg = 20.0f,
            approachState = ApproachState.APPROACHING,
            riskLevel = RiskLevel.CRITICAL
        )

        val pathAdvice = PathAdvice(
            recommendedHeadingDeg = -20.0f,
            sectorClearanceM = mapOf(Sector.CENTER to 2.0f),
            isClearAhead = true
        )

        val alert = safetyLayer.evaluate(listOf(criticalMotorbike), pathAdvice)
        assertEquals(SafetyPriority.CRITICAL, alert.priority)
        assertTrue("Alert should tell user to Stop", alert.spokenInstruction.startsWith("Stop"))
    }

    @Test
    fun testSpeechPrioritiserCooldown() {
        val prioritiser = SpeechPrioritiser()
        val warningText = "Caution. Car 2.0 metres right."

        // First attempt should succeed
        assertTrue(prioritiser.shouldQueueUtterance(warningText, SpeechPriority.WARNING, 1000L))

        // Immediate repeat of same warning within 2.5s should be suppressed
        assertFalse(prioritiser.shouldQueueUtterance(warningText, SpeechPriority.WARNING, 2000L))

        // CRITICAL alert should always pass even if repeated
        assertTrue(prioritiser.shouldQueueUtterance("Stop!", SpeechPriority.CRITICAL, 2100L))
        assertTrue(prioritiser.shouldQueueUtterance("Stop!", SpeechPriority.CRITICAL, 2200L))
    }
}
