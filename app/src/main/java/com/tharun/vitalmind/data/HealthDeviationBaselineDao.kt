package com.tharun.vitalmind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Health Deviation Baseline data operations
 */
@Dao
interface HealthDeviationBaselineDao {

    /**
     * Insert or update baseline data for a specific day
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseline(baseline: HealthDeviationBaseline)

    /**
     * Get count of unique baseline days for a user
     */
    @Query("SELECT COUNT(DISTINCT date) FROM health_deviation_baseline WHERE userId = :userId")
    suspend fun getBaselineDaysCount(userId: String): Int

    /**
     * Get all baseline data for a user (for computing statistics)
     */
    @Query("SELECT * FROM health_deviation_baseline WHERE userId = :userId ORDER BY timestamp DESC")
    fun getBaselineData(userId: String): Flow<List<HealthDeviationBaseline>>

    /**
     * Check if baseline exists for a specific date
     */
    @Query("SELECT COUNT(*) FROM health_deviation_baseline WHERE userId = :userId AND date = :date")
    suspend fun hasBaselineForDate(userId: String, date: String): Int

    /**
     * Get the most recent baseline record for a user
     */
    @Query("SELECT * FROM health_deviation_baseline WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBaseline(userId: String): HealthDeviationBaseline?

    /**
     * Delete all baseline data for a user (for testing/reset)
     */
    @Query("DELETE FROM health_deviation_baseline WHERE userId = :userId")
    suspend fun deleteBaselineForUser(userId: String)
}

