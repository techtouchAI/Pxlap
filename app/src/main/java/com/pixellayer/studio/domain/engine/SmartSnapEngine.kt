package com.pixellayer.studio.domain.engine

import com.pixellayer.studio.data.model.TransformState
import kotlin.math.abs

/**
 * Engine to calculate magnetic snapping to canvas center and other guides.
 */
class SmartSnapEngine(
    private val snapThreshold: Float = 20f
) {
    /**
     * Represents the result of a snap operation.
     */
    data class SnapResult(
        val snappedX: Float?, // The translation X if snapped, null otherwise
        val snappedY: Float?, // The translation Y if snapped, null otherwise
        val isSnappedHorizontal: Boolean,
        val isSnappedVertical: Boolean
    )

    /**
     * Calculates snapping based on current transform and canvas dimensions.
     */
    fun calculateSnap(
        transform: TransformState,
        layerWidth: Float,
        layerHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): SnapResult {
        // Calculate the center of the layer in the canvas coordinate system
        // Assuming translation is the offset from the default center position
        val layerCenterX = (canvasWidth / 2f) + transform.translateX
        val layerCenterY = (canvasHeight / 2f) + transform.translateY

        val canvasCenterX = canvasWidth / 2f
        val canvasCenterY = canvasHeight / 2f

        var newTranslateX: Float? = null
        var newTranslateY: Float? = null
        var snappedH = false
        var snappedV = false

        // Check horizontal snap (vertical guide at center X)
        if (abs(layerCenterX - canvasCenterX) < snapThreshold) {
            newTranslateX = 0f // Snap back to 0 translation relative to center
            snappedH = true
        }

        // Check vertical snap (horizontal guide at center Y)
        if (abs(layerCenterY - canvasCenterY) < snapThreshold) {
            newTranslateY = 0f // Snap back to 0 translation relative to center
            snappedV = true
        }

        return SnapResult(
            snappedX = newTranslateX,
            snappedY = newTranslateY,
            isSnappedHorizontal = snappedH,
            isSnappedVertical = snappedV
        )
    }
}
