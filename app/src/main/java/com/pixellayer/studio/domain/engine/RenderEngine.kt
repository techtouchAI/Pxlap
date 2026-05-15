package com.pixellayer.studio.domain.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.pixellayer.studio.data.model.ExportQuality
import com.pixellayer.studio.data.model.FilterType
import com.pixellayer.studio.data.model.Layer
import com.pixellayer.studio.data.model.LayerType
import com.pixellayer.studio.data.model.NonDestructiveFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Engine responsible for rendering layers to a final Bitmap for export.
 */
class RenderEngine {

    private val TAG = "RenderEngine"
    private lateinit var renderCanvas: Canvas
    private lateinit var basePaint: Paint
    private lateinit var renderMatrix: Matrix

    init {
        // Initialize expensive objects once
        basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            isDither = true
        }
        renderMatrix = Matrix()
    }

    /**
     * Renders the given layers to a Bitmap based on the requested quality.
     * Includes OOM fallback logic.
     */
    suspend fun renderToBitmap(layers: List<Layer>, initialQuality: ExportQuality): Bitmap? {
        return withContext(Dispatchers.Default) {
            var currentQuality = initialQuality
            var resultBitmap: Bitmap? = null

            // Fallback loop: 4K -> HD -> Draft
            while (resultBitmap == null) {
                try {
                    val width = currentQuality.targetWidth
                    val height = currentQuality.targetHeight

                    if (!BitmapMemoryManager.isMemoryAvailableForBitmap(width, height, Bitmap.Config.ARGB_8888)) {
                        throw OutOfMemoryError("Memory manager denied allocation for ${currentQuality.name}")
                    }

                    resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    renderCanvas = Canvas(resultBitmap)

                    // Draw all visible layers
                    // Sort by order just in case, though they should be sorted
                    layers.filter { it.isVisible }.sortedBy { it.order }.forEach { layer ->
                        drawLayer(layer, renderCanvas, width, height)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OOM while rendering at ${currentQuality.name}. Falling back...")
                    resultBitmap?.recycle()
                    resultBitmap = null

                    // Fallback logic
                    currentQuality = when (currentQuality) {
                        ExportQuality._4K -> ExportQuality.HD
                        ExportQuality.HD -> ExportQuality.Draft
                        ExportQuality.Draft -> {
                            Log.e(TAG, "Failed to render even at Draft quality.")
                            return@withContext null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rendering bitmap", e)
                    resultBitmap?.recycle()
                    return@withContext null
                }
            }
            resultBitmap
        }
    }

    private fun drawLayer(layer: Layer, canvas: Canvas, canvasWidth: Int, canvasHeight: Int) {
        canvas.save()

        // Apply Transform
        renderMatrix.reset()
        val cx = canvasWidth / 2f
        val cy = canvasHeight / 2f

        // Move to center, apply transform, move back
        renderMatrix.postTranslate(-cx, -cy)
        renderMatrix.postScale(layer.transform.scaleX, layer.transform.scaleY)
        renderMatrix.postRotate(layer.transform.rotation)
        renderMatrix.postTranslate(cx + layer.transform.translateX, cy + layer.transform.translateY)

        canvas.concat(renderMatrix)

        // Reset paint properties for each layer
        basePaint.reset()
        basePaint.isAntiAlias = true
        basePaint.isFilterBitmap = true

        // Apply shadow if enabled
        if (layer.shadow.isEnabled) {
            basePaint.setShadowLayer(
                layer.shadow.radius,
                layer.shadow.dx,
                layer.shadow.dy,
                layer.shadow.color
            )
        } else {
            basePaint.clearShadowLayer()
        }

        // Draw based on type
        when (val type = layer.type) {
            is LayerType.Image -> {
                // Apply Non-Destructive Filters
                val colorMatrix = ColorMatrix()
                applyFilters(colorMatrix, type.filters)
                basePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)

                // Dummy draw for illustration (replace with actual bitmap loading/drawing)
                // In a real implementation, you would load the URI into a Bitmap
                // val imageBitmap = loadBitmap(type.uri)
                // canvas.drawBitmap(imageBitmap, 0f, 0f, basePaint)

                // Placeholder representation
                basePaint.color = 0xFFCCCCCC.toInt()
                canvas.drawRect(cx - 200f, cy - 200f, cx + 200f, cy + 200f, basePaint)
                basePaint.color = 0xFF000000.toInt()
                canvas.drawText("Image: ${type.uri}", cx - 180f, cy, basePaint)
            }
            is LayerType.Text -> {
                basePaint.color = type.properties.color
                basePaint.textSize = 100f // Dummy size
                basePaint.strokeWidth = type.properties.strokeWidth
                if (type.properties.strokeWidth > 0f) {
                    basePaint.style = Paint.Style.FILL_AND_STROKE
                } else {
                    basePaint.style = Paint.Style.FILL
                }

                // Draw text roughly at center
                // Note: proper text rendering requires StaticLayout for multiline/alignment
                canvas.drawText(type.properties.text, cx, cy, basePaint)
            }
            is LayerType.Shape -> {
                basePaint.color = type.fillColor
                basePaint.style = Paint.Style.FILL
                val halfSize = 150f
                when (type.shapeType) {
                    com.pixellayer.studio.data.model.ShapeType.Rect -> {
                        canvas.drawRect(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, basePaint)
                        if(type.strokeWidth > 0) {
                            basePaint.style = Paint.Style.STROKE
                            basePaint.color = type.strokeColor
                            basePaint.strokeWidth = type.strokeWidth
                            canvas.drawRect(cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize, basePaint)
                        }
                    }
                    com.pixellayer.studio.data.model.ShapeType.Circle -> {
                        canvas.drawCircle(cx, cy, halfSize, basePaint)
                        if(type.strokeWidth > 0) {
                            basePaint.style = Paint.Style.STROKE
                            basePaint.color = type.strokeColor
                            basePaint.strokeWidth = type.strokeWidth
                            canvas.drawCircle(cx, cy, halfSize, basePaint)
                        }
                    }
                    com.pixellayer.studio.data.model.ShapeType.Triangle -> {
                        // Dummy triangle path
                        val path = android.graphics.Path()
                        path.moveTo(cx, cy - halfSize)
                        path.lineTo(cx + halfSize, cy + halfSize)
                        path.lineTo(cx - halfSize, cy + halfSize)
                        path.close()
                        canvas.drawPath(path, basePaint)
                        if(type.strokeWidth > 0) {
                            basePaint.style = Paint.Style.STROKE
                            basePaint.color = type.strokeColor
                            basePaint.strokeWidth = type.strokeWidth
                            canvas.drawPath(path, basePaint)
                        }
                    }
                }
            }
            is LayerType.Sticker -> {
                basePaint.color = 0xFFFFCC00.toInt()
                canvas.drawCircle(cx, cy, 100f, basePaint)
                basePaint.color = 0xFF000000.toInt()
                canvas.drawText("Sticker", cx - 40f, cy, basePaint)
            }
        }

        canvas.restore()
    }

    private fun applyFilters(matrix: ColorMatrix, filters: List<NonDestructiveFilter>) {
        for (filter in filters) {
            when (filter.type) {
                FilterType.None -> { /* No op */ }
                FilterType.Clarendon -> {
                    // Simplified approximation of Clarendon
                    val clarendon = ColorMatrix(floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.2f, 0f, 0f, 10f,
                        0f, 0f, 1.2f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.postConcat(clarendon)
                }
                FilterType.Gingham -> {
                    // Simplified approximation of Gingham
                    val gingham = ColorMatrix()
                    gingham.setSaturation(0.8f) // Desaturate
                    val tint = ColorMatrix(floatArrayOf(
                        1f, 0f, 0f, 0f, 20f,
                        0f, 1f, 0f, 0f, 20f,
                        0f, 0f, 1f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                    matrix.postConcat(gingham)
                    matrix.postConcat(tint)
                }
                FilterType.Moon -> {
                    // Simplified approximation of Moon (B&W)
                    val moon = ColorMatrix()
                    moon.setSaturation(0f)
                    matrix.postConcat(moon)
                }
            }
        }
    }
}
