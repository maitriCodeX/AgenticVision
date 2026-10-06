package com.agentic.vision.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * CameraSource implementation for the back camera on Android phones.
 * Exposes a thread-safe Flow of Frames using CameraX STRATEGY_KEEP_ONLY_LATEST.
 */
class PhoneCameraSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : CameraSource {

    override val id: String = "phone_back_camera"
    override val yawOffsetDeg: Float = 0.0f
    override var fovDeg: Float = 70.0f
        private set

    var focalLengthPx: Float = 600.0f
        private set

    private val _frames = MutableSharedFlow<Frame>(replay = 0, extraBufferCapacity = 1)
    override val frames: Flow<Frame> = _frames.asSharedFlow()

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var frameCounter = 0L
    private val frameConverter = FrameConverter()

    init {
        extractCameraIntrinsics()
    }

    private fun extractCameraIntrinsics() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            cameraManager?.let { manager ->
                for (cameraId in manager.cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        val physicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

                        if (focalLengths != null && focalLengths.isNotEmpty() && physicalSize != null) {
                            val fMm = focalLengths[0]
                            val sensorWidthMm = physicalSize.width
                            // Horizontal FOV = 2 * atan(sensorWidth / (2 * f))
                            val hFovRad = 2.0 * Math.atan((sensorWidthMm / (2.0 * fMm)).toDouble())
                            fovDeg = Math.toDegrees(hFovRad).toFloat()

                            // Approx focal length in pixels for standard 640 width frame
                            focalLengthPx = (640.0f / (2.0f * Math.tan(hFovRad / 2.0))).toFloat()
                        }
                        break
                    }
                }
            }
        } catch (e: Exception) {
            fovDeg = 70.0f
            focalLengthPx = 600.0f
        }
    }

    override fun start() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 480))
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stop() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            if (cameraProviderFuture.isDone) {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val timestamp = System.currentTimeMillis()
        val bitmap = imageProxyToBitmap(imageProxy)
        imageProxy.close()

        if (bitmap != null) {
            val rotatedBitmap = frameConverter.rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
            val frame = Frame(
                frameId = ++frameCounter,
                timestampMs = timestamp,
                bitmap = rotatedBitmap,
                width = rotatedBitmap.width,
                height = rotatedBitmap.height,
                sourceId = id,
                yawOffsetDeg = yawOffsetDeg,
                fovDeg = fovDeg,
                focalLengthPx = focalLengthPx,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )
            scope.launch {
                _frames.emit(frame)
            }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val nv21Buffer = yuv420888ToNv21(imageProxy)
        val yuvImage = YuvImage(
            nv21Buffer,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 90, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.width * image.height
        val out = ByteArray(pixelCount + pixelCount / 2)

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        yBuffer.get(out, 0, pixelCount)

        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        var position = pixelCount
        val vRowStride = image.planes[2].rowStride
        val uRowStride = image.planes[1].rowStride
        val vPixelStride = image.planes[2].pixelStride
        val uPixelStride = image.planes[1].pixelStride

        val height = image.height / 2
        val width = image.width / 2

        for (row in 0 until height) {
            for (col in 0 until width) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * uRowStride + col * uPixelStride
                if (vIndex < vSize) out[position++] = vBuffer.get(vIndex)
                if (uIndex < uSize) out[position++] = uBuffer.get(uIndex)
            }
        }
        return out
    }
}
