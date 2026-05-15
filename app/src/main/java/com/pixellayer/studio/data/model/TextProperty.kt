package com.pixellayer.studio.data.model

import kotlinx.serialization.Serializable

/**
 * Data class holding typography properties for a Text Layer.
 */
@Serializable
data class TextProperty(
    val text: String = "",
    val fontStyle: FontStyle = FontStyle.Normal,
    val fontWeight: FontWeight = FontWeight.Normal,
    val textAlign: TextAlign = TextAlign.Start,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 0f,
    val color: Int = 0xFFFFFFFF.toInt(), // Default white
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0x00000000.toInt(), // Transparent default
    val shadowRadius: Float = 0f,

    // 3D Rotation Support
    val rotateX: Float = 0f,
    val rotateY: Float = 0f,
    val rotateZ: Float = 0f,
    val perspective: Float = 800f
)
