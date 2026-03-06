package com.tharun.vitalmind.notifications

import com.tharun.vitalmind.data.HealthData
import com.tharun.vitalmind.data.HealthDeviationBaseline
import com.tharun.vitalmind.data.StressScoreHistory
import kotlin.math.abs

/**
 * Analyzes health trends over time to detect anomalies and patterns
 */
class HealthTrendAnalyzer {

    /**
     * Analyze heart rate data for anomalies
     */
    fun analyzeHeartRate(
        recentData: List<HealthData>,
        baseline: HealthDeviationBaseline?
    ): HeartRateAnalysis {
        if (baseline == null || recentData.isEmpty()) {
            return HeartRateAnalysis()
        }

        // Get recent resting heart rate (filter out high activity readings)
        val restingReadings = recentData
            .mapNotNull { it.heartRate }
            .filter { it < 100 } // Consider only resting readings
            .sorted()
            .take(recentData.size / 2) // Take lower half as "resting"

        if (restingReadings.isEmpty()) {
            return HeartRateAnalysis()
        }

        val currentRestingHR = restingReadings.average().toFloat()
        val baselineHR = baseline.resting_heart_rate
        val percentageDiff = ((currentRestingHR - baselineHR) / baselineHR * 100).toInt()

        val isElevated = percentageDiff > 15  // Alert if 15% above baseline
        val isRecovered = !isElevated && abs(percentageDiff) < 5

        // Detect trends (requires at least 3 days of data)
        val trend = detectTrend(restingReadings.takeLast(3))

        return HeartRateAnalysis(
            currentRestingHR = currentRestingHR.toInt(),
            baselineHR = baselineHR.toInt(),
            percentageDeviation = percentageDiff,
            isElevated = isElevated,
            isRecovered = isRecovered,
            trend = trend
        )
    }

    /**
     * Analyze sleep data
     */
    fun analyzeSleep(
        recentData: List<HealthData>,
        baseline: HealthDeviationBaseline?
    ): SleepAnalysis {
        if (baseline == null || recentData.isEmpty()) {
            return SleepAnalysis()
        }

        // Get yesterday's sleep (attribute sleep to the day you wake up)
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1) // Yesterday
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis
        cal.add(java.util.Calendar.DATE, 1)
        val yesterdayEnd = cal.timeInMillis

        // Get all sleep sessions where wake-up time was yesterday
        val lastNightSleepMinutes = recentData
            .filter { it.sleepDuration != null && it.sleepDuration!! > 0 }
            .filter {
                val sleepEnd = it.timestamp + it.sleepDuration!! * 60 * 1000
                sleepEnd > yesterdayStart && sleepEnd <= yesterdayEnd
            }
            .sumOf { it.sleepDuration ?: 0L }

        val lastNightSleep = lastNightSleepMinutes / 60f // Convert to hours
        val averageSleep = baseline.total_sleep_minutes / 60f

        val isLow = lastNightSleep < 5f
        val isRecovered = lastNightSleep > averageSleep && lastNightSleep > 5f

        // Calculate sleep debt (deficit from recommended 7 hours per night)
        // Group by day and sum sleep for each day
        val dailySleepHours = mutableListOf<Float>()
        for (daysAgo in 0 until 7) {
            val dayCal = java.util.Calendar.getInstance()
            dayCal.add(java.util.Calendar.DATE, -daysAgo - 1)
            dayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            dayCal.set(java.util.Calendar.MINUTE, 0)
            dayCal.set(java.util.Calendar.SECOND, 0)
            dayCal.set(java.util.Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis
            dayCal.add(java.util.Calendar.DATE, 1)
            val dayEnd = dayCal.timeInMillis

            val daySleepMinutes = recentData
                .filter { it.sleepDuration != null && it.sleepDuration!! > 0 }
                .filter {
                    val sleepEnd = it.timestamp + it.sleepDuration!! * 60 * 1000
                    sleepEnd > dayStart && sleepEnd <= dayEnd
                }
                .sumOf { it.sleepDuration ?: 0L }

            if (daySleepMinutes > 0) {
                dailySleepHours.add(daySleepMinutes / 60f)
            }
        }

        // Calculate weekly sleep deficit (how many hours short of 7h per night)
        val weeklyDeficit = dailySleepHours.sumOf { maxOf(0.0, 7.0 - it.toDouble()) }.toFloat()

        // Detect declining trend
        val trend = if (dailySleepHours.size >= 3) {
            detectTrend(dailySleepHours.takeLast(3))
        } else 0f

        return SleepAnalysis(
            lastNightHours = lastNightSleep,
            averageHours = averageSleep,
            isLow = isLow,
            isRecovered = isRecovered,
            weeklyDeficitHours = weeklyDeficit,
            decliningTrend = (trend as? Int ?: 0) < 0
        )
    }

