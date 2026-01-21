package com.tharun.vitalmind.ui.stress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.StressScoreHistory
import com.tharun.vitalmind.data.repository.StressRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class StressHistoryViewModel(
    repository: StressRepository,
    userId: String
) : ViewModel() {
    val history: StateFlow<List<StressScoreHistory>> =
        repository.getStressScoreHistory(userId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

