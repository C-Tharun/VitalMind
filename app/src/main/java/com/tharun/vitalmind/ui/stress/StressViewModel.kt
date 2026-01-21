package com.tharun.vitalmind.ui.stress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.repository.StressRepository
import com.tharun.vitalmind.data.remote.StressResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class StressUiState {
    object Idle : StressUiState()
    object Loading : StressUiState()
    data class Success(val response: StressResponse) : StressUiState()
    data class Error(val message: String) : StressUiState()
}

class StressViewModel(private val repository: StressRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<StressUiState>(StressUiState.Idle)
    val uiState: StateFlow<StressUiState> = _uiState

    fun calculateStress() {
        _uiState.value = StressUiState.Loading
        viewModelScope.launch {
            try {
                val response = repository.calculateStressScore()
                _uiState.value = StressUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = StressUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

