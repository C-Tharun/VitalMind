package com.tharun.vitalmind.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track sent notifications and prevent duplicates
 */
@Entity(tableName = "notification_history")
data class NotificationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: String,
    val notificationType: String, // e.g., "HEART_RATE_ALERT", "SLEEP_RECOVERY"
    val timestamp: Long,
    val severity: String, // "LOW", "MEDIUM", "HIGH"
    val dismissed: Boolean = false
)

