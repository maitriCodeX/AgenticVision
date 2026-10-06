package com.agentic.vision.perception

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.agentic.vision.camera.FrameConverter
import java.io.File
import java.nio.FloatBuffer
import java.util.Collections

/**
 * MiDaS v2.1 Small ONNX Depth Estimator.
 * Input: 256x256 image normalized tensor.
 * Output: Relative inverse depth map.
 */
class MidasDepthEstimator(
    private val context: Context? = null,
    modelPath: String? = null
) : DepthEstimator {

    override val name: String = "MiDaS v2.1 Small (ONNX)"

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private val frameConverter = FrameConverter()

    init {
        try {
            ortEnv = OrtEnvironment.getEnvironment()
            if (modelPath != null && File(modelPath).exists()) {
                val opts = OrtSession.SessionOptions()
                try {
                    opts.addNnapi()
                } catch (e: Throwable) {
                    // CPU fallback
                }
                ortSession = ortEnv?.createSession(modelPath, opts)
            }
        } catch (e: Throwable) {
            ortEnv = null
            ortSession = null
        }
    }

    override fun estimateDepth(bitmap: Bitmap): DepthMap {
        val targetDim = 256
        val rawBuffer = FloatArray(targetDim * targetDim)

        val env = ortEnv
        val sess = ortSession

        if (env != null && sess != null) {
            try {
                val floatBuffer = frameConverter.bitmapToMidasFloatBuffer(bitmap, targetDim, targetDim)
                val inputTensor = OnnxTensor.createTensor(
                    env,
                    floatBuffer,
                    longArrayOf(1, 3, targetDim.toLong(), targetDim.toLong())
                )
                val results = sess.run(Collections.singletonMap("input", inputTensor))
                results?.use { res ->
                    val outputValue = res.get(0).value
                    parseMidasOutput(outputValue, rawBuffer)
                }
            } catch (e: Exception) {
                simulateInverseDepth(rawBuffer, targetDim, targetDim)
            }
        } else {
            simulateInverseDepth(rawBuffer, targetDim, targetDim)
        }

        val metricBuffer = FloatArray(targetDim * targetDim)
        for (i in rawBuffer.indices) {
            metricBuffer[i] = 1.0f / (rawBuffer[i].coerceAtLeast(0.05f))
        }

        return DepthMap(
            width = targetDim,
            height = targetDim,
            rawInverseDepth = rawBuffer,
            metricDepthMeters = metricBuffer
        )
    }

    private fun parseMidasOutput(outputValue: Any, outBuffer: FloatArray) {
        if (outputValue is Array<*>) {
            // [1, 256, 256]
            val matrix = outputValue[0]
            if (matrix is Array<*>) {
                var idx = 0
                for (r in 0 until 256) {
                    val row = matrix[r]
                    if (row is FloatArray) {
                        for (c in 0 until 256) {
                            if (idx < outBuffer.size) {
                                outBuffer[idx++] = row[c]
                            }
                        }
                    }
                }
            }
        }
    }

    private fun simulateInverseDepth(outBuffer: FloatArray, w: Int, h: Int) {
        // Vertical gradient simulating ground plane & objects ahead
        for (y in 0 until h) {
            val invDepthVal = 0.1f + (y.toFloat() / h.toFloat()) * 0.8f
            for (x in 0 until w) {
                outBuffer[y * w + x] = invDepthVal
            }
        }
    }

    override fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (e: Throwable) {
            // Ignore
        }
    }
}
