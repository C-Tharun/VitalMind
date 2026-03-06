package com.tharun.vitalmind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity to store health deviation analysis history
 * Stores the last 30 days of deviation analysis results
 */
@Entity(tableName = "health_deviation_history")
data class HealthDeviationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val timestamp: Long,  // When the analysis was performed
    val date: String,     // Format: yyyy-MM-dd for easy grouping

    // Analysis results
    val deviation_score: Float,
    val drift_level: String,  // Low / Medium / High
    val confidence: Float,    // 0.0 to 1.0

    // Top contributing factors (stored as JSON string)
    val top_contributors: String,  // e.g., "total_sleep_minutes,resting_heart_rate,calories_burned"

    // Input metrics (for reference)
    val steps: Int,
    val sleep_minutes: Int,
    val calories: Float,
    val avg_heart_rate: Float,
    val resting_heart_rate: Float,

    val createdAt: Long = System.currentTimeMillis()
)

