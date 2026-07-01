package com.michu117.michivibes.ui.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michu117.michivibes.domain.model.WrappedData
import com.michu117.michivibes.domain.usecases.GetStatisticsUseCase
import com.michu117.michivibes.utils.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WrappedUiState(
    val wrappedData: WrappedData? = null,
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
)

class WrappedViewModel(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrappedUiState())
    val uiState: StateFlow<WrappedUiState> = _uiState

    fun loadWrapped(year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedYear = year)
            try {
                val wrappedData = getStatisticsUseCase.getWrappedData(year)
                _uiState.value = _uiState.value.copy(
                    wrappedData = wrappedData,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    fun nextPage() {
        val current = _uiState.value.currentPage
        if (current < 7) {
            _uiState.value = _uiState.value.copy(currentPage = current + 1)
        }
    }

    fun previousPage() {
        val current = _uiState.value.currentPage
        if (current > 0) {
            _uiState.value = _uiState.value.copy(currentPage = current - 1)
        }
    }

    class Factory(
        private val getStatisticsUseCase: GetStatisticsUseCase,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WrappedViewModel(getStatisticsUseCase, dataStoreManager) as T
        }
    }
}
