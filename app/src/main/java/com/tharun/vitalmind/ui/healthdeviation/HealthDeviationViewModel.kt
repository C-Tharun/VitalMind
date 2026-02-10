package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.remote.HealthDeviationResponse
import com.tharun.vitalmind.data.repository.HealthDeviationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI States for Health Deviation feature
 */
sealed class HealthDeviationUiState {
    object Idle : HealthDeviationUiState()
    object Loading : HealthDeviationUiState()
    data class Success(val response: HealthDeviationResponse) : HealthDeviationUiState()
    data class Error(val message: String) : HealthDeviationUiState()
}

/**
 * ViewModel for Health Deviation (PHBD-Net) feature
 */
class HealthDeviationViewModel(
    private val repository: HealthDeviationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthDeviationUiState>(HealthDeviationUiState.Idle)
    val uiState: StateFlow<HealthDeviationUiState> = _uiState

    /**
     * Analyze health deviation - called from UI
     */
    fun analyzeHealthDeviation() {
        Log.d("HealthDeviationVM", "🔄 analyzeHealthDeviation() called")
        _uiState.value = HealthDeviationUiState.Loading
        Log.d("HealthDeviationVM", "State changed to Loading")

        viewModelScope.launch {
            Log.d("HealthDeviationVM", "Launching coroutine to call repository")
            repository.getHealthDeviation()
                .onSuccess { response ->
                    Log.d("HealthDeviationVM", "✅ Success! Response: $response")
                    _uiState.value = HealthDeviationUiState.Success(response)
                }
                .onFailure { error ->
                    Log.e("HealthDeviationVM", "❌ Error occurred: ${error.message}", error)
                    _uiState.value = HealthDeviationUiState.Error(
                        error.message ?: "Unknown error occurred"
                    )
                }
        }
    }
}


