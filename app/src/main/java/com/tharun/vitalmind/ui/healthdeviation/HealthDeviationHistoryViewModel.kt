package com.tharun.vitalmind.ui.healthdeviation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.HealthDeviationHistory
import com.tharun.vitalmind.data.repository.HealthDeviationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Health Deviation History Screen
 */
class HealthDeviationHistoryViewModel(
    private val repository: HealthDeviationRepository
) : ViewModel() {

    private val _history = MutableStateFlow<List<HealthDeviationHistory>>(emptyList())
    val history: StateFlow<List<HealthDeviationHistory>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.getDeviationHistory().collect { historyList ->
                _history.value = historyList
            }
        }
    }

    /**
     * Refresh history data
     */
    fun refresh() {
        loadHistory()
    }
}

