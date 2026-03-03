package com.tharun.vitalmind.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tharun.vitalmind.data.AppDatabase
import com.tharun.vitalmind.data.NotificationHistory
import com.tharun.vitalmind.data.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Background worker that analyzes health data and triggers notifications
 * Runs daily at 7 PM or every 6 hours for periodic checks
 */
class HealthNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "HealthNotificationWorker"
        private const val WORK_NAME = "health_notification_check"

        // Notification cooldown period (24 hours)
        private val NOTIFICATION_COOLDOWN_MS = TimeUnit.HOURS.toMillis(24)
    }

    private val database = AppDatabase.getDatabase(context)
    private val notificationHelper = NotificationHelper(context)
    private val trendAnalyzer = HealthTrendAnalyzer()
    private val alertEngine = AlertEngine(trendAnalyzer)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Starting health notification check...")
            Log.d(TAG, "Current time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")

            // Check if this is a test run (bypass cooldown)
            val bypassCooldown = inputData.getBoolean("bypass_cooldown", false)
            if (bypassCooldown) {
                Log.d(TAG, "🧪 TEST MODE: Cooldown bypass enabled")
            }

            // Get user ID from preferences
            val prefs = applicationContext.getSharedPreferences("vitalmind_prefs", Context.MODE_PRIVATE)
            val userId = prefs.getString("user_id", null)

            if (userId == null) {
                Log.w(TAG, "❌ No user ID found in SharedPreferences, skipping notification check")
                Log.w(TAG, "Make sure user is signed in and setUserIdAndName was called")
                return@withContext Result.success()
            }

            Log.d(TAG, "✅ User ID found: $userId")

            // Check notification settings
            val settingsPrefs = applicationContext.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
            val alertsEnabled = settingsPrefs.getBoolean("alerts_enabled", true)
            val recoveryEnabled = settingsPrefs.getBoolean("recovery_enabled", true)
            val remindersEnabled = settingsPrefs.getBoolean("reminders_enabled", true)
            val suggestionsEnabled = settingsPrefs.getBoolean("suggestions_enabled", true)

            Log.d(TAG, "Notification settings - Alerts: $alertsEnabled, Recovery: $recoveryEnabled, Reminders: $remindersEnabled, Suggestions: $suggestionsEnabled")

            if (!alertsEnabled && !recoveryEnabled && !remindersEnabled && !suggestionsEnabled) {
                Log.d(TAG, "❌ All notifications disabled in settings, skipping")
                return@withContext Result.success()
            }

            // Analyze health data
            Log.d(TAG, "Analyzing health data...")
            val events = analyzeHealthData(userId)
            Log.d(TAG, "Generated ${events.size} potential notification events")

            events.forEachIndexed { index, event ->
                Log.d(TAG, "  Event $index: ${event.type} - ${event.title}")
            }

            // Filter events based on user preferences
            val filteredEvents = events.filter { event ->
                when (event) {
                    is NotificationEvent.HeartRateElevated,
                    is NotificationEvent.LowSleep,
                    is NotificationEvent.LowSteps,
                    is NotificationEvent.HighStress,
                    is NotificationEvent.HighDeviation,
                    is NotificationEvent.SleepTrendDeclining,
                    is NotificationEvent.HeartRateTrendIncreasing,
                    is NotificationEvent.StressTrendIncreasing -> alertsEnabled

                    is NotificationEvent.HeartRateRecovery,
                    is NotificationEvent.StressRecovery,
                    is NotificationEvent.SleepRecovery -> recoveryEnabled

                    is NotificationEvent.StepGoalNearby,
                    is NotificationEvent.InactivityReminder,
                    is NotificationEvent.SleepDebtAccumulating -> remindersEnabled

                    is NotificationEvent.WeatherSuggestion,
                    is NotificationEvent.ActivitySuggestion -> suggestionsEnabled
                }
            }

            Log.d(TAG, "After preference filtering: ${filteredEvents.size} events remain")

            // Check for duplicates and respect cooldown period (unless bypassed for testing)
            val eventsToSend = if (bypassCooldown) {
                Log.d(TAG, "🧪 Skipping duplicate check (test mode)")
                filteredEvents
            } else {
                filterDuplicates(userId, filteredEvents)
            }

            Log.d(TAG, "After duplicate filtering: ${eventsToSend.size} events to send")

            if (eventsToSend.isNotEmpty()) {
                Log.d(TAG, "✅ Sending ${eventsToSend.size} notifications:")
                eventsToSend.forEach { event ->
                    Log.d(TAG, "  📲 ${event.type}: ${event.title}")
                }
                notificationHelper.showNotifications(eventsToSend)

                // Record sent notifications
                eventsToSend.forEach { event ->
                    recordNotification(userId, event)
                }
            } else {
                Log.d(TAG, "ℹ️ No notifications to send (all filtered out or duplicates)")
            }

            // Cleanup old notification history (older than 30 days)
            cleanupOldNotifications()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in health notification worker", e)
            Result.retry()
        }
    }

    /**
     * Analyze health data and generate notification events
     */
    private suspend fun analyzeHealthData(userId: String): List<NotificationEvent> {
        val healthDao = database.healthDataDao()
        val stressDao = database.stressScoreHistoryDao()
        val baselineDao = database.healthDeviationBaselineDao()

        // Get data for last 7 days
        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val recentHealthData = healthDao.getHealthDataSince(userId, sevenDaysAgo).first()
        val recentStressData = stressDao.getHistoryForUser(userId).first().take(7)
        val latestBaseline = baselineDao.getLatestBaseline(userId)

        Log.d(TAG, "Data availability:")
        Log.d(TAG, "  Health records (7 days): ${recentHealthData.size}")
        Log.d(TAG, "  Stress records: ${recentStressData.size}")
        Log.d(TAG, "  Baseline exists: ${latestBaseline != null}")

        if (recentHealthData.isEmpty()) {
            Log.w(TAG, "⚠️ No health data found! Sync data from Google Fit first.")
        }
        if (latestBaseline == null) {
            Log.w(TAG, "⚠️ No baseline data found! Need at least 3 days of data for baseline.")
        }

        // Get today's data
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayData = recentHealthData.filter { it.timestamp >= todayStart }

        Log.d(TAG, "  Today's records: ${todayData.size}")

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Perform analyses
        val heartRateAnalysis = trendAnalyzer.analyzeHeartRate(recentHealthData, latestBaseline)
        val sleepAnalysis = trendAnalyzer.analyzeSleep(recentHealthData, latestBaseline)
        val stepsAnalysis = trendAnalyzer.analyzeSteps(todayData, latestBaseline, currentHour)
        val stressAnalysis = trendAnalyzer.analyzeStress(recentStressData)

        // Log detailed analysis results
        Log.d(TAG, "Analysis Results:")
        Log.d(TAG, "  Heart Rate: current=${heartRateAnalysis.currentRestingHR}, baseline=${heartRateAnalysis.baselineHR}, deviation=${heartRateAnalysis.percentageDeviation}%, elevated=${heartRateAnalysis.isElevated}")
        Log.d(TAG, "  Sleep: last=${sleepAnalysis.lastNightHours}h, average=${sleepAnalysis.averageHours}h, low=${sleepAnalysis.isLow}, deficit=${sleepAnalysis.weeklyDeficitHours}h")
        Log.d(TAG, "  Steps: current=${stepsAnalysis.currentSteps}, expected=${stepsAnalysis.expectedSteps}, low=${stepsAnalysis.isLow}")
        Log.d(TAG, "  Stress: current=${stressAnalysis.currentScore}, high=${stressAnalysis.isHigh}, recovered=${stressAnalysis.isRecovered}")
        Log.d(TAG, "")
        Log.d(TAG, "Notification Trigger Criteria:")
        Log.d(TAG, "  ❌ Heart Rate Alert: Need >15% deviation (current: ${heartRateAnalysis.percentageDeviation}%)")
        Log.d(TAG, "  ❌ Sleep Alert: Need <5h (current: ${sleepAnalysis.lastNightHours}h)")
        Log.d(TAG, "  ❌ Steps Alert: Need <50% expected after noon (current: ${stepsAnalysis.currentSteps}/${stepsAnalysis.expectedSteps})")
        Log.d(TAG, "  ❌ Stress Alert: Need score >7.0 for 2+ days (current: ${stressAnalysis.currentScore})")
        Log.d(TAG, "  💡 Sleep Debt: ${sleepAnalysis.weeklyDeficitHours}h (triggers at >5h)")
        Log.d(TAG, "  💡 Step Goal: Check at 7PM+ if 70-95% of goal")

        // Mock deviation analysis (you can integrate your actual deviation calculator)
        val deviationAnalysis = DeviationAnalysis(
            deviationScore = 0.0f,
            confidence = 0.0f,
            isHigh = false
        )

        // Get weather data for contextual suggestions
        val weather = try {
            WeatherApiService.getTodayWeather("auto:ip")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch weather", e)
            null
        }

        // Generate alerts
        val rawEvents = alertEngine.generateAlerts(
            heartRateAnalysis = heartRateAnalysis,
            sleepAnalysis = sleepAnalysis,
            stepsAnalysis = stepsAnalysis,
            stressAnalysis = stressAnalysis,
            deviationAnalysis = deviationAnalysis,
            weather = weather
        )

        // Filter conflicting events and prioritize
        val filtered = alertEngine.filterConflictingEvents(rawEvents)
        return alertEngine.prioritizeEvents(filtered)
    }

    /**
     * Filter out duplicate notifications based on cooldown period
     */
    private suspend fun filterDuplicates(
        userId: String,
        events: List<NotificationEvent>
    ): List<NotificationEvent> {
        val notificationDao = database.notificationHistoryDao()
        val cutoffTime = System.currentTimeMillis() - NOTIFICATION_COOLDOWN_MS

        Log.d(TAG, "Duplicate filtering:")
        Log.d(TAG, "  Cooldown period: ${NOTIFICATION_COOLDOWN_MS / 1000 / 60} minutes")
        Log.d(TAG, "  Cutoff time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(cutoffTime))}")

        return events.filter { event ->
            // Check if this notification type was sent recently
            val recentCount = notificationDao.hasRecentNotification(
                userId = userId,
                type = event.type,
                sinceTimestamp = cutoffTime
            )

            val shouldSend = if (recentCount > 0) {
                val lastNotification = notificationDao.getLatestNotification(userId, event.type)
                val lastSentTime = lastNotification?.timestamp ?: 0L
                val timeSinceLastMs = System.currentTimeMillis() - lastSentTime
                val timeSinceLastMin = timeSinceLastMs / 1000 / 60

                Log.d(TAG, "  🔄 ${event.type}: Sent recently ($timeSinceLastMin min ago)")
                Log.d(TAG, "     Last sent: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSentTime))}")
                Log.d(TAG, "     Last severity: ${lastNotification?.severity}")
                Log.d(TAG, "     Current severity: ${event.severity}")

                // Allow high severity notifications even if sent recently
                val allowHighSeverity = event.severity == NotificationSeverity.HIGH &&
                        lastNotification?.severity != "HIGH"

                if (allowHighSeverity) {
                    Log.d(TAG, "     ✅ Allowing: Severity increased to HIGH")
                } else {
                    Log.d(TAG, "     ❌ Blocked: Within cooldown period")
                }

                allowHighSeverity
            } else {
                Log.d(TAG, "  ✅ ${event.type}: Not sent recently, allowing")
                true
            }

            shouldSend
        }
    }

    /**
     * Record a sent notification in the database
     */
    private suspend fun recordNotification(userId: String, event: NotificationEvent) {
        val notificationDao = database.notificationHistoryDao()

        val notification = NotificationHistory(
            userId = userId,
            notificationType = event.type,
            timestamp = System.currentTimeMillis(),
            severity = event.severity.name,
            dismissed = false
        )

        notificationDao.insert(notification)
    }

    /**
     * Clean up old notification history
     */
    private suspend fun cleanupOldNotifications() {
        val notificationDao = database.notificationHistoryDao()
        val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        notificationDao.cleanupOldNotifications(cutoffTime)
    }
}

