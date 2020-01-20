package com.golddev.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.rotationMatrix
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

class BitmapLoader(private val absoluteFilePath: String) {

    private val rotation: Int by lazy {
        val orientation = ExifInterface(absoluteFilePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return@lazy when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            else -> 0
        }
    }

    fun loadForMaximalSize(maximalWidth: Int, maximalHeight: Int): Bitmap {
        if (maximalWidth <= 0 || maximalHeight <= 0) {
            throw IllegalArgumentException("Width and height must be > 0 when loading a scaled bitmap")
        }

        val initialBitmap = loadScaledBitmap(maximalWidth, maximalHeight)
        val aspectRatio = initialBitmap.aspectRatio

        // Here we calculate values for the resulting width and height, so we conform to both the given maximalWidth and maximalHeight
        // while staying true to the initial aspect ratio of the image
        val (resultWidth, resultHeight) = fitMaximalDimensionsForAspectRatio(
            maximalWidth,
            maximalHeight,
            aspectRatio
        )
        val resultBitmap = Bitmap.createScaledBitmap(initialBitmap, resultWidth, resultHeight, true)
        if (resultBitmap !== initialBitmap) {
            initialBitmap.recycle()
        }
        return resultBitmap.applyExifRotation()
    }

    private fun decodeScaledBitmapOptions(
        minimalWidth: Int,
        minimalHeight: Int,
        rotation: Int
    ): BitmapFactory.Options =
        BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(absoluteFilePath, this)

            val isRotated = rotation % 180 != 0
            val imageWidth = if (isRotated) this.outHeight else this.outWidth
            val imageHeight = if (isRotated) this.outWidth else this.outHeight

            val scaleFactor =
                calculateInSampleSize(minimalWidth, minimalHeight, imageWidth, imageHeight)
            inSampleSize = scaleFactor
            inJustDecodeBounds = false
        }

    private fun loadScaledBitmap(minimalWidth: Int, minimalHeight: Int): Bitmap {
        val bitmapOptions = decodeScaledBitmapOptions(minimalWidth, minimalHeight, rotation)

        return BitmapFactory.decodeFile(absoluteFilePath, bitmapOptions)
    }

    /**
     * Calculates the scale factor to use when setting inSampleSize on [BitmapFactory.Options] for loading scaled Bitmaps into memory. The
     * result will always be a power of 2, as per the documentation.
     * @param targetWidth The minimal width of the resulting bitmap.
     * @param targetHeight The minimal height of the resulting bitmap.
     * @param imageWidth The original width of the bitmap.
     * @param imageHeight The original height of the bitmap.
     */
    private fun calculateInSampleSize(
        targetWidth: Int,
        targetHeight: Int,
        imageWidth: Int,
        imageHeight: Int
    ): Int {
        var result = 1
        while (imageWidth / (result * 2) >= targetWidth && imageHeight / (result * 2) >= targetHeight) {
            result *= 2
        }
        return result
    }

    /**
     * Creates a new Bitmap that is equal to the source bitmap rotated by the given amount of rotation.
     */
    private fun Bitmap.applyExifRotation(): Bitmap {
        var resultBitmap = this
        if (rotation > 0) {
            resultBitmap = Bitmap.createBitmap(
                this,
                0,
                0,
                this.width,
                this.height,
                rotationMatrix(rotation.toFloat()),
                true
            )
            this.recycle()
        }
        return resultBitmap
    }

    private val Bitmap.aspectRatio: Float
        get() = width.toFloat() / height.toFloat()

    fun fitMaximalDimensionsForAspectRatio(
        maximalWidth: Int,
        maximalHeight: Int,
        aspectRatio: Float
    ): IntArray =
        if (aspectRatio > 1f) {
            if (maximalHeight * aspectRatio < maximalWidth) {
                aspectRatio.fitDimensionsByHeight(maximalHeight)
            } else {
                aspectRatio.fitDimensionsByWidth(maximalWidth)
            }
        } else {
            if (maximalWidth / aspectRatio < maximalHeight) {
                aspectRatio.fitDimensionsByWidth(maximalWidth)
            } else {
                aspectRatio.fitDimensionsByHeight(maximalHeight)
            }
        }

    private fun Float.fitDimensionsByHeight(height: Int) =
        intArrayOf((height * this).roundToInt(), height)

    private fun Float.fitDimensionsByWidth(width: Int) =
        intArrayOf(width, (width / this).roundToInt())

}