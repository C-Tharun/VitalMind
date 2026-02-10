package com.tharun.vitalmind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity to store daily health baseline data for personalized deviation analysis
 * Each record represents one day's aggregated health metrics for a user
 */
@Entity(tableName = "health_deviation_baseline")
data class HealthDeviationBaseline(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val date: String,  // Format: yyyy-MM-dd (unique per user)
    val timestamp: Long,  // Start of day timestamp

    // Core metrics (aggregated for the day)
    val avg_heart_rate: Float,
    val resting_heart_rate: Float,
    val hr_variance: Float,
    val steps_total: Int,
    val total_sleep_minutes: Int,
    val calories_burned: Float,

    // Derived metrics
    val sedentary_ratio: Float,
    val movement_variance: Float,
    val activity_load_index: Float,
    val sleep_consistency: Float,

    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)

