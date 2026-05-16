package com.pixellayer.studio.domain.engine

import android.graphics.BitmapFactory
import android.util.Log

/**
 * Utility to manage Bitmap memory and prevent OutOfMemory (OOM) errors.
 */
object BitmapMemoryManager {

    private const val TAG = "BitmapMemoryManager"

    /**
     * Calculates the appropriate inSampleSize to downsample the image if it's too large.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Checks if there's enough memory to allocate for a bitmap.
     * Fallback mechanism if memory is low.
     */
    fun isMemoryAvailableForBitmap(width: Int, height: Int, config: android.graphics.Bitmap.Config): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = runtime.maxMemory() - usedMemory

        // Assume 4 bytes per pixel for ARGB_8888
        val bytesPerPixel = if (config == android.graphics.Bitmap.Config.ARGB_8888) 4 else 2
        val requiredMemory = width.toLong() * height.toLong() * bytesPerPixel

        // Leave a safety margin (e.g., 20%)
        val safeMargin = requiredMemory * 1.2

        return if (availableMemory >= safeMargin) {
            true
        } else {
            Log.w(TAG, "Not enough memory for bitmap: Req: $requiredMemory, Avail: $availableMemory")
            false
        }
    }
}
