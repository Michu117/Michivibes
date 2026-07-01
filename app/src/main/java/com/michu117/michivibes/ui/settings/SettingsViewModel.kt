package com.michu117.michivibes.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michu117.michivibes.data.database.AppDatabase
import com.michu117.michivibes.utils.DataStoreManager
import com.michu117.michivibes.utils.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class SettingsUiState(
    val theme: String = ThemeMode.SYSTEM,
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val autoExport: Boolean = false,
    val language: String = "es",
    val isExporting: Boolean = false,
    val exportPath: String? = null,
    val resettingStats: Boolean = false,
    val message: String? = null
)

class SettingsViewModel(
    private val dataStoreManager: DataStoreManager,
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            dataStoreManager.theme.collect { theme ->
                _uiState.value = _uiState.value.copy(theme = theme)
            }
        }
        viewModelScope.launch {
            dataStoreManager.selectedYear.collect { year ->
                _uiState.value = _uiState.value.copy(selectedYear = year)
            }
        }
        viewModelScope.launch {
            dataStoreManager.autoExport.collect { auto ->
                _uiState.value = _uiState.value.copy(autoExport = auto)
            }
        }
        viewModelScope.launch {
            dataStoreManager.language.collect { lang ->
                _uiState.value = _uiState.value.copy(language = lang)
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            dataStoreManager.setTheme(theme)
        }
    }

    fun setYear(year: Int) {
        viewModelScope.launch {
            dataStoreManager.setSelectedYear(year)
        }
    }

    fun setAutoExport(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setAutoExport(enabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            dataStoreManager.setLanguage(language)
        }
    }

    fun resetStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(resettingStats = true)
            try {
                database.clearAllTables()
                _uiState.value = _uiState.value.copy(
                    resettingStats = false,
                    message = "Estadísticas reiniciadas correctamente"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    resettingStats = false,
                    message = "Error al reiniciar: ${e.message}"
                )
            }
        }
    }

    fun exportData(context: Context, bitmap: Bitmap?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                if (bitmap != null) {
                    val file = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "MusicWrapped_${System.currentTimeMillis()}.png"
                    )
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportPath = file.absolutePath,
                        message = "Exportado a: ${file.absolutePath}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        message = "No hay contenido para exportar"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    message = "Error al exportar: ${e.message}"
                )
            }
        }
    }

    fun shareWrapped(context: Context, bitmap: Bitmap?) {
        viewModelScope.launch {
            try {
                if (bitmap != null) {
                    val file = File(
                        context.cacheDir,
                        "MusicWrapped_${System.currentTimeMillis()}.png"
                    )
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )

                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Wrapped"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error al compartir: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    class Factory(
        private val dataStoreManager: DataStoreManager,
        private val database: AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(dataStoreManager, database) as T
        }
    }
}
