package com.tharun.vitalmind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HealthData::class,
        StressScoreHistory::class,
        HealthDeviationBaseline::class,
        NotificationHistory::class,
        HealthDeviationHistory::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun healthDataDao(): HealthDataDao
    abstract fun stressScoreHistoryDao(): StressScoreHistoryDao
    abstract fun healthDeviationBaselineDao(): HealthDeviationBaselineDao
    abstract fun notificationHistoryDao(): NotificationHistoryDao
    abstract fun healthDeviationHistoryDao(): HealthDeviationHistoryDao

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
