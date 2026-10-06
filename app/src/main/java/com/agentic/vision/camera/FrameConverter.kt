package com.agentic.vision.camera

import android.graphics.Bitmap
import android.graphics.Matrix

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * High-performance frame converter reusing pre-allocated direct ByteBuffers
 * to prevent GC pressure and latency spikes during high-FPS AI inference.
 */
class FrameConverter {

    /**
     * Converts a Bitmap into an NCHW FloatBuffer normalized to [0.0, 1.0] for YOLOv10.
     */
    fun bitmapToFloatBufferNCHW(
        bitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        outBuffer: FloatBuffer? = null
    ): FloatBuffer {
        val scaledBitmap = if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        val capacity = 3 * targetWidth * targetHeight
        val buffer = outBuffer ?: ByteBuffer.allocateDirect(capacity * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        buffer.rewind()

        val pixels = IntArray(targetWidth * targetHeight)
        scaledBitmap.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        val channelSize = targetWidth * targetHeight
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            buffer.put(i, r)
            buffer.put(channelSize + i, g)
            buffer.put(channelSize * 2 + i, b)
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Converts a Bitmap for MiDaS depth input (typically 256x256), with standard MiDaS normalization.
     */
    fun bitmapToMidasFloatBuffer(
        bitmap: Bitmap,
        targetWidth: Int = 256,
        targetHeight: Int = 256
    ): FloatBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val buffer = ByteBuffer.allocateDirect(3 * targetWidth * targetHeight * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val pixels = IntArray(targetWidth * targetHeight)
        scaled.getPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)
        val channelSize = targetWidth * targetHeight

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (((pixel shr 16) and 0xFF) / 255.0f - mean[0]) / std[0]
            val g = (((pixel shr 8) and 0xFF) / 255.0f - mean[1]) / std[1]
            val b = ((pixel and 0xFF) / 255.0f - mean[2]) / std[2]

            buffer.put(i, r)
            buffer.put(channelSize + i, g)
            buffer.put(channelSize * 2 + i, b)
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Rotates a bitmap to account for device camera sensor orientation.
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0.0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
