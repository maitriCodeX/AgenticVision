package com.agentic.vision.tracking

import com.agentic.vision.perception.Detection
import kotlin.math.hypot

data class TrackedObject(
    val id: Int,
    val label: String,
    val kalman: KalmanFilter,
    var age: Int = 1,
    var hits: Int = 1,
    var timeSinceUpdate: Int = 0,
    var lastBbox: FloatArray = floatArrayOf(0f, 0f, 0f, 0f),
    var lastConfidence: Float = 1.0f
)

/**
 * SORT/ByteTrack multi-object tracker using 2D ground distance & IoU data association.
 */
class Tracker {

    private var nextId = 1
    private val tracks = mutableListOf<TrackedObject>()
    private val maxAge = 5 // Delete track if lost for 5 consecutive frames
    private val minHits = 2 // Confirm track after 2 consecutive detections

    fun update(
        detections: List<Detection>,
        metricDistances: List<Float>,
        bearingsDeg: List<Float>,
        timestampMs: Long
    ): List<TrackedObject> {
        // 1. Predict state for all existing tracks
        for (tr in tracks) {
            tr.kalman.predict(timestampMs)
            tr.timeSinceUpdate++
            tr.age++
        }

        // 2. Data Association (Greedy Euclidean Distance on ground plane [x, z])
        val unmatchedDetections = detections.indices.toMutableSet()
        val matchedTracks = mutableSetOf<Int>()

        for (i in tracks.indices) {
            val tr = tracks[i]
            var bestDetIdx = -1
            var minDistance = 1.5f // 1.5m matching threshold

            for (j in unmatchedDetections) {
                val detLabel = detections[j].label
                if (detLabel == tr.label) {
                    val distM = metricDistances[j]
                    val bearingRad = Math.toRadians(bearingsDeg[j].toDouble()).toFloat()
                    val detX = distM * Math.sin(bearingRad.toDouble()).toFloat()
                    val detZ = distM * Math.cos(bearingRad.toDouble()).toFloat()

                    val distance = hypot(
                        (tr.kalman.posX - detX).toDouble(),
                        (tr.kalman.posZ - detZ).toDouble()
                    ).toFloat()

                    if (distance < minDistance) {
                        minDistance = distance
                        bestDetIdx = j
                    }
                }
            }

            if (bestDetIdx != -1) {
                // Update track with matched detection
                val distM = metricDistances[bestDetIdx]
                val bearingRad = Math.toRadians(bearingsDeg[bestDetIdx].toDouble()).toFloat()
                val detX = distM * Math.sin(bearingRad.toDouble()).toFloat()
                val detZ = distM * Math.cos(bearingRad.toDouble()).toFloat()

                tr.kalman.update(detX, detZ)
                tr.hits++
                tr.timeSinceUpdate = 0
                tr.lastBbox = detections[bestDetIdx].boundingBox
                tr.lastConfidence = detections[bestDetIdx].confidence

                unmatchedDetections.remove(bestDetIdx)
                matchedTracks.add(i)
            }
        }

        // 3. Create new tracks for unmatched detections
        for (j in unmatchedDetections) {
            val distM = metricDistances[j]
            val bearingRad = Math.toRadians(bearingsDeg[j].toDouble()).toFloat()
            val detX = distM * Math.sin(bearingRad.toDouble()).toFloat()
            val detZ = distM * Math.cos(bearingRad.toDouble()).toFloat()

            val newKf = KalmanFilter(detX, detZ, timestampMs)
            tracks.add(
                TrackedObject(
                    id = nextId++,
                    label = detections[j].label,
                    kalman = newKf,
                    lastBbox = detections[j].boundingBox,
                    lastConfidence = detections[j].confidence
                )
            )
        }

        // 4. Track Death & Cleanup
        tracks.removeAll { it.timeSinceUpdate > maxAge }

        // Return confirmed tracks
        return tracks.filter { it.hits >= minHits || it.timeSinceUpdate == 0 }
    }

    fun reset() {
        tracks.clear()
        nextId = 1
    }
}
