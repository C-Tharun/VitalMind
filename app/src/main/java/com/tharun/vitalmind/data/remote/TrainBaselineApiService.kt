package com.tharun.vitalmind.data.remote

/**
 * Data classes for Baseline Training (Autoencoder) API
 */

/**
 * Represents a single day's baseline metrics
 */
data class BaselineDay(
    val avg_heart_rate: Float,
    val resting_heart_rate: Float,
    val hr_variance: Float,
    val total_sleep_minutes: Int,
    val steps_total: Int,
    val calories_burned: Float,
    val sedentary_ratio: Float,
    val movement_variance: Float,
    val activity_load_index: Float,
    val sleep_consistency: Float
)

/**
 * Request for training the baseline model on the server
 * Sends all collected baseline days to the backend for Autoencoder training
 */
data class TrainBaselineRequest(
    val user_id: String,
    val baseline_data: List<BaselineDay>
)

/**
 * Response from baseline training endpoint
 */
data class TrainBaselineResponse(
    val status: String,  // "trained"
    val baseline_updated: Boolean
)

