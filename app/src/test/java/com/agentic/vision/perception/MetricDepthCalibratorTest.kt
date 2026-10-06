package com.agentic.vision.perception

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricDepthCalibratorTest {

    @Test
    fun testPinholeDistanceCalculation() {
        val calibrator = MetricDepthCalibrator()
        // Person (1.65m tall), focalLength = 600px, bbox height = 300px in 600px frame (boxY1=0.2, boxY2=0.7 -> 0.5 ratio -> 300px)
        val dist = calibrator.computePinholeDistance(
            objectLabel = "person",
            boxY1 = 0.2f,
            boxY2 = 0.7f,
            frameHeightPx = 600,
            focalLengthPx = 600.0f
        )
        // Z = 600 * 1.65 / 300 = 3.30m
        assertEquals(3.30f, dist!!, 0.01f)
    }

    @Test
    fun testScaleCalibrationAndMetricScaling() {
        val calibrator = MetricDepthCalibrator()
        val rawBuffer = FloatArray(256 * 256) { 0.5f } // raw inverse depth 0.5
        val rawDepth = DepthMap(256, 256, rawBuffer, rawBuffer)

        // Known distance = 2.0m -> target inverse depth = 0.5
        calibrator.manualCalibrate(knownDistanceMeters = 2.0f, measuredRawInverseDepth = 0.5f)
        assertEquals(1.0f, calibrator.scaleFactor, 0.01f)

        val metricDepthMap = calibrator.applyMetricScaling(rawDepth, System.currentTimeMillis())
        val distAtCenter = metricDepthMap.getMetricDepthAt(128, 128)
        assertEquals(2.0f, distAtCenter, 0.01f)
    }

    @Test
    fun testDistanceEvaluatorMetrics() {
        val evaluator = DistanceEvaluator()
        evaluator.recordSample("person", groundTruthMeters = 2.0f, estimatedMeters = 2.2f) // error 0.2 (10%)
        evaluator.recordSample("chair", groundTruthMeters = 1.0f, estimatedMeters = 0.9f)  // error 0.1 (10%)

        val report = evaluator.computeReport()
        assertEquals(2, report.sampleCount)
        assertEquals(0.15f, report.maeMeters, 0.01f)
        assertEquals(10.0f, report.mapePercent, 0.1f)
    }
}
