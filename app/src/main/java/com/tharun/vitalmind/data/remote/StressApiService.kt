package com.tharun.vitalmind.data.remote

// Interface for Stress Score API (stubbed for now)
interface StressApiService {
    /**
     * Sends stress analysis request to backend (stubbed for Phase 1).
     * TODO: Implement real network call when backend is available.
     */
    suspend fun calculateStress(request: StressRequest): StressResponse
}

// Data class for request (matches backend contract)
data class StressRequest(
    val avg_heart_rate: Float,
    val max_heart_rate: Float,
    val steps_total: Int,
    val calories_total: Float,
    val distance_total: Float,
    val sleep_minutes: Int,
    val activity: String,
    val hour_of_day: Int,
    val is_sedentary: Int,
    val recent_stress_scores: List<Float>? = null
)

// Data class for response (matches backend contract)
data class StressResponse(
    val stress_level: String,
    val stress_score: Float,
    val stress_status: String,
    val stress_stability: String,
    val mood: String
)

