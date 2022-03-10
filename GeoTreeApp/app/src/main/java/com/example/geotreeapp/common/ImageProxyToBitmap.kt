package com.example.geotreeapp.common

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy
import java.lang.NullPointerException
import java.nio.ByteBuffer


@SuppressLint("UnsafeOptInUsageError")
fun ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: throw NullPointerException("Image is null")

    val planes: Array<Image.Plane> = image.planes
    val buffer: ByteBuffer = planes[0].buffer
    val pixelStride: Int = planes[0].pixelStride
    val rowStride: Int = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * image.width
    val bitmap = Bitmap.createBitmap(
        image.width + rowPadding / pixelStride,
        image.height,
        Bitmap.Config.ARGB_8888
    )
    bitmap.copyPixelsFromBuffer(buffer)

    return bitmap
}