package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.repository.HealthDeviationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Health Deviation (PHBD-Net) feature with personalized baseline support
 */
class HealthDeviationViewModel(
    private val repository: HealthDeviationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthDeviationUiStateExtended>(HealthDeviationUiStateExtended.Idle)
    val uiState: StateFlow<HealthDeviationUiStateExtended> = _uiState

    init {
        /**
         * Baseline Collection Strategy:
         *
         * - Check baseline status immediately to determine UI state
         * - Attempt to collect today's baseline data
         *
         * Why baseline collection runs in init():
         * - Ensures baseline is updated each time user opens Home screen
         * - Repository has safety checks to prevent premature collection
         * - If data is insufficient (e.g., Google Fit not synced yet), collection is skipped
         * - Will retry on next app open or sync
         *
         * Race condition safety:
         * - collectTodaysBaseline() checks for sufficient data before saving
         * - Requires minimum: 3 HR samples OR steps/calories present
         * - Silent failure if conditions not met (no UI impact)
         *
         * Why resting HR is derived:
         * - Google Fit API doesn't provide explicit "resting heart rate"
         * - We derive it from HR samples during sleep or low-activity periods
         * - Uses lowest 15th percentile of rest-period HR readings
         * - This is physiologically accurate and defensible
         */
        checkBaselineStatus()
        collectTodaysBaseline()
    }

    /**
     * Check if baseline is ready or still collecting
     */
    fun checkBaselineStatus() {
        Log.d("HealthDeviationVM", "🔍 Checking baseline status")
        viewModelScope.launch {
            try {
                val (status, daysCollected) = repository.getBaselineStatus()
                Log.d("HealthDeviationVM", "📊 Baseline status: $status, Days: $daysCollected/${HealthDeviationRepository.MINIMUM_BASELINE_DAYS}")

                when (status) {
                    BaselineStatus.COLLECTING -> {
                        _uiState.value = HealthDeviationUiStateExtended.CollectingBaseline(
                            daysCollected = daysCollected,
                            daysNeeded = HealthDeviationRepository.MINIMUM_BASELINE_DAYS
                        )
                    }
                    BaselineStatus.READY -> {
                        _uiState.value = HealthDeviationUiStateExtended.Ready
                    }
                }
            } catch (e: Exception) {
                Log.e("HealthDeviationVM", "❌ Error checking baseline status: ${e.message}", e)
                _uiState.value = HealthDeviationUiStateExtended.Error("Failed to check baseline status")
            }
        }
    }

    /**
     * Collect today's baseline data (called automatically and can be manually triggered)
     */
    fun collectTodaysBaseline() {
        viewModelScope.launch {
            repository.collectTodaysBaseline()
                .onSuccess {
                    Log.d("HealthDeviationVM", "✅ Today's baseline collected")
                    // Refresh status after collection
                    checkBaselineStatus()
                }
                .onFailure { error ->
                    Log.w("HealthDeviationVM", "⚠️ Could not collect today's baseline: ${error.message}")
                }
        }
    }

    /**
     * Analyze health deviation - called from UI (only when baseline is READY)
     */
    fun analyzeHealthDeviation() {
        Log.d("HealthDeviationVM", "🔄 analyzeHealthDeviation() called")

        // Check if baseline is ready
        if (_uiState.value !is HealthDeviationUiStateExtended.Ready) {
            Log.w("HealthDeviationVM", "⚠️ Cannot analyze - baseline not ready")
            return
        }

        _uiState.value = HealthDeviationUiStateExtended.Loading
        Log.d("HealthDeviationVM", "State changed to Loading")

        viewModelScope.launch {
            Log.d("HealthDeviationVM", "Launching coroutine to call repository")
            repository.getHealthDeviation()
                .onSuccess { response ->
                    Log.d("HealthDeviationVM", "✅ Success! Response: $response")
                    _uiState.value = HealthDeviationUiStateExtended.Success(response)
                }
                .onFailure { error ->
                    Log.e("HealthDeviationVM", "❌ Error occurred: ${error.message}", error)
                    _uiState.value = HealthDeviationUiStateExtended.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }
}


