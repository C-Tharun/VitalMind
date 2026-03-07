package com.tharun.vitalmind.notifications

/**
 * Sealed class representing different types of health notifications
 */
sealed class NotificationEvent {
    abstract val severity: NotificationSeverity
    abstract val title: String
    abstract val message: String
    abstract val type: String

    // 🚨 ALERTS (High Priority)
    data class HeartRateElevated(
        val percentageAbove: Int,
        val currentHR: Int,
        val baselineHR: Int
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Elevated Heart Rate Detected"
        override val message = "Your resting heart rate ($currentHR bpm) is $percentageAbove% above your usual baseline ($baselineHR bpm)."
        override val type = "HEART_RATE_ALERT"
    }

    data class LowSleep(
        val hours: Float,
        val averageHours: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Low Sleep Detected"
        override val message = "You slept only ${String.format("%.1f", hours)} hours last night, well below your average of ${String.format("%.1f", averageHours)} hours."
        override val type = "LOW_SLEEP_ALERT"
    }

    data class LowSteps(
        val currentSteps: Int,
        val averageSteps: Int,
        val currentTime: String
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = "Activity Below Normal"
        override val message = "You have only $currentSteps steps at $currentTime, compared to your average of $averageSteps by this time."
        override val type = "LOW_STEPS_ALERT"
    }

    data class HighStress(
        val consecutiveDays: Int,
        val averageScore: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Elevated Stress Detected"
        override val message = "Your stress levels have been consistently high for $consecutiveDays days (avg score: ${String.format("%.1f", averageScore)})."
        override val type = "HIGH_STRESS_ALERT"
    }

    data class HighDeviation(
        val deviationScore: Float,
        val confidence: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Health Deviation Detected"
        override val message = "Your health metrics are deviating from your baseline (score: ${String.format("%.1f", deviationScore)}, confidence: ${String.format("%.0f", confidence * 100)}%)."
        override val type = "DEVIATION_ALERT"
    }

    // 🟢 RECOVERY NOTIFICATIONS
    data class HeartRateRecovery(
        val currentHR: Int,
        val baselineHR: Int
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.LOW
        override val title = "Recovery Detected"
        override val message = "Your heart rate has returned to normal levels ($currentHR bpm, baseline: $baselineHR bpm). Great recovery!"
        override val type = "HEART_RATE_RECOVERY"
    }

    data class StressRecovery(
        val currentScore: Float,
        val improvement: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.LOW
        override val title = "Stress Improvement"
        override val message = "Your stress levels have improved significantly (current: ${String.format("%.1f", currentScore)}, improvement: ${String.format("%.1f", improvement)})."
        override val type = "STRESS_RECOVERY"
    }

    data class SleepRecovery(
        val hours: Float,
        val previousHours: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.LOW
        override val title = "Better Sleep Detected"
        override val message = "You slept ${String.format("%.1f", hours)} hours last night, a good recovery from ${String.format("%.1f", previousHours)} hours."
        override val type = "SLEEP_RECOVERY"
    }

    // 🎯 GOAL REMINDERS
    data class StepGoalNearby(
        val currentSteps: Int,
        val remainingSteps: Int,
        val goalSteps: Int
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = "You're Close to Your Step Goal"
        override val message = "Only $remainingSteps steps left to reach your daily goal of $goalSteps steps!"
        override val type = "STEP_GOAL_REMINDER"
    }

    data class InactivityReminder(
        val hours: Int
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = "Time to Move"
        override val message = "You've been inactive for $hours hours. A short walk can help boost your energy!"
        override val type = "INACTIVITY_REMINDER"
    }

    data class SleepDebtAccumulating(
        val totalDeficitHours: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = "Sleep Debt Accumulating"
        override val message = "You've accumulated ${String.format("%.1f", totalDeficitHours)} hours of sleep debt this week. Consider getting extra rest tonight."
        override val type = "SLEEP_DEBT_REMINDER"
    }

    // 🧠 TREND WARNINGS (Predictive)
    data class SleepTrendDeclining(
        val days: Int,
        val averageDecline: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Sleep Quality Declining"
        override val message = "Your sleep has decreased for $days consecutive days (avg decline: ${String.format("%.1f", averageDecline)} hours/night)."
        override val type = "SLEEP_TREND_WARNING"
    }

    data class HeartRateTrendIncreasing(
        val days: Int,
        val averageIncrease: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Heart Rate Trend Rising"
        override val message = "Your resting heart rate has been increasing for $days days (avg increase: ${String.format("%.1f", averageIncrease)} bpm/day)."
        override val type = "HEART_RATE_TREND_WARNING"
    }

    data class StressTrendIncreasing(
        val days: Int,
        val currentAverage: Float
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.HIGH
        override val title = "Stress Accumulating"
        override val message = "Your stress levels have been rising for $days days (current avg: ${String.format("%.1f", currentAverage)})."
        override val type = "STRESS_TREND_WARNING"
    }

    // 💡 CONTEXTUAL SUGGESTIONS
    data class WeatherSuggestion(
        val weatherCondition: String,
        val temperature: Float,
        val aqi: String
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.LOW
        override val title = "Great Weather for a Walk"
        override val message = "AQI is $aqi and temperature is ${String.format("%.0f", temperature)}°C. Ideal time for outdoor activity!"
        override val type = "WEATHER_SUGGESTION"
    }

    data class ActivitySuggestion(
        val timeOfDay: String,
        val reason: String
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.LOW
        override val title = "Activity Suggestion"
        override val message = "$timeOfDay is a great time for activity. $reason"
        override val type = "ACTIVITY_SUGGESTION"
    }

    // 🌅 DAILY GOAL REMINDERS
    data class MorningMotivation(
        val goalSteps: Int,
        val userName: String = ""
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = "Ready for Today?"
        override val message = if (userName.isNotEmpty()) {
            "Good morning $userName! Your goal is $goalSteps steps today. Let's make it happen! 💪"
        } else {
            "Good morning! Your goal is $goalSteps steps today. Let's make it happen! 💪"
        }
        override val type = "MORNING_MOTIVATION"
    }

    data class EveningGoalPush(
        val currentSteps: Int,
        val goalSteps: Int,
        val remaining: Int
    ) : NotificationEvent() {
        override val severity = NotificationSeverity.MEDIUM
        override val title = if (remaining > 0) "Push to Finish Strong!" else "Goal Achieved! 🎉"
        override val message = if (remaining > 0) {
            "You're at $currentSteps/$goalSteps steps. Just $remaining steps to go! You can do it!"
        } else {
            "Amazing! You've reached your goal of $goalSteps steps today! 🌟"
        }
        override val type = "EVENING_GOAL_PUSH"
    }
}

/**
 * Severity levels for notifications
 */
enum class NotificationSeverity {
    LOW,    // Suggestions, recoveries
    MEDIUM, // Reminders, moderate alerts
    HIGH    // Critical alerts, trends
}

