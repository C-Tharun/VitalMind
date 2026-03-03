package com.tharun.vitalmind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for notification history operations
 */
@Dao
interface NotificationHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationHistory)

    /**
     * Check if a notification of this type was sent within the specified time window
     */
    @Query("""
        SELECT COUNT(*) FROM notification_history 
        WHERE userId = :userId 
        AND notificationType = :type 
        AND timestamp > :sinceTimestamp
    """)
    suspend fun hasRecentNotification(userId: String, type: String, sinceTimestamp: Long): Int

    /**
     * Get the most recent notification of a specific type
     */
    @Query("""
        SELECT * FROM notification_history 
        WHERE userId = :userId 
        AND notificationType = :type 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestNotification(userId: String, type: String): NotificationHistory?

    /**
     * Clean up old notifications (older than 30 days)
     */
    @Query("""
        DELETE FROM notification_history 
        WHERE timestamp < :cutoffTimestamp
    """)
    suspend fun cleanupOldNotifications(cutoffTimestamp: Long)
}

