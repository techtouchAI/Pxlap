package com.pixellayer.studio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pixellayer.studio.data.model.*
import com.pixellayer.studio.domain.engine.RenderEngine
import com.pixellayer.studio.ui.components.HorizontalToolbar
import com.pixellayer.studio.ui.components.LayerBottomSheet
import com.pixellayer.studio.ui.workspace.WorkspaceCanvas
import com.pixellayer.studio.viewmodel.LayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repository = ProjectRepository(filesDir, cacheDir)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PixelLayerApp(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelLayerApp(repository: ProjectRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel: LayerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LayerViewModel(repository) as T
            }
        }
    )

    val selectedLayerId by viewModel.selectedLayerId.collectAsState()
    val layers by viewModel.layers.collectAsState()

    var showLayerSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val renderEngine = remember { RenderEngine() }

    Scaffold(
        topBar = {
            HorizontalToolbar(
                onCategorySelected = { category ->
                    when (category) {
                        "Project" -> {
                            coroutineScope.launch {
                                repository.saveProject("MyProject", layers)
                                Toast.makeText(context, "Project Auto-Saved!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        "Text" -> {
                            viewModel.addLayer(Layer.createText("New Text", layers.size))
                        }
                        "Objects" -> {
                            viewModel.addLayer(Layer.createShape(ShapeType.Rect, 0xFFE91E63.toInt(), 0xFF000000.toInt(), 5f, "New Shape", layers.size))
                        }
                        "Layers" -> {
                            showLayerSheet = true
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedLayerId != null) {
                val selectedLayer = layers.find { it.id == selectedLayerId }
                if (selectedLayer != null) {
                    PropertyPanel(
                        layer = selectedLayer,
                        onUpdate = { updateBlock ->
                            viewModel.updateLayer(selectedLayer.id, updateBlock)
                        }
                    )
                }
            } else {
                ExportPanel(
                    isExporting = isExporting,
                    onExport = { quality ->
                        coroutineScope.launch {
                            isExporting = true
                            val bitmap = renderEngine.renderToBitmap(layers, quality)
                            isExporting = false
                            if (bitmap != null) {
                                Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Export failed due to OOM", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            WorkspaceCanvas(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showLayerSheet) {
        LayerBottomSheet(
            viewModel = viewModel,
            sheetState = sheetState,
            onDismissRequest = { showLayerSheet = false }
        )
    }
}

@Composable
fun ExportPanel(isExporting: Boolean, onExport: (ExportQuality) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Export Options", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onExport(ExportQuality.Draft) }, enabled = !isExporting) { Text("Draft") }
                Button(onClick = { onExport(ExportQuality.HD) }, enabled = !isExporting) { Text("HD") }
                Button(onClick = { onExport(ExportQuality._4K) }, enabled = !isExporting) { Text("4K") }
            }
            if (isExporting) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Exporting High-Res Bitmap...", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PropertyPanel(layer: Layer, onUpdate: ((Layer) -> Layer) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            Text("Properties: ${layer.name}", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            // Shared Dynamic Shadow Properties
            Text("Dynamic Shadow", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = layer.shadow.isEnabled,
                    onCheckedChange = { checked ->
                        onUpdate { it.copy(shadow = it.shadow.copy(isEnabled = checked)) }
                    }
                )
            }
            if (layer.shadow.isEnabled) {
                Text("Light Angle: ${layer.shadow.lightAngleDegrees.toInt()}°", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = layer.shadow.lightAngleDegrees,
                    onValueChange = { angle ->
                        onUpdate { it.copy(shadow = it.shadow.copy(lightAngleDegrees = angle)) }
                    },
                    valueRange = 0f..360f
                )
                Text("Distance: ${layer.shadow.distance.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = layer.shadow.distance,
                    onValueChange = { dist ->
                        onUpdate { it.copy(shadow = it.shadow.copy(distance = dist)) }
                    },
                    valueRange = 0f..100f
                )
                Text("Shadow Radius: ${layer.shadow.radius.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = layer.shadow.radius,
                    onValueChange = { radius ->
                        onUpdate { it.copy(shadow = it.shadow.copy(radius = radius)) }
                    },
                    valueRange = 1f..50f
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Layer-specific Properties
            when (val type = layer.type) {
                is LayerType.Text -> {
                    Text("Text Attributes", style = MaterialTheme.typography.titleSmall)
                    TextField(
                        value = type.properties.text,
                        onValueChange = { newText ->
                            onUpdate {
                                it.copy(type = LayerType.Text(type.properties.copy(text = newText)))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Size (Scale): ${(layer.transform.scaleX * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = layer.transform.scaleX,
                        onValueChange = { scale ->
                            onUpdate {
                                it.copy(transform = it.transform.copy(scaleX = scale, scaleY = scale))
                            }
                        },
                        valueRange = 0.1f..5f
                    )

                    Text("Stroke Width: ${type.properties.strokeWidth.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = type.properties.strokeWidth,
                        onValueChange = { width ->
                            onUpdate {
                                it.copy(type = LayerType.Text(type.properties.copy(strokeWidth = width)))
                            }
                        },
                        valueRange = 0f..20f
                    )

                    // Color Picker (Mockup using buttons for simplicity)
                    Text("Text Color", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Text(type.properties.copy(color = 0xFFFFFFFF.toInt()))) } }) { Text("White") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Text(type.properties.copy(color = 0xFF000000.toInt()))) } }) { Text("Black") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Text(type.properties.copy(color = 0xFFFF0000.toInt()))) } }) { Text("Red") }
                    }
                }
                is LayerType.Shape -> {
                    Text("Shape Properties", style = MaterialTheme.typography.titleSmall)
                    Text("Stroke Width: ${type.strokeWidth.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = type.strokeWidth,
                        onValueChange = { width ->
                            onUpdate {
                                it.copy(type = LayerType.Shape(type.shapeType, type.fillColor, type.strokeColor, width))
                            }
                        },
                        valueRange = 0f..50f
                    )

                    Text("Fill Color", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, 0xFF4CAF50.toInt(), type.strokeColor, type.strokeWidth)) } }) { Text("Green") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, 0xFF2196F3.toInt(), type.strokeColor, type.strokeWidth)) } }) { Text("Blue") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, 0xFFFFC107.toInt(), type.strokeColor, type.strokeWidth)) } }) { Text("Yellow") }
                    }

                    Text("Stroke Color", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, type.fillColor, 0xFF000000.toInt(), type.strokeWidth)) } }) { Text("Black") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, type.fillColor, 0xFFFFFFFF.toInt(), type.strokeWidth)) } }) { Text("White") }
                        Button(onClick = { onUpdate { it.copy(type = LayerType.Shape(type.shapeType, type.fillColor, 0xFFFF0000.toInt(), type.strokeWidth)) } }) { Text("Red") }
                    }
                }
                else -> {
                    Text("No specific properties available for this layer type.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
