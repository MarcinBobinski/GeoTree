package com.example.geotreeapp.tree_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber

class TreeDetectionModel(context: Context) {
    companion object {
        private const val MODEL_NAME = "tree_ssd_mobilenet_v2_640.tflite"
        private const val THREADS_NUM = 4

        private const val INPUT_IMAGE_SIZE = 640
        private val INPUT_DATA_TYPE = DataType.FLOAT32

        private const val MAX_NUM_DETECTIONS = 100
        private const val MIN_CONFIDENCE = 0.6f
    }

    private val inputImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(CastOp(INPUT_DATA_TYPE))
        .add(NormalizeOp(127.5f, 127.5f)) // Normalize input data (-1,1)
        .build()

    private var interpreter: Interpreter

    init {
        val interpreterOptions = Interpreter.Options().apply {
//            addDelegate(GpuDelegate())
            setNumThreads(THREADS_NUM)
            setUseXNNPACK(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setUseNNAPI(true)
                Timber.i("NNAPI acceleration is included")
            }
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(context, MODEL_NAME), interpreterOptions)
    }

    fun inference(inputImage: Bitmap): InferenceOutput {
        val boxes =
            TensorBuffer.createFixedSize(intArrayOf(MAX_NUM_DETECTIONS, 4), DataType.FLOAT32)
        val scores =
            TensorBuffer.createFixedSize(intArrayOf(MAX_NUM_DETECTIONS), DataType.FLOAT32)
        val classes =
            TensorBuffer.createFixedSize(intArrayOf(MAX_NUM_DETECTIONS), DataType.FLOAT32)
        val detectionNum = TensorBuffer.createFixedSize(intArrayOf(MAX_NUM_DETECTIONS), DataType.FLOAT32)

        val outputs = mapOf(
            1 to boxes.buffer,
            3 to classes.buffer,
            0 to scores.buffer,
            2 to detectionNum.buffer
        )

        val tensorImage = TensorImage.fromBitmap(inputImage).let { inputImageProcessor.process(it) }

        Timber.i("Inference START")

        var inferenceTime = System.currentTimeMillis()
        interpreter.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)
        inferenceTime = System.currentTimeMillis() - inferenceTime

        Timber.i("Inference time: $inferenceTime ms.")
        Timber.i("Inference results: ${scores.floatArray.take(10).joinToString(separator = ", ")}")

        return prepareOutput(inferenceTime, scores, boxes, inputImage.width, inputImage.height)
    }


    private fun prepareOutput(
        inferenceTime: Long,
        scores: TensorBuffer,
        boxes: TensorBuffer,
        inputImageWidth: Int,
        inputImageHeight: Int
    ): InferenceOutput {
        val scoresFloatArray = scores.floatArray
        val boxesFloatArray = boxes.floatArray

        val treeBoxes = mutableListOf<TreeDetection>()

        for (i in scoresFloatArray.indices) {
            if (scoresFloatArray[i] >= MIN_CONFIDENCE) {
                treeBoxes.add(
                    TreeDetection(
                        Rect(
                            (boxesFloatArray[(i * 4) + 1] * inputImageWidth).toInt()
                                .coerceAtLeast(0),
                            (boxesFloatArray[(i * 4) + 0] * (inputImageHeight)).toInt()
                                .coerceAtLeast(0),
                            (boxesFloatArray[(i * 4) + 3] * inputImageWidth - 1).toInt()
                                .coerceAtMost(inputImageWidth),
                            (boxesFloatArray[(i * 4) + 2] * inputImageHeight).toInt()
                                .coerceAtMost(inputImageHeight - 1)
                        ),
                        scoresFloatArray[i]
                    )
                )
            }
        }

        return InferenceOutput(inferenceTime, treeBoxes)
    }
}

data class InferenceOutput(
    val inferenceTime: Long,
    val detections: List<TreeDetection>
)
data class TreeDetection(val rect: Rect, val score: Float)