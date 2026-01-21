package com.tharun.vitalmind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StressScoreHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: StressScoreHistory)

    @Query("SELECT * FROM stress_score_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getHistoryForUser(userId: String): Flow<List<StressScoreHistory>>
}

