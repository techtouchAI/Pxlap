package com.pixellayer.studio.data.model

import kotlinx.serialization.Serializable

/**
 * Defines the export quality settings.
 */
@Serializable
enum class ExportQuality(val targetWidth: Int, val targetHeight: Int) {
    Draft(1080, 1080),
    HD(2048, 2048),
    _4K(3840, 3840) // Using _4K because enum cannot start with a number
}

/**
 * Font style choices for typography.
 */
@Serializable
enum class FontStyle { Normal, Italic }

/**
 * Font weight choices for typography.
 */
@Serializable
enum class FontWeight { Normal, Bold, Light }

/**
 * Text alignment choices for typography.
 */
@Serializable
enum class TextAlign { Start, Center, End }

/**
 * Available shape types.
 */
@Serializable
enum class ShapeType { Rect, Circle, Triangle }

/**
 * Available non-destructive color filters.
 */
@Serializable
enum class FilterType { None, Clarendon, Gingham, Moon }

/**
 * Represents a non-destructive filter.
 */
@Serializable
data class NonDestructiveFilter(
    val type: FilterType,
    val intensity: Float = 1.0f
)

/**
 * Represents the type of layer.
 * Using a sealed class for polymorphic serialization and exhaustiveness in `when` blocks.
 */
@Serializable
sealed class LayerType {

    /**
     * Text layer type with typography properties.
     */
    @Serializable
    data class Text(val properties: TextProperty) : LayerType()

    /**
     * Image layer type with non-destructive filters.
     */
    @Serializable
    data class Image(
        val uri: String, // Or any identifier like path, byte array, etc.
        val filters: List<NonDestructiveFilter> = emptyList()
    ) : LayerType()

    /**
     * Shape layer type with editable properties.
     */
    @Serializable
    data class Shape(
        val shapeType: ShapeType,
        val fillColor: Int,
        val strokeColor: Int,
        val strokeWidth: Float
    ) : LayerType()

    /**
     * Sticker layer type.
     */
    @Serializable
    data class Sticker(val resId: Int) : LayerType()
}
