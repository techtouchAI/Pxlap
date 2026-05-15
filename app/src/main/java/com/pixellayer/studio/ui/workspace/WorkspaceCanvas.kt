package com.pixellayer.studio.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.pixellayer.studio.data.model.Layer
import com.pixellayer.studio.domain.engine.SmartSnapEngine
import com.pixellayer.studio.viewmodel.LayerViewModel

/**
 * The central workspace canvas where layers are rendered and interacted with.
 */
@Composable
fun WorkspaceCanvas(
    viewModel: LayerViewModel,
    modifier: Modifier = Modifier
) {
    val layers by viewModel.layers.collectAsState()
    val selectedLayerId by viewModel.selectedLayerId.collectAsState()

    val snapEngine = remember { SmartSnapEngine() }

    // State to hold temporary snap guides for rendering
    var showVerticalGuide by remember { mutableStateOf(false) }
    var showHorizontalGuide by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.DarkGray)) {

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            // Simplified hit testing: just deselect on tap for now
                            // In a real app, you'd calculate if the offset intersects with any layer's bounds
                            viewModel.selectLayer(null)
                        }
                    )
                }
                .pointerInput(selectedLayerId) {
                    if (selectedLayerId != null) {
                        detectTransformGestures { centroid, pan, zoom, rotation ->
                            val selectedLayer = layers.find { it.id == selectedLayerId }

                            if (selectedLayer != null && !selectedLayer.isLocked) {
                                viewModel.updateSelectedLayerTransform { current ->
                                    val newTransform = current
                                        .scaleBy(zoom)
                                        .rotateBy(rotation)
                                        .translateBy(pan.x, pan.y)

                                    // Apply snapping
                                    val snapResult = snapEngine.calculateSnap(
                                        newTransform,
                                        layerWidth = 200f, // Dummy width
                                        layerHeight = 200f, // Dummy height
                                        canvasWidth = size.width.toFloat(),
                                        canvasHeight = size.height.toFloat()
                                    )

                                    showVerticalGuide = snapResult.isSnappedHorizontal
                                    showHorizontalGuide = snapResult.isSnappedVertical

                                    newTransform.copy(
                                        translateX = snapResult.snappedX ?: newTransform.translateX,
                                        translateY = snapResult.snappedY ?: newTransform.translateY
                                    )
                                }
                            }
                        }
                    } else {
                        // Reset guides if nothing is selected or being transformed
                        showVerticalGuide = false
                        showHorizontalGuide = false
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Render layers
            layers.filter { it.isVisible }.sortedBy { it.order }.forEach { layer ->
                withTransform({
                    translate(
                        left = canvasWidth / 2f + layer.transform.translateX,
                        top = canvasHeight / 2f + layer.transform.translateY
                    )
                    rotate(layer.transform.rotation)
                    scale(layer.transform.scaleX, layer.transform.scaleY)
                }) {
                    drawLayerRepresentation(layer, isSelected = layer.id == selectedLayerId)
                }
            }

            // Render Magnetic Guides
            if (showVerticalGuide) {
                drawLine(
                    color = Color.Cyan,
                    start = Offset(canvasWidth / 2f, 0f),
                    end = Offset(canvasWidth / 2f, canvasHeight),
                    strokeWidth = 2f
                )
            }
            if (showHorizontalGuide) {
                drawLine(
                    color = Color.Cyan,
                    start = Offset(0f, canvasHeight / 2f),
                    end = Offset(canvasWidth, canvasHeight / 2f),
                    strokeWidth = 2f
                )
            }
        }
    }
}

/**
 * Helper to draw a representation of a layer on the Compose Canvas.
 */
private fun DrawScope.drawLayerRepresentation(layer: Layer, isSelected: Boolean) {
    // This is a simplified representation for the Compose UI preview
    // Actual rendering to Bitmap is done by RenderEngine

    val rectSize = 200f

    when (layer.type) {
        is com.pixellayer.studio.data.model.LayerType.Text -> {
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(-rectSize / 2, -rectSize / 4),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize / 2)
            )
        }
        is com.pixellayer.studio.data.model.LayerType.Image -> {
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(-rectSize / 2, -rectSize / 2),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize)
            )
        }
        is com.pixellayer.studio.data.model.LayerType.Shape -> {
            val shapeType = layer.type as com.pixellayer.studio.data.model.LayerType.Shape
            drawRect(
                color = Color(shapeType.fillColor),
                topLeft = Offset(-rectSize / 2, -rectSize / 2),
                size = androidx.compose.ui.geometry.Size(rectSize, rectSize)
            )
        }
        is com.pixellayer.studio.data.model.LayerType.Sticker -> {
            drawCircle(
                color = Color.Yellow,
                radius = rectSize / 2,
                center = Offset.Zero
            )
        }
    }

    // Draw selection border
    if (isSelected) {
        drawRect(
            color = Color.Blue,
            topLeft = Offset(-rectSize / 2 - 5f, -rectSize / 2 - 5f),
            size = androidx.compose.ui.geometry.Size(rectSize + 10f, rectSize + 10f),
            style = Stroke(width = 4f)
        )
    }
}
