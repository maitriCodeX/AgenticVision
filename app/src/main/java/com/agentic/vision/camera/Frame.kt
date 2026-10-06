package com.agentic.vision.camera

import android.graphics.Bitmap

/**
 * Encapsulates a processed or raw video frame from any camera source.
 * Includes sensor calibration intrinsics required for accurate metric 3D perception.
 */
data class Frame(
    val frameId: Long,
    val timestampMs: Long,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val sourceId: String,
    val yawOffsetDeg: Float = 0.0f,
    val fovDeg: Float = 70.0f,
    val focalLengthPx: Float = 600.0f,
    val rotationDegrees: Int = 0
)
