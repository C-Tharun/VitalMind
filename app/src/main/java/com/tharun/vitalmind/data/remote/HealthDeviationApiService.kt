package com.tharun.vitalmind.data.remote

/**
 * Data classes for Health Deviation (PHBD-Net) API
 */

// Request data class for /health_deviation endpoint
// Updated to match backend's actual requirements
data class HealthDeviationRequest(
    val user_id: String,                    // MANDATORY
    val avg_heart_rate: Float,              // MANDATORY - Backend requires non-null
    val resting_heart_rate: Float,          // MANDATORY - New required field
    val hr_variance: Float,                 // MANDATORY - Heart rate variability
    val steps_total: Int,                   // MANDATORY
    val total_sleep_minutes: Int,           // MANDATORY - Backend requires non-null
    val calories_burned: Float,             // MANDATORY - Backend requires non-null
    val sedentary_ratio: Float,             // MANDATORY - Ratio of sedentary time
    val movement_variance: Float,           // MANDATORY - Movement pattern variance
    val activity_load_index: Float,         // MANDATORY - Activity intensity index
    val sleep_consistency: Float,           // MANDATORY - Sleep pattern consistency
    val hour_of_day: Int,                   // Time context
    val is_sedentary: Int                   // Binary flag
)

// Response data class matching backend contract
data class HealthDeviationResponse(
    val health_deviation_score: Float,
    val stress_drift_level: String,  // Low / Medium / High
    val confidence: Float,            // 0.0 to 1.0
    val top_contributors: List<String>
)


