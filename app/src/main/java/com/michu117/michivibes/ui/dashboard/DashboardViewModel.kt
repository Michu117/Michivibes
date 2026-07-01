package com.michu117.michivibes.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.michu117.michivibes.MusicWrappedApp
import com.michu117.michivibes.domain.model.MusicStats
import com.michu117.michivibes.domain.usecases.GetStatisticsUseCase
import com.michu117.michivibes.service.NowPlayingInfo
import com.michu117.michivibes.utils.DataStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: MusicStats? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val nowPlaying: NowPlayingInfo = NowPlayingInfo(),
    val hasImportedData: Boolean = false,
    val notificationAccessGranted: Boolean = true
)

class DashboardViewModel(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreManager.selectedYear.collect { year ->
                _uiState.value = _uiState.value.copy(selectedYear = year)
                loadStats(year)
            }
        }

        viewModelScope.launch {
            MusicWrappedApp.instance.nowPlayingFlow.collect { nowPlaying ->
                _uiState.value = _uiState.value.copy(nowPlaying = nowPlaying)
            }
        }

        val app = MusicWrappedApp.instance
        val granted = com.michu117.michivibes.service.MediaSessionMonitorService.isNotificationAccessGranted(app)
        _uiState.value = _uiState.value.copy(notificationAccessGranted = granted)
    }

    fun loadStats(year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val stats = getStatisticsUseCase.getGeneralStats(year)
                val app = MusicWrappedApp.instance
                val sessionCount = app.listeningSessionRepository.getSessionCount()
                val legacyCount = app.scrobbleRepository.getScrobbleCount()

                _uiState.value = _uiState.value.copy(
                    stats = stats,
                    isLoading = false,
                    hasImportedData = sessionCount > 0 || legacyCount > 0
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setYear(year: Int) {
        viewModelScope.launch {
            dataStoreManager.setSelectedYear(year)
        }
    }

    class Factory(
        private val getStatisticsUseCase: GetStatisticsUseCase,
        private val dataStoreManager: DataStoreManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(getStatisticsUseCase, dataStoreManager) as T
        }
    }
}