    /**
     * Analyze step count
     */
    fun analyzeSteps(
        todayData: List<HealthData>,
        baseline: HealthDeviationBaseline?,
        currentHour: Int
    ): StepsAnalysis {
        if (baseline == null) {
            return StepsAnalysis()
        }

        val todaySteps = todayData.mapNotNull { it.steps }.sum()
        val averageSteps = baseline.steps_total

        // Estimate expected steps by this hour (assuming linear accumulation)
        val expectedByNow = (averageSteps * currentHour / 24f).toInt()

        val isLow = todaySteps < expectedByNow * 0.5 && currentHour > 12
        val percentageOfGoal = if (averageSteps > 0) (todaySteps * 100 / averageSteps) else 0
        val remainingToGoal = maxOf(0, averageSteps - todaySteps)

        return StepsAnalysis(
            currentSteps = todaySteps,
            expectedSteps = expectedByNow,
            goalSteps = averageSteps,
            isLow = isLow,
            percentageOfGoal = percentageOfGoal,
            remainingToGoal = remainingToGoal
        )
    }

    /**
     * Analyze stress scores
     */
    fun analyzeStress(
        recentStress: List<StressScoreHistory>
    ): StressAnalysis {
        if (recentStress.isEmpty()) {
            return StressAnalysis()
        }

        val scores = recentStress.map { it.stress_score }
        val currentScore = scores.lastOrNull() ?: 0f
        val averageScore = scores.average().toFloat()

        // Count consecutive high stress days (score > 7.0)
        var consecutiveHighDays = 0
        for (score in scores.reversed()) {
            if (score > 7.0f) {
                consecutiveHighDays++
            } else {
                break
            }
        }

        val isHigh = consecutiveHighDays >= 2

        // Check for improvement (current lower than recent average)
        val recentAverage = scores.takeLast(3).average().toFloat()
        val improvement = if (scores.size > 3) {
            val previousAverage = scores.dropLast(3).takeLast(3).average().toFloat()
            previousAverage - recentAverage
        } else 0f

        val isRecovered = improvement > 1.5f && currentScore < 6.0f

        // Detect increasing trend
        val trend = detectTrend(scores.takeLast(3))

        return StressAnalysis(
            currentScore = currentScore,
            averageScore = averageScore,
            consecutiveHighDays = consecutiveHighDays,
            isHigh = isHigh,
            isRecovered = isRecovered,
            improvement = improvement,
            increasingTrend = trend > 0
        )
    }

    /**
     * Analyze health deviation score
     */
    fun analyzeDeviation(
        deviationScore: Float,
        confidence: Float
    ): DeviationAnalysis {
        val isHigh = deviationScore > 0.6f && confidence > 0.75f

        return DeviationAnalysis(
            deviationScore = deviationScore,
            confidence = confidence,
            isHigh = isHigh
        )
    }

    /**
     * Detect trend in a series of values
     * Returns: positive if increasing, negative if decreasing, 0 if stable
     */
    private fun detectTrend(values: List<Number>): Int {
        if (values.size < 3) return 0

        val floatValues = values.map { it.toFloat() }
        var increasingCount = 0
        var decreasingCount = 0

        for (i in 1 until floatValues.size) {
            when {
                floatValues[i] > floatValues[i - 1] -> increasingCount++
                floatValues[i] < floatValues[i - 1] -> decreasingCount++
            }
        }

        return when {
            increasingCount > decreasingCount -> 1
            decreasingCount > increasingCount -> -1
            else -> 0
        }
    }
}

// Analysis result data classes
data class HeartRateAnalysis(
    val currentRestingHR: Int = 0,
    val baselineHR: Int = 0,
    val percentageDeviation: Int = 0,
    val isElevated: Boolean = false,
    val isRecovered: Boolean = false,
    val trend: Int = 0 // -1 = decreasing, 0 = stable, 1 = increasing
)

data class SleepAnalysis(
    val lastNightHours: Float = 0f,
    val averageHours: Float = 0f,
    val isLow: Boolean = false,
    val isRecovered: Boolean = false,
    val weeklyDeficitHours: Float = 0f,
    val decliningTrend: Boolean = false
)

data class StepsAnalysis(
    val currentSteps: Int = 0,
    val expectedSteps: Int = 0,
    val goalSteps: Int = 0,
    val isLow: Boolean = false,
    val percentageOfGoal: Int = 0,
    val remainingToGoal: Int = 0
)

data class StressAnalysis(
    val currentScore: Float = 0f,
    val averageScore: Float = 0f,
    val consecutiveHighDays: Int = 0,
    val isHigh: Boolean = false,
    val isRecovered: Boolean = false,
    val improvement: Float = 0f,
    val increasingTrend: Boolean = false
)

data class DeviationAnalysis(
    val deviationScore: Float = 0f,
    val confidence: Float = 0f,
    val isHigh: Boolean = false
)


