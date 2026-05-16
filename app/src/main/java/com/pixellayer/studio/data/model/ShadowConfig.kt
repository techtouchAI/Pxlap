package com.pixellayer.studio.data.model

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data class for dynamic shadow configuration.
 */
@Serializable
data class ShadowConfig(
    val isEnabled: Boolean = false,
    val radius: Float = 10f,
    val color: Int = 0x80000000.toInt(), // Default semi-transparent black
    val distance: Float = 15f,
    val lightAngleDegrees: Float = 45f // 0 to 360 degrees
) {
    /**
     * Calculates the X offset of the shadow based on the light angle and distance.
     * Uses trigonometry (cos(angle) * distance).
     */
    val dx: Float
        get() = (cos(Math.toRadians(lightAngleDegrees.toDouble())) * distance).toFloat()

    /**
     * Calculates the Y offset of the shadow based on the light angle and distance.
     * Uses trigonometry (sin(angle) * distance).
     */
    val dy: Float
        get() = (sin(Math.toRadians(lightAngleDegrees.toDouble())) * distance).toFloat()
}
