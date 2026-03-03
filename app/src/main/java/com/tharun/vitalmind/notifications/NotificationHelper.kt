package com.tharun.vitalmind.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tharun.vitalmind.MainActivity

/**
 * Helper class for creating and managing health notifications
 * Follows Android 13+ notification standards
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"

        // Notification Channel IDs
        const val CHANNEL_ALERTS = "health_alerts"
        const val CHANNEL_RECOVERY = "health_recovery"
        const val CHANNEL_REMINDERS = "health_reminders"
        const val CHANNEL_SUGGESTIONS = "health_suggestions"

        // Notification IDs
        private const val BASE_NOTIFICATION_ID = 1000

        /**
         * Create notification channels (call this from Application or MainActivity)
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // 🚨 Alerts Channel (High Priority)
                val alertsChannel = NotificationChannel(
                    CHANNEL_ALERTS,
                    "Health Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical health alerts and warnings"
                    enableVibration(true)
                    enableLights(true)
                }

                // 🟢 Recovery Channel (Default Priority)
                val recoveryChannel = NotificationChannel(
                    CHANNEL_RECOVERY,
                    "Health Recovery",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Positive health recovery notifications"
                    enableLights(true)
                }

                // 🎯 Reminders Channel (Default Priority)
                val remindersChannel = NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Health Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Goal reminders and activity prompts"
                }

                // 💡 Suggestions Channel (Low Priority)
                val suggestionsChannel = NotificationChannel(
                    CHANNEL_SUGGESTIONS,
                    "Health Suggestions",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Contextual health suggestions"
                }

                notificationManager.createNotificationChannels(
                    listOf(alertsChannel, recoveryChannel, remindersChannel, suggestionsChannel)
                )

                Log.d(TAG, "Notification channels created successfully")
            }
        }
    }

    /**
     * Build and show a notification for the given event
     */
    fun showNotification(event: NotificationEvent, notificationId: Int = BASE_NOTIFICATION_ID) {
        Log.d(TAG, "showNotification() called for event: ${event.type}")

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Android 13+ notification permission: $hasPermission")

            if (!hasPermission) {
                Log.e(TAG, "❌ POST_NOTIFICATIONS permission NOT granted! Cannot show notification.")
                Log.e(TAG, "Go to: Settings → Apps → VitalMind → Notifications → Allow notifications")
                return
            }
        }

        val channelId = getChannelForEvent(event)
        val priority = getPriorityForSeverity(event.severity)
        val color = getColorForEvent(event)
        val icon = getIconForEvent(event)

        Log.d(TAG, "Notification details:")
        Log.d(TAG, "  Channel: $channelId")
        Log.d(TAG, "  Title: ${event.title}")
        Log.d(TAG, "  Message: ${event.message}")
        Log.d(TAG, "  Priority: $priority")
        Log.d(TAG, "  ID: $notificationId")

        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", event.type)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(event.title)
            .setContentText(event.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.message))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(color)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        Log.d(TAG, "Notification built successfully, calling notify()...")

        // Show the notification
        try {
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
            Log.d(TAG, "✅ Notification sent successfully! ID: $notificationId")
            Log.d(TAG, "Check your notification shade (swipe down from top)")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException: Notification permission denied!", e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send notification", e)
        }
    }

    /**
     * Show multiple notifications (limited to top 3 by priority)
     */
    fun showNotifications(events: List<NotificationEvent>) {
        events.take(3).forEachIndexed { index, event ->
            showNotification(event, BASE_NOTIFICATION_ID + index)
        }
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        with(NotificationManagerCompat.from(context)) {
            cancel(notificationId)
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        with(NotificationManagerCompat.from(context)) {
            cancelAll()
        }
    }

    /**
     * Get the appropriate channel for an event type
     */
    private fun getChannelForEvent(event: NotificationEvent): String {
        return when (event) {
            is NotificationEvent.HeartRateElevated,
            is NotificationEvent.LowSleep,
            is NotificationEvent.LowSteps,
            is NotificationEvent.HighStress,
            is NotificationEvent.HighDeviation,
            is NotificationEvent.SleepTrendDeclining,
            is NotificationEvent.HeartRateTrendIncreasing,
            is NotificationEvent.StressTrendIncreasing -> CHANNEL_ALERTS

            is NotificationEvent.HeartRateRecovery,
            is NotificationEvent.StressRecovery,
            is NotificationEvent.SleepRecovery -> CHANNEL_RECOVERY

            is NotificationEvent.StepGoalNearby,
            is NotificationEvent.InactivityReminder,
            is NotificationEvent.SleepDebtAccumulating -> CHANNEL_REMINDERS

            is NotificationEvent.WeatherSuggestion,
            is NotificationEvent.ActivitySuggestion -> CHANNEL_SUGGESTIONS
        }
    }

    /**
     * Get notification priority based on severity
     */
    private fun getPriorityForSeverity(severity: NotificationSeverity): Int {
        return when (severity) {
            NotificationSeverity.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationSeverity.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            NotificationSeverity.LOW -> NotificationCompat.PRIORITY_LOW
        }
    }

    /**
     * Get color based on event type
     */
    private fun getColorForEvent(event: NotificationEvent): Int {
        return when (event) {
            // Red for alerts
            is NotificationEvent.HeartRateElevated,
            is NotificationEvent.LowSleep,
            is NotificationEvent.HighStress,
            is NotificationEvent.HighDeviation,
            is NotificationEvent.SleepTrendDeclining,
            is NotificationEvent.HeartRateTrendIncreasing,
            is NotificationEvent.StressTrendIncreasing -> 0xFFE57373.toInt() // Light Red

            // Green for recovery
            is NotificationEvent.HeartRateRecovery,
            is NotificationEvent.StressRecovery,
            is NotificationEvent.SleepRecovery -> 0xFF81C784.toInt() // Light Green

            // Blue for reminders
            is NotificationEvent.StepGoalNearby,
            is NotificationEvent.InactivityReminder,
            is NotificationEvent.SleepDebtAccumulating -> 0xFF64B5F6.toInt() // Light Blue

            // Teal for suggestions
            is NotificationEvent.WeatherSuggestion,
            is NotificationEvent.ActivitySuggestion,
            is NotificationEvent.LowSteps -> 0xFF4DB6AC.toInt() // Teal
        }
    }

    /**
     * Get appropriate icon for event type
     */
    private fun getIconForEvent(event: NotificationEvent): Int {
        // Using Android's built-in icons
        // In production, you would use custom icons from your drawable resources
        return when (event) {
            is NotificationEvent.HeartRateElevated,
            is NotificationEvent.HeartRateRecovery,
            is NotificationEvent.HeartRateTrendIncreasing -> android.R.drawable.ic_dialog_info

            is NotificationEvent.LowSleep,
            is NotificationEvent.SleepRecovery,
            is NotificationEvent.SleepTrendDeclining,
            is NotificationEvent.SleepDebtAccumulating -> android.R.drawable.ic_lock_idle_alarm

            is NotificationEvent.LowSteps,
            is NotificationEvent.StepGoalNearby,
            is NotificationEvent.InactivityReminder -> android.R.drawable.ic_menu_mylocation

            is NotificationEvent.HighStress,
            is NotificationEvent.StressRecovery,
            is NotificationEvent.StressTrendIncreasing -> android.R.drawable.ic_dialog_alert

            is NotificationEvent.WeatherSuggestion -> android.R.drawable.ic_menu_mylocation

            else -> android.R.drawable.ic_dialog_info
        }
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older versions
        }
    }
}



