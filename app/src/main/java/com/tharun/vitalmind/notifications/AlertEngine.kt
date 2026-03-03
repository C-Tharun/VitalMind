package com.tharun.vitalmind.notifications

import com.tharun.vitalmind.data.WeatherApiResponse
import java.util.Calendar

/**
 * Converts health analysis results into notification events
 */
class AlertEngine(
    private val analyzer: HealthTrendAnalyzer = HealthTrendAnalyzer()
) {

    fun generateAlerts(
        heartRateAnalysis: HeartRateAnalysis,
        sleepAnalysis: SleepAnalysis,
        stepsAnalysis: StepsAnalysis,
        stressAnalysis: StressAnalysis,
        deviationAnalysis: DeviationAnalysis,
        weather: WeatherApiResponse? = null
    ): List<NotificationEvent> {
        val events = mutableListOf<NotificationEvent>()

        // 🚨 ALERTS (High Priority)

        // Heart rate elevated
        if (heartRateAnalysis.isElevated) {
            events.add(
                NotificationEvent.HeartRateElevated(
                    percentageAbove = heartRateAnalysis.percentageDeviation,
                    currentHR = heartRateAnalysis.currentRestingHR,
                    baselineHR = heartRateAnalysis.baselineHR
                )
            )
        }

        // Low sleep
        if (sleepAnalysis.isLow) {
            events.add(
                NotificationEvent.LowSleep(
                    hours = sleepAnalysis.lastNightHours,
                    averageHours = sleepAnalysis.averageHours
                )
            )
        }

        // Low steps (only check after noon)
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (stepsAnalysis.isLow && currentHour >= 12) {
            events.add(
                NotificationEvent.LowSteps(
                    currentSteps = stepsAnalysis.currentSteps,
                    averageSteps = stepsAnalysis.expectedSteps,
                    currentTime = "${currentHour}:00"
                )
            )
        }

        // High stress
        if (stressAnalysis.isHigh) {
            events.add(
                NotificationEvent.HighStress(
                    consecutiveDays = stressAnalysis.consecutiveHighDays,
                    averageScore = stressAnalysis.averageScore
                )
            )
        }

        // Health deviation
        if (deviationAnalysis.isHigh) {
            events.add(
                NotificationEvent.HighDeviation(
                    deviationScore = deviationAnalysis.deviationScore,
                    confidence = deviationAnalysis.confidence
                )
            )
        }

        // 🟢 RECOVERY NOTIFICATIONS

        // Heart rate recovery
        if (heartRateAnalysis.isRecovered) {
            events.add(
                NotificationEvent.HeartRateRecovery(
                    currentHR = heartRateAnalysis.currentRestingHR,
                    baselineHR = heartRateAnalysis.baselineHR
                )
            )
        }

        // Stress recovery
        if (stressAnalysis.isRecovered) {
            events.add(
                NotificationEvent.StressRecovery(
                    currentScore = stressAnalysis.currentScore,
                    improvement = stressAnalysis.improvement
                )
            )
        }

        // Sleep recovery
        if (sleepAnalysis.isRecovered) {
            events.add(
                NotificationEvent.SleepRecovery(
                    hours = sleepAnalysis.lastNightHours,
                    previousHours = sleepAnalysis.averageHours
                )
            )
        }

        // 🎯 GOAL REMINDERS

        // Step goal nearby (check after 7 PM)
        if (currentHour >= 19 && stepsAnalysis.percentageOfGoal in 70..95) {
            events.add(
                NotificationEvent.StepGoalNearby(
                    currentSteps = stepsAnalysis.currentSteps,
                    remainingSteps = stepsAnalysis.remainingToGoal,
                    goalSteps = stepsAnalysis.goalSteps
                )
            )
        }

        // Sleep debt accumulating
        if (sleepAnalysis.weeklyDeficitHours > 5f) {
            events.add(
                NotificationEvent.SleepDebtAccumulating(
                    totalDeficitHours = sleepAnalysis.weeklyDeficitHours
                )
            )
        }

        // 🧠 TREND WARNINGS

        // Sleep trend declining
        if (sleepAnalysis.decliningTrend) {
            events.add(
                NotificationEvent.SleepTrendDeclining(
                    days = 3,
                    averageDecline = (sleepAnalysis.averageHours - sleepAnalysis.lastNightHours)
                )
            )
        }

        // Heart rate trend increasing
        if (heartRateAnalysis.trend > 0 && heartRateAnalysis.percentageDeviation > 8) {
            events.add(
                NotificationEvent.HeartRateTrendIncreasing(
                    days = 3,
                    averageIncrease = (heartRateAnalysis.currentRestingHR - heartRateAnalysis.baselineHR) / 3f
                )
            )
        }

        // Stress trend increasing
        if (stressAnalysis.increasingTrend && stressAnalysis.averageScore > 6.0f) {
            events.add(
                NotificationEvent.StressTrendIncreasing(
                    days = 3,
                    currentAverage = stressAnalysis.averageScore
                )
            )
        }

        // 💡 CONTEXTUAL SUGGESTIONS

        // Weather suggestion (only during daylight hours)
        if (weather != null && currentHour in 9..17) {
            val aqi = weather.current.airQuality?.usEpaIndex
            val aqiLevel = when (aqi) {
                1, 2 -> "good"
                3 -> "moderate"
                else -> null
            }

            val temp = weather.current.temp_c

            // Good weather conditions
            if (aqiLevel == "good" && temp in 15f..28f && stepsAnalysis.currentSteps < stepsAnalysis.goalSteps) {
                events.add(
                    NotificationEvent.WeatherSuggestion(
                        weatherCondition = weather.current.condition.text,
                        temperature = temp,
                        aqi = aqiLevel
                    )
                )
            }
        }

        return events
    }

    /**
     * Prioritize events based on severity
     * High severity alerts come first
     */
    fun prioritizeEvents(events: List<NotificationEvent>): List<NotificationEvent> {
        return events.sortedByDescending { event ->
            when (event.severity) {
                NotificationSeverity.HIGH -> 3
                NotificationSeverity.MEDIUM -> 2
                NotificationSeverity.LOW -> 1
            }
        }
    }

    /**
     * Filter out redundant or conflicting events
     * For example, don't show both "heart rate elevated" and "heart rate recovery"
     */
    fun filterConflictingEvents(events: List<NotificationEvent>): List<NotificationEvent> {
        val filtered = mutableListOf<NotificationEvent>()

        val hasHeartRateAlert = events.any { it is NotificationEvent.HeartRateElevated }
        val hasHeartRateRecovery = events.any { it is NotificationEvent.HeartRateRecovery }

        val hasStressAlert = events.any { it is NotificationEvent.HighStress }
        val hasStressRecovery = events.any { it is NotificationEvent.StressRecovery }

        for (event in events) {
            when {
                // Skip recovery if alert exists
                event is NotificationEvent.HeartRateRecovery && hasHeartRateAlert -> continue
                event is NotificationEvent.StressRecovery && hasStressAlert -> continue
                else -> filtered.add(event)
            }
        }

        return filtered
    }
}

