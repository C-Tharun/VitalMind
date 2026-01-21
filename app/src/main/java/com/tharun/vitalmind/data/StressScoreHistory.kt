package com.tharun.vitalmind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stress_score_history")
data class StressScoreHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val timestamp: Long,
    val stress_score: Float,
    val stress_level: String,
    val stress_status: String,
    val stress_stability: String,
    val mood: String,
    val request_json: String // Store the full request for traceability
)

