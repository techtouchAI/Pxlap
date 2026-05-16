package com.pixellayer.studio.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository to handle saving and loading projects to/from local storage.
 */
class ProjectRepository(private val filesDir: File, private val cacheDir: File) {

    private val autoSaveFile = File(cacheDir, "autosave.json")

    /**
     * Auto-saves the current state to the cache directory.
     * Uses IO dispatcher as it performs file operations.
     */
    suspend fun autoSave(layers: List<Layer>) {
        withContext(Dispatchers.IO) {
            try {
                val json = ProjectSerializer.serialize(layers)
                autoSaveFile.writeText(json)
            } catch (e: Exception) {
                e.printStackTrace() // In a real app, log error or report to analytics
            }
        }
    }

    /**
     * Loads the auto-saved state from the cache directory.
     * Uses IO dispatcher.
     */
    suspend fun loadAutoSave(): List<Layer> {
        return withContext(Dispatchers.IO) {
            try {
                if (autoSaveFile.exists()) {
                    val json = autoSaveFile.readText()
                    ProjectSerializer.deserialize(json)
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Saves a named project to the files directory.
     */
    suspend fun saveProject(projectName: String, layers: List<Layer>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filesDir, "$projectName.json")
                val json = ProjectSerializer.serialize(layers)
                file.writeText(json)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Loads a named project from the files directory.
     */
    suspend fun loadProject(projectName: String): List<Layer>? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filesDir, "$projectName.json")
                if (file.exists()) {
                    val json = file.readText()
                    ProjectSerializer.deserialize(json)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
