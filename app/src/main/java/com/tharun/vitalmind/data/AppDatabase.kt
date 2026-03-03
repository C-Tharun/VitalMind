package com.tharun.vitalmind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tharun.vitalmind.data.StressScoreHistory

@Database(
    entities = [HealthData::class, StressScoreHistory::class, HealthDeviationBaseline::class, NotificationHistory::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun healthDataDao(): HealthDataDao
    abstract fun stressScoreHistoryDao(): StressScoreHistoryDao
    abstract fun healthDeviationBaselineDao(): HealthDeviationBaselineDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vitalsync_database"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
