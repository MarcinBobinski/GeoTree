package com.example.geotreeapp.screens.camera_detection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView
import androidx.core.graphics.toRectF
import com.example.geotreeapp.tree_detection.TreeDetection

class TreeDetectionDrawingSurface(
    context: Context,
    attributeSet: AttributeSet
) : SurfaceView(context, attributeSet) {
    var treeDetections: List<TreeDetection>? = null

    var isTransformInitialized = false
    private var modelToSurfaceTransform: Matrix = Matrix()

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        if (isTransformInitialized) {
            treeDetections?.forEach {
                it.rect.toRectF()
                    .apply { modelToSurfaceTransform.mapRect(this) }
                    .run {
                        canvas.drawRect(this, RECT_PAINT)
                    }
            }
        }
    }

    fun transformInit(modelOutputHeight: Int, modelOutputWidth: Int){
        modelToSurfaceTransform.preScale(
            width.toFloat() / modelOutputWidth.toFloat(),
            height.toFloat() / modelOutputHeight.toFloat()
        )
        isTransformInitialized = true
    }

    companion object {
        private val RECT_PAINT = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = Color.RED
        }
    }
}