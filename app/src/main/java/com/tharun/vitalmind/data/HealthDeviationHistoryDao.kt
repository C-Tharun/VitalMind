package com.tharun.vitalmind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Health Deviation History operations
 */
@Dao
interface HealthDeviationHistoryDao {

    /**
     * Insert a new deviation analysis record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviationHistory(history: HealthDeviationHistory)

    /**
     * Get all deviation history for a user (last 30 days)
     */
    @Query("""
        SELECT * FROM health_deviation_history 
        WHERE userId = :userId 
        AND timestamp >= :thirtyDaysAgo
        ORDER BY timestamp DESC
    """)
    fun getDeviationHistory(userId: String, thirtyDaysAgo: Long): Flow<List<HealthDeviationHistory>>

    /**
     * Get deviation history for a specific date
     */
    @Query("""
        SELECT * FROM health_deviation_history 
        WHERE userId = :userId 
        AND date = :date
        ORDER BY timestamp DESC
    """)
    suspend fun getDeviationForDate(userId: String, date: String): List<HealthDeviationHistory>

    /**
     * Get the most recent deviation analysis
     */
    @Query("""
        SELECT * FROM health_deviation_history 
        WHERE userId = :userId 
        ORDER BY timestamp DESC 
        LIMIT 1
    """)
    suspend fun getLatestDeviation(userId: String): HealthDeviationHistory?

    /**
     * Delete old records (older than 30 days)
     */
    @Query("""
        DELETE FROM health_deviation_history 
        WHERE userId = :userId 
        AND timestamp < :cutoffTime
    """)
    suspend fun deleteOldRecords(userId: String, cutoffTime: Long)

    /**
     * Get count of deviation records
     */
    @Query("SELECT COUNT(*) FROM health_deviation_history WHERE userId = :userId")
    suspend fun getRecordCount(userId: String): Int
}

