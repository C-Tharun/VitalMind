package com.tharun.vitalmind.data.repository

import com.tharun.vitalmind.data.remote.StressApiService
import com.tharun.vitalmind.data.remote.StressRequest
import com.tharun.vitalmind.data.remote.StressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StressRepository(private val apiService: StressApiService) {
    // TODO: Replace stub with real data sources and API call when backend is ready
    suspend fun calculateStressScore(): StressResponse = withContext(Dispatchers.IO) {
        // TODO: Pull real data from repositories (heart rate, steps, etc.)
        val request = StressRequest(
            avg_heart_rate = 72.0f,
            max_heart_rate = 120.0f,
            steps_total = 5000,
            calories_total = 2200.0f,
            distance_total = 3.5f,
            sleep_minutes = 420,
            activity = "walking",
            hour_of_day = 14,
            is_sedentary = 0,
            recent_stress_scores = listOf(0.5f, 0.6f, 0.7f)
        )
        // Stubbed response for Phase 1
        // TODO: Call apiService.calculateStress(request) when backend is ready
        StressResponse(
            stress_level = "Moderate",
            stress_score = 0.65f,
            stress_status = "Stable",
            stress_stability = "Consistent",
            mood = "Neutral"
        )
    }
}

