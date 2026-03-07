package com.tharun.vitalmind.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Utility class to schedule and manage notification workers
 */
object NotificationScheduler {

    private const val TAG = "NotificationScheduler"
    private const val DAILY_WORK_NAME = "daily_health_notification"
    private const val PERIODIC_WORK_NAME = "periodic_health_notification"
    private const val MORNING_REMINDER_WORK_NAME = "morning_goal_reminder"
    private const val EVENING_REMINDER_WORK_NAME = "evening_goal_reminder"

    /**
     * Schedule all daily notifications (morning, evening, and health checks)
     */
    fun scheduleAllDailyNotifications(context: Context) {
        scheduleMorningGoalReminder(context)
        scheduleEveningGoalReminder(context)
        scheduleDailyNotification(context)
        Log.d(TAG, "✅ All daily notifications scheduled!")
    }

    /**
     * Schedule morning goal motivation (9 AM)
     */
    fun scheduleMorningGoalReminder(context: Context) {
        scheduleNotificationAt(
            context = context,
            hour = 9,
            minute = 0,
            workName = MORNING_REMINDER_WORK_NAME,
            tag = "morning_reminder",
            notificationType = "morning_motivation"
        )
    }

    /**
     * Schedule evening goal push (7 PM)
     */
    fun scheduleEveningGoalReminder(context: Context) {
        scheduleNotificationAt(
            context = context,
            hour = 19,
            minute = 0,
            workName = EVENING_REMINDER_WORK_NAME,
            tag = "evening_reminder",
            notificationType = "evening_goal_push"
        )
    }

    /**
     * Generic function to schedule a notification at a specific time
     */
    private fun scheduleNotificationAt(
        context: Context,
        hour: Int,
        minute: Int,
        workName: String,
        tag: String,
        notificationType: String
    ) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If it's already past the scheduled time, schedule for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val hoursUntil = timeDiff / 1000 / 60 / 60
        val minutesUntil = (timeDiff / 1000 / 60) % 60

        Log.d(TAG, "Scheduling $notificationType:")
        Log.d(TAG, "  Scheduled for: ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(dueDate.time)}")
        Log.d(TAG, "  Time until: ${hoursUntil}h ${minutesUntil}m")

        val constraints = Constraints.Builder().build()

        val inputData = Data.Builder()
            .putBoolean("bypass_cooldown", true)
            .putString("notification_type", notificationType)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<HealthNotificationWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(tag)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedule daily notification check at 7 PM
     */
    fun scheduleDailyNotification(context: Context) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20) // 8 PM (20:00) - TESTING TODAY
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If it's already past 8 PM, schedule for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
            Log.d(TAG, "Past 8:00 PM today, scheduling for tomorrow")
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val hoursUntil = timeDiff / 1000 / 60 / 60
        val minutesUntil = (timeDiff / 1000 / 60) % 60

        Log.d(TAG, "========================================")
        Log.d(TAG, "Scheduling Daily Notification:")
        Log.d(TAG, "  Current time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(currentDate.time)}")
        Log.d(TAG, "  Scheduled for: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(dueDate.time)}")
        Log.d(TAG, "  Time until: ${hoursUntil}h ${minutesUntil}m")
        Log.d(TAG, "========================================")

        // Remove network constraint for testing to ensure immediate execution
        val constraints = Constraints.Builder()
            // .setRequiredNetworkType(NetworkType.CONNECTED) // Commented out for testing
            .build()

        // Add bypass_cooldown for testing scheduled notifications
        val inputData = Data.Builder()
            .putBoolean("bypass_cooldown", true) // Bypass cooldown for testing
            .build()

        val dailyWorkRequest = OneTimeWorkRequestBuilder<HealthNotificationWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("daily_notification")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            dailyWorkRequest
        )

        Log.d(TAG, "✅ Daily notification scheduled successfully!")
        Log.d(TAG, "🧪 TEST MODE: Cooldown will be bypassed for this scheduled check")
    }

    /**
     * Schedule periodic notification check (every 6 hours)
     */
    fun schedulePeriodicNotification(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<HealthNotificationWorker>(
            6, TimeUnit.HOURS,
            15, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .addTag("periodic_notification")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Cancel all scheduled notification workers
     */
    fun cancelAllNotifications(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(DAILY_WORK_NAME)
            cancelUniqueWork(PERIODIC_WORK_NAME)
        }
    }

    /**
     * Trigger an immediate notification check (for testing)
     * @param bypassCooldown If true, ignores the 24h cooldown for duplicate notifications
     */
    fun triggerImmediateCheck(context: Context, bypassCooldown: Boolean = true) {
        Log.d(TAG, "Triggering immediate notification check (bypass cooldown: $bypassCooldown)")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putBoolean("bypass_cooldown", bypassCooldown)
            .build()

        val immediateWorkRequest = OneTimeWorkRequestBuilder<HealthNotificationWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("immediate_notification")
            .build()

        WorkManager.getInstance(context).enqueue(immediateWorkRequest)
        Log.d(TAG, "✅ Immediate check enqueued")
    }
}

