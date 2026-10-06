package com.agentic.vision.fusion

import com.agentic.vision.camera.Frame
import com.agentic.vision.perception.DepthMap
import com.agentic.vision.perception.Detection
import com.agentic.vision.perception.MetricDepthCalibrator
import com.agentic.vision.tracking.TrajectoryPredictor
import com.agentic.vision.tracking.Tracker
import kotlin.math.hypot

/**
 * Scene Fusion module merging 2D bounding boxes, metric depth maps, SORT tracking,
 * and Kalman motion predictions into unified 3D SceneObjects.
 */
class SceneFusion(
    private val calibrator: MetricDepthCalibrator = MetricDepthCalibrator(),
    private val tracker: Tracker = Tracker(),
    private val predictor: TrajectoryPredictor = TrajectoryPredictor()
) {

    fun fuseScene(
        frame: Frame,
        detections: List<Detection>,
        rawDepthMap: DepthMap
    ): List<SceneObject> {
        // 1. Scale calibration & metric depth map generation
        calibrator.calibrateScale(detections, rawDepthMap, frame.focalLengthPx, frame.timestampMs)
        val metricDepthMap = calibrator.applyMetricScaling(rawDepthMap, frame.timestampMs)

        // 2. Extract per-detection bearings and median metric distances
        val distances = mutableListOf<Float>()
        val bearings = mutableListOf<Float>()

        for (det in detections) {
            val bbox = det.boundingBox
            val centerX = (bbox[0] + bbox[2]) / 2.0f
            // Bearing in degrees (- left, + right) relative to horizontal FOV
            val bearingDeg = (centerX - 0.5f) * frame.fovDeg + frame.yawOffsetDeg

            val distM = calibrator.getMedianObjectMetricDistance(bbox, metricDepthMap)

            distances.add(distM)
            bearings.add(bearingDeg)
        }

        // 3. Multi-object tracking update
        val activeTracks = tracker.update(detections, distances, bearings, frame.timestampMs)

        // 4. Trajectory analysis & SceneObject generation
        val sceneObjects = mutableListOf<SceneObject>()

        for (tr in activeTracks) {
            val posX = tr.kalman.posX
            val posZ = tr.kalman.posZ
            val vx = tr.kalman.velVx
            val vz = tr.kalman.velVz

            val distanceM = hypot(posX.toDouble(), posZ.toDouble()).toFloat()
            val bearingDeg = Math.toDegrees(Math.atan2(posX.toDouble(), posZ.toDouble())).toFloat()

            val riskResult = predictor.analyzeTrajectory(posX, posZ, vx, vz)

            sceneObjects.add(
                SceneObject(
                    trackId = tr.id,
                    label = tr.label,
                    confidence = tr.lastConfidence,
                    distanceM = distanceM,
                    bearingDeg = bearingDeg,
                    lateralOffsetM = posX,
                    positionX = posX,
                    positionZ = posZ,
                    velocityVx = vx,
                    velocityVz = vz,
                    speed = riskResult.speedMps,
                    approachState = riskResult.approachState,
                    ttcSeconds = riskResult.ttcSeconds,
                    riskLevel = riskResult.riskLevel,
                    bbox = tr.lastBbox
                )
            )
        }

        return sceneObjects
    }

    fun reset() {
        tracker.reset()
    }
}
