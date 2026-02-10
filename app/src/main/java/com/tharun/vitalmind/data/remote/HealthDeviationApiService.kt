package com.tharun.vitalmind.data.remote

/**
 * Data classes for Health Deviation (PHBD-Net) API
 */

// Request data class for /health_deviation endpoint
data class HealthDeviationRequest(
    val avg_heart_rate: Float,
    val max_heart_rate: Float,
    val steps_total: Int,
    val calories_burned: Float,
    val distance_total: Float,
    val total_sleep_minutes: Int,
    val activity: String,
    val hour_of_day: Int,
    val is_sedentary: Int
)

// Response data class matching backend contract
data class HealthDeviationResponse(
    val health_deviation_score: Float,
    val stress_drift_level: String,  // Low / Medium / High
    val confidence: Float,            // 0.0 to 1.0
    val top_contributors: List<String>
)

