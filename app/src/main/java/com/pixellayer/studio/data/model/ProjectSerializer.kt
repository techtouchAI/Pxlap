package com.pixellayer.studio.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Handles JSON serialization of the Layer list.
 * Includes configuration for polymorphic types (LayerType).
 */
object ProjectSerializer {

    // Configure JSON to handle the sealed class `LayerType`
    val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            polymorphic(LayerType::class) {
                subclass(LayerType.Text::class)
                subclass(LayerType.Image::class)
                subclass(LayerType.Shape::class)
                subclass(LayerType.Sticker::class)
            }
        }
    }

    /**
     * Serializes a list of layers to a JSON string.
     */
    fun serialize(layers: List<Layer>): String {
        return try {
            jsonFormat.encodeToString(layers)
        } catch (e: Exception) {
            e.printStackTrace()
            "[]"
        }
    }

    /**
     * Deserializes a JSON string back into a list of layers.
     */
    fun deserialize(jsonString: String): List<Layer> {
        return try {
            jsonFormat.decodeFromString<List<Layer>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
