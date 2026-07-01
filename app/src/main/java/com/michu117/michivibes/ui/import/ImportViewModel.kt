package com.michu117.michivibes.ui.import

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michu117.michivibes.MusicWrappedApp
import com.michu117.michivibes.utils.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ImportUiState(
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null,
    val previewData: List<String> = emptyList(),
    val error: String? = null
)

class ImportViewModel : ViewModel() {
    private val app = MusicWrappedApp.instance
    private val songRepository = app.songRepository
    private val scrobbleRepository = app.scrobbleRepository

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState

    fun loadPreview(context: Context, uri: Uri, isJson: Boolean) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = inputStream?.bufferedReader()
                val lines = reader?.readLines() ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    previewData = if (isJson) {
                        listOf("Archivo JSON detectado") + lines.take(5)
                    } else {
                        lines.take(20)
                    },
                    error = null
                )
                reader?.close()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error al leer archivo: ${e.message}")
            }
        }
    }

    fun importFile(context: Context, uri: Uri, isJson: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            try {
                val result = if (isJson) {
                    app.jsonImporter.import(context, uri)
                } else {
                    app.csvImporter.import(context, uri)
                }

                if (result.songs.isNotEmpty() || result.scrobbles.isNotEmpty()) {
                    songRepository.insertAll(result.songs)
                    scrobbleRepository.insertAll(result.scrobbles)
                }

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "Error al importar: ${e.message}"
                )
            }
        }
    }

    fun resetImport() {
        _uiState.value = ImportUiState()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportViewModel() as T
        }
    }
}
