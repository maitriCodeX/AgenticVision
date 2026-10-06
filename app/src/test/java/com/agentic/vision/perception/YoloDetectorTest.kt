package com.agentic.vision.perception

import org.junit.Assert.assertEquals
import org.junit.Test

class YoloDetectorTest {

    @Test
    fun testTensorParsingAndNavFiltering() {
        val detector = YoloDetector()
        detector.confThreshold = 0.5f
        detector.filterNavRelevantOnly = true

        // Synthetic YOLOv10 output tensor [1, 2, 6]
        // Obj 1: Person (classId 0), conf 0.90, bbox [100, 100, 300, 500] in 640x640 frame
        // Obj 2: Donut (classId 54), conf 0.85 (not nav relevant)
        val mockBatch = arrayOf(
            floatArrayOf(100f, 100f, 300f, 500f, 0.90f, 0.0f), // person
            floatArrayOf(50f, 50f, 100f, 100f, 0.85f, 54.0f)   // donut
        )
        val mockData = arrayOf(mockBatch)

        val detections = mutableListOf<Detection>()
        detector.parseOutputTensor(mockData, detections, 640, 640)

        assertEquals(1, detections.size)
        assertEquals("person", detections[0].label)
        assertEquals(0.90f, detections[0].confidence, 0.01f)
        assertEquals(100f / 640f, detections[0].boundingBox[0], 0.001f)
        assertEquals(300f / 640f, detections[0].boundingBox[2], 0.001f)

        // Turn off nav filtering, now donut should also be included
        detector.filterNavRelevantOnly = false
        val allDetections = mutableListOf<Detection>()
        detector.parseOutputTensor(mockData, allDetections, 640, 640)
        assertEquals(2, allDetections.size)
    }
}
