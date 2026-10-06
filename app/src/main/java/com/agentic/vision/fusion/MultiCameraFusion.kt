package com.agentic.vision.fusion

import com.agentic.vision.camera.CameraSource
import com.agentic.vision.camera.Frame

/**
 * MultiCameraFusion stub for future 360° neckband hardware integration.
 *
 * Architecture & Design:
 * - Accepts 4 CameraSource streams positioned at:
 *   1. Front: yawOffset = 0°
 *   2. Right: yawOffset = +90°
 *   3. Rear:  yawOffset = +180°
 *   4. Left:  yawOffset = -90° (270°)
 * - Each detected object's local camera bearing ($\theta_{cam}$) is transformed into unified user-body space:
 *   $$\theta_{body} = \theta_{cam} + \text{yawOffsetDeg}$$
 * - Overlapping FOV regions (e.g. 120° FOVs creating 30° overlap zones between adjacent sensors) are resolved
 *   using Spatial Non-Maximum Suppression (NMS) based on 3D ground plane Euclidean distance.
 */
class MultiCameraFusion(
    val sources: List<CameraSource>
) {
    /**
     * Converts a camera-relative bearing to global user-body bearing.
     */
    fun computeUserBodyBearing(cameraBearingDeg: Float, sourceYawOffsetDeg: Float): Float {
        var bodyBearing = cameraBearingDeg + sourceYawOffsetDeg
        while (bodyBearing > 180.0f) bodyBearing -= 360.0f
        while (bodyBearing <= -180.0f) bodyBearing += 360.0f
        return bodyBearing
    }

    /**
     * Merges scene objects detected across multiple camera sources, performing spatial NMS across overlaps.
     */
    fun mergeDetections(sourceDetections: Map<String, List<SceneObject>>): List<SceneObject> {
        val allObjects = mutableListOf<SceneObject>()
        for ((sourceId, detections) in sourceDetections) {
            val source = sources.find { it.id == sourceId }
            val yawOffset = source?.yawOffsetDeg ?: 0.0f
            for (obj in detections) {
                val updatedBearing = computeUserBodyBearing(obj.bearingDeg, yawOffset)
                allObjects.add(obj.copy(bearingDeg = updatedBearing))
            }
        }

        // Deduplicate overlapping detections across sensors within 0.5m threshold
        val merged = mutableListOf<SceneObject>()
        for (candidate in allObjects) {
            val isDuplicate = merged.any { existing ->
                existing.label == candidate.label &&
                        Math.hypot(
                            (existing.positionX - candidate.positionX).toDouble(),
                            (existing.positionZ - candidate.positionZ).toDouble()
                        ) < 0.5
            }
            if (!isDuplicate) {
                merged.add(candidate)
            }
        }
        return merged
    }
}
