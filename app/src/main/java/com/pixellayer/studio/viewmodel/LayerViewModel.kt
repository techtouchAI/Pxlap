package com.pixellayer.studio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixellayer.studio.data.model.Layer
import com.pixellayer.studio.data.model.ProjectRepository
import com.pixellayer.studio.data.model.TransformState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the state of the workspace and layers.
 */
class LayerViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _layers = MutableStateFlow<List<Layer>>(emptyList())
    val layers: StateFlow<List<Layer>> = _layers.asStateFlow()

    private val _selectedLayerId = MutableStateFlow<String?>(null)
    val selectedLayerId: StateFlow<String?> = _selectedLayerId.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        // Load initial state if any
        viewModelScope.launch {
            val savedLayers = repository.loadAutoSave()
            if (savedLayers.isNotEmpty()) {
                _layers.value = savedLayers
            }
        }
    }

    /**
     * Adds a new layer to the workspace.
     */
    fun addLayer(layer: Layer) {
        _layers.update { currentList ->
            val updatedList = currentList + layer
            triggerAutoSave(updatedList)
            updatedList
        }
        selectLayer(layer.id)
    }

    /**
     * Updates an existing layer.
     */
    fun updateLayer(layerId: String, updateBlock: (Layer) -> Layer) {
        _layers.update { currentList ->
            val updatedList = currentList.map {
                if (it.id == layerId) updateBlock(it) else it
            }
            triggerAutoSave(updatedList)
            updatedList
        }
    }

    /**
     * Removes a layer from the workspace.
     */
    fun removeLayer(layerId: String) {
        _layers.update { currentList ->
            val updatedList = currentList.filterNot { it.id == layerId }
            triggerAutoSave(updatedList)
            updatedList
        }
        if (_selectedLayerId.value == layerId) {
            selectLayer(null)
        }
    }

    /**
     * Selects a layer for editing.
     */
    fun selectLayer(layerId: String?) {
        _selectedLayerId.value = layerId
    }

    /**
     * Updates the transform state of the currently selected layer.
     */
    fun updateSelectedLayerTransform(transformBlock: (TransformState) -> TransformState) {
        val selectedId = _selectedLayerId.value ?: return
        updateLayer(selectedId) { layer ->
            layer.copy(transform = transformBlock(layer.transform))
        }
    }

    /**
     * Toggles the visibility of a layer.
     */
    fun toggleLayerVisibility(layerId: String) {
        updateLayer(layerId) { layer ->
            layer.copy(isVisible = !layer.isVisible)
        }
    }

    /**
     * Toggles the lock state of a layer.
     */
    fun toggleLayerLock(layerId: String) {
        updateLayer(layerId) { layer ->
            layer.copy(isLocked = !layer.isLocked)
        }
    }

    /**
     * Reorders layers (simple move to front/back could be implemented, or full reorder).
     * This is a simplified move to front for demonstration.
     */
    fun bringLayerToFront(layerId: String) {
        _layers.update { currentList ->
            val layer = currentList.find { it.id == layerId } ?: return@update currentList
            val updatedList = currentList.filterNot { it.id == layerId } + layer.copy(order = currentList.maxOfOrNull { it.order }?.plus(1) ?: 0)
            triggerAutoSave(updatedList)
            updatedList
        }
    }

    /**
     * Triggers debounced auto-save.
     */
    private fun triggerAutoSave(layersToSave: List<Layer>) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // Debounce for 500ms
            repository.autoSave(layersToSave)
        }
    }
}
