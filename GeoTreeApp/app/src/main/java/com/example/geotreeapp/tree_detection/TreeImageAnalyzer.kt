package com.example.geotreeapp.tree_detection

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.base.rotate
import com.example.geotreeapp.base.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TreeImageAnalyzer(
    private val treeDetectionModel: TreeDetectionModel
) : ImageAnalysis.Analyzer {
    private var isProcessing = false

    private val _detectionPayload: MutableLiveData<DetectionPayload> = MutableLiveData()
    val detectionPayload: LiveData<DetectionPayload>
        get() = _detectionPayload

    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true

        val imageBitmap = imageProxy.use {
             it.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        CoroutineScope(Dispatchers.Default).launch {
            runInference(imageBitmap)
        }
    }

    private suspend fun runInference(inputImage: Bitmap) {
        withContext(Dispatchers.Default) {
            val inferenceResult = treeDetectionModel.inference(inputImage)
            withContext(Dispatchers.Main) {
                isProcessing = false
                _detectionPayload.value = DetectionPayload(
                    inferenceResult.detections,
                    inputImage.height,
                    inputImage.width,
                    inferenceResult.inferenceTime
                )
            }
        }
    }
}

data class DetectionPayload(
    val detections: List<TreeDetection>,
    val imageHeight: Int,
    val imageWidth: Int,
    val inferenceTime: Long
)