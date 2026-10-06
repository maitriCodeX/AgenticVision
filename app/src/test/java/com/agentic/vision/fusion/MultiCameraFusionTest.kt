package com.agentic.vision.fusion

import com.agentic.vision.camera.CameraSource
import com.agentic.vision.camera.Frame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class MockCameraSource(
    override val id: String,
    override val yawOffsetDeg: Float,
    override val fovDeg: Float = 120.0f
) : CameraSource {
    override val frames: Flow<Frame> = emptyFlow()
    override fun start() {}
    override fun stop() {}
}

class MultiCameraFusionTest {

    @Test
    fun testBearingTransformation() {
        val frontCam = MockCameraSource("front", 0.0f)
        val rightCam = MockCameraSource("right", 90.0f)
        val rearCam  = MockCameraSource("rear", 180.0f)
        val leftCam  = MockCameraSource("left", -90.0f)

        val fusion = MultiCameraFusion(listOf(frontCam, rightCam, rearCam, leftCam))

        // Object 10° right on front camera -> 10° body bearing
        assertEquals(10.0f, fusion.computeUserBodyBearing(10.0f, frontCam.yawOffsetDeg), 0.01f)

        // Object 0° center on right camera -> 90° body bearing
        assertEquals(90.0f, fusion.computeUserBodyBearing(0.0f, rightCam.yawOffsetDeg), 0.01f)

        // Object 10° right on rear camera -> -170° body bearing
        assertEquals(-170.0f, fusion.computeUserBodyBearing(10.0f, rearCam.yawOffsetDeg), 0.01f)

        // Object -10° left on left camera -> -100° body bearing
        assertEquals(-100.0f, fusion.computeUserBodyBearing(-10.0f, leftCam.yawOffsetDeg), 0.01f)
    }

    @Test
    fun testSpatialDeduplicationAcrossOverlaps() {
        val frontCam = MockCameraSource("front", 0.0f)
        val rightCam = MockCameraSource("right", 90.0f)

        val fusion = MultiCameraFusion(listOf(frontCam, rightCam))

        val frontObj = SceneObject(
            trackId = 1,
            label = "person",
            confidence = 0.85f,
            distanceM = 2.0f,
            bearingDeg = 45.0f, // camera bearing
            positionX = 1.4f,
            positionZ = 1.4f
        )

        val rightObj = SceneObject(
            trackId = 2,
            label = "person",
            confidence = 0.80f,
            distanceM = 2.0f,
            bearingDeg = -45.0f, // camera bearing on right camera -> body bearing 45°
            positionX = 1.4f,
            positionZ = 1.4f
        )

        val detections = mapOf(
            "front" to listOf(frontObj),
            "right" to listOf(rightObj)
        )

        val merged = fusion.mergeDetections(detections)
        // Should deduplicate duplicate person detected in overlapping FOV region
        assertEquals(1, merged.size)
        assertEquals(45.0f, merged[0].bearingDeg, 0.01f)
    }
}
