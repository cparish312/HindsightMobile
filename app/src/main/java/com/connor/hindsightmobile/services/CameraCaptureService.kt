package com.connor.hindsightmobile.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleService
import com.connor.hindsightmobile.utils.getCameraCaptureDirectory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CameraCaptureService : LifecycleService() {
    private var handler: Handler? = null

    private var cameraDevice: CameraDevice? = null // Make global var to ensure cleaner closing
    private var pendingCaptures = 2

    override fun onCreate() {
        Log.d("CameraCaptureService", "onCreate")
        handler = Handler(Looper.getMainLooper())
        takePictureFromBothCameras()
        super.onCreate()
    }

    fun takePictureFromBothCameras() {
        // Initialize the camera manager and get camera characteristics
        Thread.sleep(1000)
        setUpImageReader("0", "backCamera")
    }
    fun setUpImageReader(cameraId: String, cameraName: String) {
        cameraDevice?.close()
        Log.d("CameraCaptureService", "Setting up image reader for $cameraName")
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
        val outputSize = map.getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.width * it.height }!!
        Log.d("CameraCaptureService", "Output size for $cameraName: ${outputSize.width}x${outputSize.height}")

        // Create an ImageReader holding the maximum available size.
        val imageReader = ImageReader.newInstance(outputSize.width, outputSize.height, ImageFormat.JPEG, 2).apply {
            setOnImageAvailableListener({ reader ->
                Log.d("CameraCaptureService", "Image Available for $cameraName")
                val image = reader.acquireNextImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                Log.d("CameraCaptureService", "Image Acquired for $cameraName")
                saveImageData(bitmap, this@CameraCaptureService, cameraName)
                image.close()
                if (cameraName == "backCamera") {
                    setUpImageReader("1", "frontCamera" )
                } else{
                    onDestroy()
                }
                checkAndFinalize()
            }, null)
        }

        openCameraSafely(handler!!, this, cameraId, imageReader!!.surface)
    }

    fun createCameraCaptureSession(cameraDevice: CameraDevice, captureSurface: Surface) {
        Log.d("CameraCaptureSession", "Creating camera capture session")

        try {
            cameraDevice.createCaptureSession(listOf(captureSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(captureSurface)

                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                        set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    }

                    session.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                            Log.d("CameraCaptureSession", "Focus locked! Taking photo.")
                        }
                    }, null)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraCaptureSession", "Failed to configure capture session")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("CameraCaptureSession", "Failed to create capture session", e)
            cameraDevice.close()
        }
    }

    fun openCamera(context: Context, cameraId: String, captureSurface: Surface) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e("CameraPermission", "Camera permission not granted")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(cd: CameraDevice) {
                    Log.d("CameraAccess", "Camera opened successfully")
                    try {
                        cameraDevice = cd
                        createCameraCaptureSession(cameraDevice!!, captureSurface)
                    } catch (e: Exception) {
                        Log.e("CameraAccess", "Failed to handle camera properly", e)
                    }
                }

                override fun onDisconnected(cd: CameraDevice) {
                    cameraDevice?.close()
                }

                override fun onError(cd: CameraDevice, error: Int) {
                    cameraDevice?.close()
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openCameraSafely(handler: Handler, context: Context, cameraId: String, captureSurface: Surface) {
        try {
            openCamera(context, cameraId, captureSurface)
        } catch (e: CameraAccessException) {
            Log.e("CameraAccess", "Camera access error", e)
            if (e.reason == CameraAccessException.CAMERA_IN_USE) {
                handler.postDelayed({
                    openCameraSafely(handler, context, cameraId, captureSurface)
                }, 500) // Retry after 1 second
            } else {
                throw e
            }
        }
    }

    private fun saveImageData(bitmap: Bitmap, context: Context, cameraName: String?) {
        // Use the app's private storage directory
        val directory = getCameraCaptureDirectory(context)

        val file = File(directory, "${cameraName}_${System.currentTimeMillis()}.webp")
        try {
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, fos)
                Log.d("BackgroundRecorderService", "Image saved to ${file.absolutePath}")
            }
        } catch (e: IOException) {
            Log.e("BackgroundRecorderService", "Failed to save image", e)
        }
    }

    private fun checkAndFinalize() {
        cameraDevice?.close()
        pendingCaptures--
        if (pendingCaptures == 0) {
            onDestroy()  // Call onDestroy only when all captures are complete
        }
    }

    override fun onDestroy() {
        Log.d("CameraCaptureService", "onDestroy")
        // imageReader?.close()
        cameraDevice?.close()
        stopSelf()
        super.onDestroy()
    }
}

