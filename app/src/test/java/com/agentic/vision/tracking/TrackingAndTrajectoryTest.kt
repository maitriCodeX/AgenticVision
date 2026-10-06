package com.agentic.vision.tracking

import com.agentic.vision.fusion.ApproachState
import com.agentic.vision.fusion.RiskLevel
import com.agentic.vision.perception.Detection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingAndTrajectoryTest {

    @Test
    fun testKalmanFilterConstantVelocityPrediction() {
        val initialTime = 1000L
        val kf = KalmanFilter(initialX = 0.0f, initialZ = 5.0f, initialTimestampMs = initialTime)

        // Step 1: Object moves towards user from 5.0m to 4.0m in 1 second (vz = -1.0 m/s)
        kf.predict(2000L) // dt = 1.0s
        kf.update(measX = 0.0f, measZ = 4.0f)

        assertEquals(0.0f, kf.posX, 0.1f)
        assertEquals(4.0f, kf.posZ, 0.2f)
        assertTrue("Longitudinal velocity should be negative (approaching)", kf.velVz < -0.2f)

        // Step 2: Next frame at 3000L
        kf.predict(3000L)
        // Predicted Z should be around 3.0m
        assertEquals(3.0f, kf.posZ, 0.5f)
    }

    @Test
    fun testTrajectoryPredictionTtcAndRisk() {
        val predictor = TrajectoryPredictor()

        // 1. Approaching motorbike at 2.0m, velocity vz = -1.5 m/s directly in corridor (x = 0)
        val resultApproaching = predictor.analyzeTrajectory(posX = 0.0f, posZ = 2.0f, vx = 0.0f, vz = -1.5f)
        assertEquals(ApproachState.APPROACHING, resultApproaching.approachState)
        assertNotNull(resultApproaching.ttcSeconds)
        assertTrue("TTC should be around 1.33 seconds", resultApproaching.ttcSeconds!! < 1.5f)
        assertEquals(RiskLevel.CRITICAL, resultApproaching.riskLevel)

        // 2. Crossing pedestrian at x = 2.0m, z = 3.0m, vx = -1.0 m/s crossing left into corridor (TTC = 1.7s -> WARNING)
        val resultCrossing = predictor.analyzeTrajectory(posX = 2.0f, posZ = 3.0f, vx = -1.0f, vz = 0.0f)
        assertEquals(ApproachState.CROSSING, resultCrossing.approachState)
        assertEquals(RiskLevel.WARNING, resultCrossing.riskLevel)

        // 3. Stationary chair at 0.8m in corridor (< 1.0m -> CRITICAL)
        val resultStationary = predictor.analyzeTrajectory(posX = 0.1f, posZ = 0.8f, vx = 0.0f, vz = 0.0f)
        assertEquals(ApproachState.STATIC, resultStationary.approachState)
        assertEquals(RiskLevel.CRITICAL, resultStationary.riskLevel)
    }

    @Test
    fun testTrackerAssociationAndConfirmation() {
        val tracker = Tracker()

        val det1 = Detection("person", 0, 0.9f, floatArrayOf(0.3f, 0.2f, 0.6f, 0.8f))
        val timestamp1 = 1000L

        // Frame 1: Initial detection creates unconfirmed track (hits = 1)
        var activeTracks = tracker.update(listOf(det1), listOf(3.0f), listOf(0.0f), timestamp1)
        assertEquals(1, activeTracks.size)

        // Frame 2: Second detection confirms track (hits = 2)
        val timestamp2 = 1100L
        activeTracks = tracker.update(listOf(det1), listOf(2.8f), listOf(0.0f), timestamp2)
        assertEquals(1, activeTracks.size)
        assertEquals(1, activeTracks[0].id)
        assertEquals("person", activeTracks[0].label)
    }
}
