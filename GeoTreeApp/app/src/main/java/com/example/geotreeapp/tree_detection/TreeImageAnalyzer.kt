package com.example.geotreeapp.tree_detection

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.base.rotate
import com.example.geotreeapp.base.toBitmap
import com.example.geotreeapp.screens.camera_detection.TreeDetectionDrawingSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TreeImageAnalyzer(
    private val treeDetectionModel: TreeDetectionModel,
    private val treeDetectionDrawingSurface: TreeDetectionDrawingSurface
) : ImageAnalysis.Analyzer {
    private var isProcessing = false
    private val _treeBoxes: MutableLiveData<List<TreeBox>> = MutableLiveData()
    val treeBoxes: LiveData<List<TreeBox>>
        get() = _treeBoxes

    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true

        val imageBitmap = imageProxy.use {
             it.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        }

        if (!treeDetectionDrawingSurface.isTransformInitialized) {
            treeDetectionDrawingSurface.transformInit(imageBitmap.height, imageBitmap.width)
        }

        CoroutineScope(Dispatchers.Default).launch {
            runInference(imageBitmap)
        }
    }

    private suspend fun runInference(inputImage: Bitmap) {
        withContext(Dispatchers.Default) {
            val treeBoxes = treeDetectionModel.inference(inputImage)
            withContext(Dispatchers.Main) {
                isProcessing = false
                _treeBoxes.value = treeBoxes
                treeDetectionDrawingSurface.treeBoxes = treeBoxes
                treeDetectionDrawingSurface.invalidate()    // Call onDraw
            }
        }
    }

}