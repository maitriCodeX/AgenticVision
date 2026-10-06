package com.agentic.vision.camera

import kotlinx.coroutines.flow.Flow

/**
 * Interface representing a hardware or simulated camera stream source.
 * Designed to support multi-camera arrays (e.g. 4-camera 360° neckband) seamlessly.
 */
interface CameraSource {
    val id: String
    val yawOffsetDeg: Float
    val fovDeg: Float
    val frames: Flow<Frame>

    fun start()
    fun stop()
}
