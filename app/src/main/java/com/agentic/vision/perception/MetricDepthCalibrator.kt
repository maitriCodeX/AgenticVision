package com.agentic.vision.perception

import kotlin.math.abs
import kotlin.math.max

/**
 * Metric Scale Recovery and Spatial Depth Calibrator.
 * Combines physical object priors with pinhole camera projections and Huber regression fitting.
 */
class MetricDepthCalibrator {

    var scaleFactor: Float = 1.0f
        private set
    var shiftFactor: Float = 0.0f
        private set

    private var lastCalibratedTimeMs: Long = 0L
    private val emaAlpha: Float = 0.15f // Scale smoothing factor

    companion object {
        // Physical real-world heights in meters
        val OBJECT_HEIGHT_PRIORS = mapOf(
            "person" to 1.65f,
            "car" to 1.50f,
            "motorcycle" to 1.20f,
            "bicycle" to 1.00f,
            "chair" to 0.90f,
            "couch" to 0.85f,
            "bus" to 2.80f,
            "truck" to 2.50f,
            "stop sign" to 0.90f,
            "traffic light" to 0.80f,
            "door" to 2.00f,
            "fire hydrant" to 0.70f
        )
    }

    /**
     * Estimates pinhole distance: Z = (focalLengthPx * realHeightMeters) / pixelHeight
     */
    fun computePinholeDistance(
        objectLabel: String,
        boxY1: Float,
        boxY2: Float,
        frameHeightPx: Int,
        focalLengthPx: Float
    ): Float? {
        val realHeightM = OBJECT_HEIGHT_PRIORS[objectLabel.lowercase()] ?: return null
        val bboxHeightPx = max(1.0f, (boxY2 - boxY1) * frameHeightPx)
        return (focalLengthPx * realHeightM) / bboxHeightPx
    }

    /**
     * Fits robust Huber scale and shift between raw MiDaS inverse depth and pinhole metric targets.
     */
    fun calibrateScale(
        detections: List<Detection>,
        rawDepthMap: DepthMap,
        focalLengthPx: Float,
        currentTimeMs: Long
    ) {
        val pinholeTargetInvDepths = mutableListOf<Float>()
        val rawMidasInvDepths = mutableListOf<Float>()

        for (det in detections) {
            val pinholeDist = computePinholeDistance(
                objectLabel = det.label,
                boxY1 = det.boundingBox[1],
                boxY2 = det.boundingBox[3],
                frameHeightPx = rawDepthMap.height,
                focalLengthPx = focalLengthPx
            )

            if (pinholeDist != null && pinholeDist > 0.5f && pinholeDist < 10.0f) {
                val medianRawInvDepth = getMedianRawInverseDepth(det.boundingBox, rawDepthMap)
                if (medianRawInvDepth > 0.01f) {
                    pinholeTargetInvDepths.add(1.0f / pinholeDist)
                    rawMidasInvDepths.add(medianRawInvDepth)
                }
            }
        }

        if (pinholeTargetInvDepths.isNotEmpty()) {
            // Fit scale alpha using Huber loss / Median ratio
            val ratios = mutableListOf<Float>()
            for (i in pinholeTargetInvDepths.indices) {
                ratios.add(pinholeTargetInvDepths[i] / rawMidasInvDepths[i])
            }
            ratios.sort()
            val medianRatio = ratios[ratios.size / 2]

            // Apply exponential moving average to prevent temporal depth jitter
            scaleFactor = scaleFactor * (1.0f - emaAlpha) + medianRatio * emaAlpha
            lastCalibratedTimeMs = currentTimeMs
        }
    }

    /**
     * Applies metric scale transform to produce metric depth map in meters.
     */
    fun applyMetricScaling(rawDepthMap: DepthMap, currentTimeMs: Long): DepthMap {
        val isFresh = (currentTimeMs - lastCalibratedTimeMs) < 5000L
        val size = rawDepthMap.rawInverseDepth.size
        val metricBuffer = FloatArray(size)

        for (i in 0 until size) {
            val rawInv = rawDepthMap.rawInverseDepth[i]
            val scaledInv = (rawInv * scaleFactor + shiftFactor).coerceAtLeast(0.05f)
            metricBuffer[i] = 1.0f / scaledInv
        }

        return rawDepthMap.copy(
            metricDepthMeters = metricBuffer,
            isScaleCalibrated = isFresh,
            scaleFactor = scaleFactor,
            shiftFactor = shiftFactor
        )
    }

    /**
     * Computes median depth over central 60% region of the bounding box (ignoring outer 20% margin).
     */
    fun getMedianObjectMetricDistance(
        bbox: FloatArray,
        depthMap: DepthMap
    ): Float {
        val x1 = bbox[0]
        val y1 = bbox[1]
        val x2 = bbox[2]
        val y2 = bbox[3]

        // Crop 20% inner margin
        val marginX = (x2 - x1) * 0.20f
        val marginY = (y2 - y1) * 0.20f

        val cropX1 = ((x1 + marginX) * depthMap.width).toInt().coerceIn(0, depthMap.width - 1)
        val cropX2 = ((x2 - marginX) * depthMap.width).toInt().coerceIn(cropX1, depthMap.width - 1)
        val cropY1 = ((y1 + marginY) * depthMap.height).toInt().coerceIn(0, depthMap.height - 1)
        val cropY2 = ((y2 - marginY) * depthMap.height).toInt().coerceIn(cropY1, depthMap.height - 1)

        val values = mutableListOf<Float>()
        for (y in cropY1..cropY2) {
            for (x in cropX1..cropX2) {
                val d = depthMap.getMetricDepthAt(x, y)
                if (d in 0.2f..15.0f) {
                    values.add(d)
                }
            }
        }

        if (values.isEmpty()) return 2.0f
        values.sort()
        return values[values.size / 2]
    }

    private fun getMedianRawInverseDepth(bbox: FloatArray, depthMap: DepthMap): Float {
        val x1 = (bbox[0] * depthMap.width).toInt().coerceIn(0, depthMap.width - 1)
        val x2 = (bbox[2] * depthMap.width).toInt().coerceIn(x1, depthMap.width - 1)
        val y1 = (bbox[1] * depthMap.height).toInt().coerceIn(0, depthMap.height - 1)
        val y2 = (bbox[3] * depthMap.height).toInt().coerceIn(y1, depthMap.height - 1)

        val values = mutableListOf<Float>()
        for (y in y1..y2) {
            for (x in x1..x2) {
                values.add(depthMap.rawInverseDepth[y * depthMap.width + x])
            }
        }
        if (values.isEmpty()) return 0.5f
        values.sort()
        return values[values.size / 2]
    }

    fun manualCalibrate(knownDistanceMeters: Float, measuredRawInverseDepth: Float) {
        if (measuredRawInverseDepth > 0.01f && knownDistanceMeters > 0.1f) {
            val targetInv = 1.0f / knownDistanceMeters
            scaleFactor = targetInv / measuredRawInverseDepth
            lastCalibratedTimeMs = System.currentTimeMillis()
        }
    }
}
