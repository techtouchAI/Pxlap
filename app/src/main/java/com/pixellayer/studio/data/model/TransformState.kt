package com.pixellayer.studio.data.model

import kotlinx.serialization.Serializable

/**
 * Data class representing the transformation state of a layer.
 */
@Serializable
data class TransformState(
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val rotation: Float = 0.0f,
    val translateX: Float = 0.0f,
    val translateY: Float = 0.0f,
    val pivotX: Float = 0.5f, // Normalized pivot (0 to 1)
    val pivotY: Float = 0.5f  // Normalized pivot (0 to 1)
) {
    /**
     * Helper to update translation based on delta values.
     */
    fun translateBy(dx: Float, dy: Float): TransformState {
        return copy(translateX = translateX + dx, translateY = translateY + dy)
    }

    /**
     * Helper to update scale based on delta values.
     */
    fun scaleBy(ds: Float): TransformState {
        return copy(scaleX = scaleX * ds, scaleY = scaleY * ds)
    }

    /**
     * Helper to update rotation based on delta values.
     */
    fun rotateBy(dr: Float): TransformState {
        return copy(rotation = rotation + dr)
    }
}
