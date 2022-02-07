package com.example.geotreeapp.sensors

import android.Manifest
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.Executors

class CameraService(): Service() {
    inner class CameraServiceBinder: Binder() { fun getService(): CameraService = this@CameraService }
    private val binder = CameraServiceBinder()
    override fun onBind(intent: Intent?): IBinder { return binder }

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val PERMISSIONS_REQUEST_CODE = 3
    }

    private fun checkPermissions() = GpsService.REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    fun requirePermissions(activity: Activity){
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        imageAnalyzer: ImageAnalysis.Analyzer
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()


            val preview = Preview
                .Builder()
                .build()
                .also { it.setSurfaceProvider(surfaceProvider) }

            val resolution = resources.displayMetrics.let { Size(it.widthPixels, it.heightPixels) }

            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetResolution(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply { setAnalyzer(Executors.newSingleThreadExecutor(), imageAnalyzer) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *arrayOf(imageAnalysis, preview).filterNotNull().toTypedArray()
                )
            } catch (e: Exception) {
                Timber.e("Failed to start camera", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera(context: Context){
        ProcessCameraProvider.getInstance(context).get().unbindAll()
    }
}