package com.tharun.vitalsync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_data")
data class HealthData(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String, // To store data per Google account
    val timestamp: Long,
    val heartRate: Float? = null,
    val steps: Int? = null,
    val calories: Float? = null,
    val heartPoints: Int? = null,
    val distance: Float? = null, // in kilometers
    val activityType: String? = null,
    val sleepDuration: Long? = null // in minutes
)
