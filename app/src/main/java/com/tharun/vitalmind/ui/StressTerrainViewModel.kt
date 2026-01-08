package com.tharun.vitalmind.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.AppDatabase
import com.tharun.vitalmind.data.StressCluster
import com.tharun.vitalmind.data.StressTerrainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StressTerrainState(
    val isLoading: Boolean = true,
    val stressClusters: List<StressCluster> = emptyList(),
    val calmingClusters: List<StressCluster> = emptyList(),
    val showStressZones: Boolean = true,
    val errorMessage: String? = null
)

/**
 * ViewModel for the Stress Terrain Map feature.
 * Manages stress cluster data and UI state for the map visualization.
 */
class StressTerrainViewModel(application: Application) : AndroidViewModel(application) {

    private val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
    private val stressTerrainRepository = StressTerrainRepository(healthDataDao)

    private val _state = MutableStateFlow(StressTerrainState())
    val state: StateFlow<StressTerrainState> = _state.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)

    init {
        // Observe state changes to load data when userId is set
        viewModelScope.launch {
            _userId.collect { userId ->
                if (userId != null) {
                    loadStressTerrainData(userId)
                }
            }
        }
    }

    /**
     * Sets the current user ID and triggers data loading
     */
    fun setUserId(userId: String) {
        _userId.value = userId
    }

    /**
     * Loads stress terrain data for the current user
     */
    private fun loadStressTerrainData(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            try {
                stressTerrainRepository.getStressTerrainData(userId, dayCount = 30).collect { (stressClusters, calmingClusters) ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        stressClusters = stressClusters,
                        calmingClusters = calmingClusters
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load stress terrain data: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggles between stress zones and calming zones
     */
    fun toggleZoneType() {
        _state.value = _state.value.copy(showStressZones = !_state.value.showStressZones)
    }

    /**
     * Refreshes the stress terrain data
     */
    fun refreshData() {
        _userId.value?.let { loadStressTerrainData(it) }
    }
}

