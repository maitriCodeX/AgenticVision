package com.agentic.vision.perception

import android.graphics.Bitmap

/**
 * Interface allowing swappable neural depth estimation backends (MiDaS v2.1 Small, Depth Anything V2, ARCore Depth API).
 */
interface DepthEstimator {
    val name: String
    fun estimateDepth(bitmap: Bitmap): DepthMap
    fun close()
}
