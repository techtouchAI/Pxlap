package com.pixellayer.studio.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Wrapper data class representing a single layer in the workspace.
 */
@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: LayerType,
    val transform: TransformState = TransformState(),
    val shadow: ShadowConfig = ShadowConfig(),
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val order: Int = 0
) {
    companion object {
        /**
         * Factory method to create a new Text Layer.
         */
        fun createText(text: String, order: Int): Layer {
            val defaultTextProperty = TextProperty(text = text)
            return Layer(
                name = "Text Layer",
                type = LayerType.Text(defaultTextProperty),
                order = order
            )
        }

        /**
         * Factory method to create a new Image Layer.
         */
        fun createImage(uri: String, name: String = "Image Layer", order: Int): Layer {
            return Layer(
                name = name,
                type = LayerType.Image(uri = uri),
                order = order
            )
        }

        /**
         * Factory method to create a new Shape Layer.
         */
        fun createShape(
            shapeType: ShapeType,
            fillColor: Int,
            strokeColor: Int,
            strokeWidth: Float,
            name: String = "Shape Layer",
            order: Int
        ): Layer {
            return Layer(
                name = name,
                type = LayerType.Shape(shapeType, fillColor, strokeColor, strokeWidth),
                order = order
            )
        }

        /**
         * Factory method to create a new Sticker Layer.
         */
        fun createSticker(resId: Int, name: String = "Sticker Layer", order: Int): Layer {
            return Layer(
                name = name,
                type = LayerType.Sticker(resId),
                order = order
            )
        }
    }
}
