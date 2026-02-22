package com.tharun.vitalmind.data.repository

import android.util.Log
import com.tharun.vitalmind.data.HealthData
import com.tharun.vitalmind.data.HealthDataRepository
import com.tharun.vitalmind.data.HealthDeviationBaseline
import com.tharun.vitalmind.data.HealthDeviationBaselineDao
import com.tharun.vitalmind.data.remote.HealthDeviationRequest
import com.tharun.vitalmind.data.remote.HealthDeviationResponse
import com.tharun.vitalmind.data.remote.TrainBaselineRequest
import com.tharun.vitalmind.data.remote.TrainBaselineResponse
import com.tharun.vitalmind.data.remote.BaselineDay
import com.tharun.vitalmind.data.BaselineTrainingPreferences
import com.tharun.vitalmind.ui.healthdeviation.BaselineStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

/**
 * Retrofit API service for Health Deviation backend
 */
interface HealthDeviationApiService {
    @POST("/health_deviation")
    suspend fun analyzeHealthDeviation(@Body request: HealthDeviationRequest): HealthDeviationResponse

    @POST("/train_baseline_model")
    suspend fun trainBaseline(@Body request: TrainBaselineRequest): TrainBaselineResponse
}

/**
 * Repository for Health Deviation (PHBD-Net) feature with personalized baseline support
 */
class HealthDeviationRepository(
    private val healthDataRepository: HealthDataRepository,
    private val baselineDao: HealthDeviationBaselineDao,
    private val trainingPreferences: BaselineTrainingPreferences,
    private val userId: String
) {
    companion object {
        const val MINIMUM_BASELINE_DAYS = 10
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)  // Increased for backend cold starts on Render
        .readTimeout(90, TimeUnit.SECONDS)     // Increased for backend cold starts on Render
        .writeTimeout(90, TimeUnit.SECONDS)    // Increased for backend cold starts on Render
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://vitalmind-stress-api.onrender.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(HealthDeviationApiService::class.java) }

    /**
     * Check baseline status for the current user
     */
    suspend fun getBaselineStatus(): Pair<BaselineStatus, Int> = withContext(Dispatchers.IO) {
        val daysCount = baselineDao.getBaselineDaysCount(userId)
        val status = if (daysCount >= MINIMUM_BASELINE_DAYS) BaselineStatus.READY else BaselineStatus.COLLECTING
        Pair(status, daysCount)
    }

    /**
     * Check if the baseline model has been trained on the server
     */
    fun isModelTrained(): Boolean {
        return trainingPreferences.isModelTrained(userId)
    }

    /**
     * Check if model needs retraining (30 days passed)
     */
    fun needsRetraining(): Boolean {
        return trainingPreferences.needsRetraining(userId)
    }

    /**
     * Check if there is new baseline data collected since the last training
     * Required for monthly retraining to ensure we have fresh data
     *
     * @return true if at least 10 baseline days exist AFTER lastTrainingDate
     */
    suspend fun hasNewBaselineDataSinceLastTraining(): Boolean = withContext(Dispatchers.IO) {
        val lastTrainingDate = trainingPreferences.getLastTrainingDate(userId)

        if (lastTrainingDate == 0L) {
            // Never trained before - check if we have minimum baseline days
            val allBaseline = baselineDao.getBaselineData(userId).first()
            return@withContext allBaseline.size >= MINIMUM_BASELINE_DAYS
        }

        // Get baseline data created AFTER last training
        val allBaseline = baselineDao.getBaselineData(userId).first()
        val newBaselineDays = allBaseline.filter { it.timestamp > lastTrainingDate }

        val hasNewData = newBaselineDays.size >= MINIMUM_BASELINE_DAYS

        Log.d("PHBD_TRAINING", "New baseline days since last training: ${newBaselineDays.size} (need $MINIMUM_BASELINE_DAYS)")

        return@withContext hasNewData
    }

    /**
     * Collect and store today's baseline data
     *
     * Safety: Only collects baseline if sufficient data exists for TODAY.
     * This prevents premature baseline creation before Google Fit sync completes.
     *
     * Called automatically on ViewModel init, but gracefully skips if data is insufficient.
     */
    suspend fun collectTodaysBaseline(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = dayKey(System.currentTimeMillis())

            // Check if we already have baseline for today
            val hasToday = baselineDao.hasBaselineForDate(userId, today) > 0
            if (hasToday) {
                Log.d("HealthDeviationRepo", "✅ Baseline already exists for today: $today")
                return@withContext Result.success(Unit)
            }

            // Get all health data for the user
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            if (healthDataList.isEmpty()) {
                Log.w("HealthDeviationRepo", "⚠️ No health data available to create baseline - skipping")
                return@withContext Result.failure(Exception("No health data available"))
            }

            // Get TODAY's data
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000

            val todayData = healthDataList.filter { it.timestamp >= todayStart && it.timestamp < todayEnd }

            if (todayData.isEmpty()) {
                Log.w("HealthDeviationRepo", "⚠️ No health data for today yet - skipping baseline collection (will retry on next sync)")
                return@withContext Result.failure(Exception("No health data for today"))
            }

            /**
             * Data Sufficiency Check:
             * Ensure we have enough quality data before creating baseline.
             * This prevents baseline creation immediately after app launch,
             * before Google Fit sync has completed.
             *
             * Minimum requirements:
             * - At least 3 heart rate samples OR
             * - Steps or calories data present
             */
            val heartRateSamples = todayData.mapNotNull { it.heartRate }
            val hasSteps = todayData.any { it.steps != null && it.steps > 0 }
            val hasCalories = todayData.any { it.calories != null && it.calories > 0f }
            val hasSufficientHR = heartRateSamples.size >= 3

            val hasSufficientData = hasSufficientHR || hasSteps || hasCalories

            if (!hasSufficientData) {
                Log.w("HealthDeviationRepo", "⚠️ Insufficient data for baseline (HR samples: ${heartRateSamples.size}, hasSteps: $hasSteps, hasCal: $hasCalories)")
                Log.w("HealthDeviationRepo", "   Waiting for more sync data - will retry on next app open")
                return@withContext Result.failure(Exception("Insufficient data for today"))
            }

            // Get recent 7 days for computing trends
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val recentData = healthDataList.filter { it.timestamp >= sevenDaysAgo }

            // Compute all metrics for today
            val todaySteps = todayData.sumOf { it.steps ?: 0 }
            val todayCalories = todayData.sumOf { it.calories?.toDouble() ?: 0.0 }.toFloat()
            val todayHeartRates = todayData.mapNotNull { it.heartRate }
            val todayMoveMinutes = todayData.sumOf { it.moveMinutes ?: 0 }

            val avgHeartRate = computeAvgHeartRate(todayHeartRates, recentData)
            val restingHeartRate = computeRestingHeartRate(recentData, avgHeartRate)
            val hrVariance = computeHeartRateVariance(recentData)
            val totalSleepMinutes = computeTodaySleepMinutes(todayData, todayStart)
            val caloriesBurned = if (todayCalories > 0f) todayCalories else computeEstimatedCalories(todaySteps)
            val sedentaryRatio = computeSedentaryRatio(todayMoveMinutes, todaySteps)
            val movementVariance = computeMovementVariance(recentData)
            val activityLoadIndex = computeActivityLoadIndex(todaySteps, caloriesBurned)
            val sleepVariance = computeSleepVariance(recentData)

            // Create baseline record
            val baseline = HealthDeviationBaseline(
                userId = userId,
                date = today,
                timestamp = todayStart,
                avg_heart_rate = avgHeartRate,
                resting_heart_rate = restingHeartRate,
                hr_variance = hrVariance,
                steps_total = todaySteps,
                total_sleep_minutes = totalSleepMinutes,
                calories_burned = caloriesBurned,
                sedentary_ratio = sedentaryRatio,
                movement_variance = movementVariance,
                activity_load_index = activityLoadIndex,
                sleep_consistency = sleepVariance
            )

            baselineDao.insertBaseline(baseline)
            Log.d("HealthDeviationRepo", "✅ Baseline saved for $today")
            Log.d("HealthDeviationRepo", "   Steps: $todaySteps, Sleep: ${totalSleepMinutes}min, HR samples: ${todayHeartRates.size}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("HealthDeviationRepo", "❌ Error collecting baseline: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Analyzes health deviation based on current health data
     * Returns Result.success or Result.failure
     */
    suspend fun getHealthDeviation(): Result<HealthDeviationResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthDeviationRepo", "=== Starting Health Deviation Analysis ===")
            Log.d("HealthDeviationRepo", "User ID: $userId")
            Log.d("HealthDeviationRepo", "⏰ Current time: ${System.currentTimeMillis()}")

            // Get all health data for the user
            Log.d("HealthDeviationRepo", "📊 Fetching health data from repository...")
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            Log.d("HealthDeviationRepo", "Retrieved ${healthDataList.size} health data records")

            if (healthDataList.isEmpty()) {
                Log.e("HealthDeviationRepo", "❌ No synced health data available for user $userId")
                return@withContext Result.failure(Exception("No health data available. Please sync with Google Fit first."))
            }

            // Get TODAY's data (matching MainViewModel logic)
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000

            val todayData = healthDataList.filter { it.timestamp >= todayStart && it.timestamp < todayEnd }
            Log.d("HealthDeviationRepo", "Today's data records: ${todayData.size}")

            // Get recent 7 days for computing trends
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val recentData = healthDataList.filter { it.timestamp >= sevenDaysAgo }
            Log.d("HealthDeviationRepo", "Recent data (7 days): ${recentData.size} records")

            // Aggregate TODAY's data (like Dashboard does)
            val todaySteps = todayData.sumOf { it.steps ?: 0 }
            val todayCalories = todayData.sumOf { it.calories?.toDouble() ?: 0.0 }.toFloat()
            val todayHeartRates = todayData.mapNotNull { it.heartRate }
            val todayMoveMinutes = todayData.sumOf { it.moveMinutes ?: 0 }

            Log.d("HealthDeviationRepo", "TODAY's aggregated data:")
            Log.d("HealthDeviationRepo", "  Today steps: $todaySteps")
            Log.d("HealthDeviationRepo", "  Today calories: $todayCalories")
            Log.d("HealthDeviationRepo", "  Today move minutes: $todayMoveMinutes")

            // Compute derived metrics using both today's and recent data
            val avgHeartRate = computeAvgHeartRate(todayHeartRates, recentData)
            val restingHeartRate = computeRestingHeartRate(recentData, avgHeartRate)
            val hrVariance = computeHeartRateVariance(recentData)
            val stepsTotal = todaySteps
            val totalSleepMinutes = computeTodaySleepMinutes(todayData, todayStart)
            val caloriesBurned = if (todayCalories > 0f) todayCalories else computeEstimatedCalories(stepsTotal)
            val sedentaryRatio = computeSedentaryRatio(todayMoveMinutes, stepsTotal)
            val movementVariance = computeMovementVariance(recentData)
            val activityLoadIndex = computeActivityLoadIndex(stepsTotal, caloriesBurned)
            val sleepVariance = computeSleepVariance(recentData)

            Log.d("HealthDeviationRepo", "Computed metrics:")
            Log.d("HealthDeviationRepo", "  avg_heart_rate: $avgHeartRate")
            Log.d("HealthDeviationRepo", "  resting_heart_rate: $restingHeartRate")
            Log.d("HealthDeviationRepo", "  hr_variance: $hrVariance")
            Log.d("HealthDeviationRepo", "  steps_total: $stepsTotal")
            Log.d("HealthDeviationRepo", "  total_sleep_minutes: $totalSleepMinutes")
            Log.d("HealthDeviationRepo", "  calories_burned: $caloriesBurned")
            Log.d("HealthDeviationRepo", "  sedentary_ratio: $sedentaryRatio")
            Log.d("HealthDeviationRepo", "  movement_variance: $movementVariance")
            Log.d("HealthDeviationRepo", "  activity_load_index: $activityLoadIndex")
            Log.d("HealthDeviationRepo", "  sleep_variance: $sleepVariance")

            val now = java.util.Calendar.getInstance()

            // Build request with all required fields
            val request = HealthDeviationRequest(
                user_id = userId,
                avg_heart_rate = avgHeartRate,
                resting_heart_rate = restingHeartRate,
                hr_variance = hrVariance,
                steps_total = stepsTotal,
                total_sleep_minutes = totalSleepMinutes,
                calories_burned = caloriesBurned,
                sedentary_ratio = sedentaryRatio,
                movement_variance = movementVariance,
                activity_load_index = activityLoadIndex,
                sleep_consistency = sleepVariance,  // Using variance (lower = more consistent)
                hour_of_day = now.get(java.util.Calendar.HOUR_OF_DAY),
                is_sedentary = if (stepsTotal < 1000) 1 else 0
            )

            Log.d("HealthDeviationRepo", "📤 Sending request to API")
            val response = api.analyzeHealthDeviation(request)

            Log.d("HealthDeviationRepo", "📥 Received response: $response")
            Log.d("HealthDeviationRepo", "✅ Health deviation score: ${response.health_deviation_score}")
            Log.d("HealthDeviationRepo", "✅ Drift level: ${response.stress_drift_level}")
            Log.d("HealthDeviationRepo", "✅ Confidence: ${response.confidence}")

            Result.success(response)

        } catch (e: SocketTimeoutException) {
            Log.e("HealthDeviationRepo", "⏱️ Timeout error: ${e.message}", e)
            Log.e("HealthDeviationRepo", "Stack trace:", e)
            Log.w("HealthDeviationRepo", "💡 Tip: Backend may be cold-starting. This can take 30-60 seconds on first request.")
            Result.failure(Exception("Request timeout. The backend may be starting up. Please try again in a moment."))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("HealthDeviationRepo", "🌐 HTTP error ${e.code()}: $errorBody", e)
            Log.e("HealthDeviationRepo", "Response: ${e.response()}")

            // ✅ CRITICAL: Handle 404 - model missing on backend
            // This can happen after backend redeploy or model expiration
            if (e.code() == 404) {
                Log.e("HealthDeviationRepo", "🔄 Model missing on backend (404). Resetting training status.")
                trainingPreferences.resetTrainingStatus(userId)
                Log.d("HealthDeviationRepo", "   Training status reset. User will need to retrain model.")
                Result.failure(Exception("Model missing on server. Please retrain your baseline model."))
            } else {
                Result.failure(Exception("Server error: ${e.code()}"))
            }
        } catch (e: Exception) {
            Log.e("HealthDeviationRepo", "❌ Unexpected error: ${e.javaClass.simpleName} - ${e.message}", e)
            Log.e("HealthDeviationRepo", "Stack trace:", e)
            Result.failure(e)
        }
    }

    /**
     * Train the baseline model on the server using collected baseline data
     *
     * DATA SOURCE: Uses ONLY baseline data from baselineDao (NOT healthDataRepository)
     *
     * Called when:
     * 1. Baseline becomes READY (10 days collected) AND model not yet trained
     * 2. Monthly retraining: 30 days passed AND 10 new baseline days exist
     *
     * @return Result.success if training completed, Result.failure otherwise
     */
    suspend fun trainBaselineOnServer(): Result<TrainBaselineResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("PHBD_TRAINING", "=== Starting Baseline Model Training ===")
            Log.d("PHBD_TRAINING", "User ID: $userId")

            // ✅ CRITICAL: Get baseline data ONLY from baselineDao (NOT healthDataRepository)
            // This ensures we send the pre-aggregated baseline metrics, not raw health data
            val baselineRecords = baselineDao.getBaselineData(userId).first()
            Log.d("PHBD_TRAINING", "Retrieved ${baselineRecords.size} baseline records from DAO")

            if (baselineRecords.isEmpty()) {
                Log.e("PHBD_TRAINING", "❌ No baseline data available for training")
                return@withContext Result.failure(Exception("No baseline data available"))
            }

            if (baselineRecords.size < MINIMUM_BASELINE_DAYS) {
                Log.e("PHBD_TRAINING", "❌ Insufficient baseline data: ${baselineRecords.size} days (need $MINIMUM_BASELINE_DAYS)")
                return@withContext Result.failure(Exception("Insufficient baseline data"))
            }

            Log.d("PHBD_TRAINING", "📊 Preparing ${baselineRecords.size} baseline days for training")
            Log.d("PHBD_TRAINING", "   Date range: ${baselineRecords.last().date} to ${baselineRecords.first().date}")

            // Convert Room entities to API format (using baseline data ONLY)
            val baselineDays = baselineRecords.map { record ->
                BaselineDay(
                    avg_heart_rate = record.avg_heart_rate,
                    resting_heart_rate = record.resting_heart_rate,
                    hr_variance = record.hr_variance,
                    total_sleep_minutes = record.total_sleep_minutes,
                    steps_total = record.steps_total,
                    calories_burned = record.calories_burned,
                    sedentary_ratio = record.sedentary_ratio,
                    movement_variance = record.movement_variance,
                    activity_load_index = record.activity_load_index,
                    sleep_consistency = record.sleep_consistency
                )
            }

            // Build training request
            val request = TrainBaselineRequest(
                user_id = userId,
                baseline_data = baselineDays
            )

            Log.d("PHBD_TRAINING", "📤 Sending baseline to backend for training")
            Log.d("PHBD_TRAINING", "   Days included: ${baselineDays.size}")
            Log.d("PHBD_TRAINING", "   Sample (first day): HR=${baselineDays.first().avg_heart_rate}, Steps=${baselineDays.first().steps_total}")

            // Call training endpoint
            val response = api.trainBaseline(request)

            Log.d("PHBD_TRAINING", "📥 Received response: $response")

            // ✅ CRITICAL: Only set trained flag if backend confirms success
            if (response.status == "trained" && response.baseline_updated) {
                Log.d("PHBD_TRAINING", "✅ Baseline training successful!")

                // Save training status to SharedPreferences ONLY on confirmed success
                trainingPreferences.setModelTrained(userId, true)
                trainingPreferences.setLastTrainingDate(userId, System.currentTimeMillis())

                Log.d("PHBD_TRAINING", "💾 Training status saved to preferences")
                Log.d("PHBD_TRAINING", "   modelTrained = true")
                Log.d("PHBD_TRAINING", "   lastTrainingDate = ${System.currentTimeMillis()}")

                Result.success(response)
            } else {
                // Training failed - do NOT set trained flag
                Log.e("PHBD_TRAINING", "❌ Training failed: status=${response.status}, updated=${response.baseline_updated}")
                Log.e("PHBD_TRAINING", "   Training status NOT saved (modelTrained remains false)")
                Result.failure(Exception("Training failed: ${response.status}"))
            }

        } catch (e: SocketTimeoutException) {
            Log.e("PHBD_TRAINING", "⏱️ Timeout during training: ${e.message}", e)
            Log.e("PHBD_TRAINING", "   Training status NOT saved (timeout)")
            Result.failure(Exception("Training request timeout. Please try again."))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("PHBD_TRAINING", "🌐 HTTP error during training ${e.code()}: $errorBody", e)
            Log.e("PHBD_TRAINING", "   Training status NOT saved (HTTP error)")
            Result.failure(Exception("Server error during training: ${e.code()}"))
        } catch (e: Exception) {
            Log.e("PHBD_TRAINING", "❌ Unexpected error during training: ${e.javaClass.simpleName} - ${e.message}", e)
            Log.e("PHBD_TRAINING", "   Training status NOT saved (exception)")
            Result.failure(e)
        }
    }

    /**
     * Helper functions to compute derived metrics from health data
     */

    private fun dayKey(timestamp: Long): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }

    private fun computeAvgHeartRate(todayHeartRates: List<Float>, recentData: List<HealthData>): Float {
        // Prefer today's average if available
        return if (todayHeartRates.isNotEmpty()) {
            todayHeartRates.average().toFloat()
        } else {
            // Fallback to recent 7-day average
            val recentHeartRates = recentData.mapNotNull { it.heartRate }
            if (recentHeartRates.isNotEmpty()) {
                recentHeartRates.average().toFloat()
            } else {
                70f  // Default fallback
            }
        }
    }

    private fun computeRestingHeartRate(recentData: List<HealthData>, avgHeartRate: Float): Float {
        /**
         * Resting heart rate represents the lowest stable HR during rest periods.
         *
         * Physiological reasoning:
         * - True resting HR occurs during sleep or prolonged inactivity
         * - Google Fit doesn't explicitly provide "resting HR", so we derive it
         * - We filter HR samples where user is likely at rest (sleep or no movement)
         * - Taking the lowest 10-15% percentile averages out measurement noise
         *
         * Strategy:
         * 1. Collect HR samples during sleep periods (sleepDuration > 0)
         * 2. OR collect HR samples where steps == 0 (sedentary periods)
         * 3. Sort and take lowest 15th percentile
         * 4. Average that subset for stable resting HR estimate
         */

        // Collect HR during rest periods (sleep or zero steps)
        val restingHeartRates = recentData.mapNotNull { data ->
            // Include HR if during sleep OR during sedentary period
            val isDuringSleep = data.sleepDuration != null && data.sleepDuration > 0
            val isLowActivity = data.steps == null || data.steps == 0

            if ((isDuringSleep || isLowActivity) && data.heartRate != null) {
                data.heartRate
            } else {
                null
            }
        }

        return if (restingHeartRates.isNotEmpty()) {
            // Sort HR values to find lowest stable readings
            val sorted = restingHeartRates.sorted()

            // Take lowest 15% percentile to filter out measurement noise
            val percentileCount = maxOf(1, (sorted.size * 0.15).toInt())
            val lowestPercentile = sorted.take(percentileCount)

            // Average the lowest stable readings
            val restingHR = lowestPercentile.average().toFloat()

            Log.d("HealthDeviationRepo", "Computed resting HR from ${restingHeartRates.size} rest samples, lowest 15% avg: $restingHR")
            restingHR
        } else {
            // Fallback: Resting HR is typically 10-15 bpm lower than average
            // Use 85% of average HR as physiologically reasonable estimate
            val estimate = avgHeartRate * 0.85f
            Log.d("HealthDeviationRepo", "No rest HR samples, estimating resting HR as 85% of avg: $estimate")
            estimate
        }
    }

    private fun computeHeartRateVariance(recentData: List<HealthData>): Float {
        val heartRates = recentData.mapNotNull { it.heartRate }
        if (heartRates.size < 2) return 5f  // Default variance

        val mean = heartRates.average()
        val variance = heartRates.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun computeTodaySleepMinutes(todayData: List<HealthData>, todayStart: Long): Int {
        // Calculate sleep overlapping with today (matching MainViewModel logic)
        val todayEnd = todayStart + 24 * 60 * 60 * 1000
        val sleepRecords = todayData.filter { it.sleepDuration != null && it.sleepDuration > 0 }

        var totalSleepMinutes = 0L
        for (record in sleepRecords) {
            val sleepStart = record.timestamp
            val sleepEnd = sleepStart + (record.sleepDuration!! * 60 * 1000)

            // Calculate overlap with today
            val overlapStart = maxOf(sleepStart, todayStart)
            val overlapEnd = minOf(sleepEnd, todayEnd)

            if (overlapEnd > overlapStart) {
                val overlapMinutes = (overlapEnd - overlapStart) / (60 * 1000)
                totalSleepMinutes += overlapMinutes
            }
        }

        return if (totalSleepMinutes > 0) totalSleepMinutes.toInt() else 420  // Default 7 hours
    }

    private fun computeEstimatedCalories(steps: Int): Float {
        // Rough estimate: ~0.04 calories per step
        return steps * 0.04f
    }

    private fun computeSedentaryRatio(moveMinutes: Int, steps: Int): Float {
        // Sedentary ratio = proportion of time with low activity
        val totalMinutes = 24 * 60f  // Total minutes in a day

        return if (moveMinutes > 0) {
            1f - (moveMinutes.toFloat() / totalMinutes)
        } else {
            // Fallback based on steps
            if (steps < 1000) 0.9f else if (steps < 5000) 0.7f else 0.5f
        }
    }

    private fun computeMovementVariance(recentData: List<HealthData>): Float {
        // Group by day and sum steps per day
        val dailySteps = recentData.groupBy { dayKey(it.timestamp) }
            .mapValues { it.value.sumOf { d -> d.steps ?: 0 }.toFloat() }
            .values.toList()

        if (dailySteps.size < 2) return 1000f  // Default variance

        val mean = dailySteps.average()
        val variance = dailySteps.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun computeActivityLoadIndex(steps: Int, calories: Float): Float {
        // Normalize activity load: combines steps and calories
        val stepScore = (steps / 10000f).coerceIn(0f, 1f)
        val calorieScore = (calories / 2500f).coerceIn(0f, 1f)
        return (stepScore + calorieScore) / 2f
    }

    private fun computeSleepVariance(recentData: List<HealthData>): Float {
        /**
         * Sleep Variance: Measures inconsistency in sleep patterns
         *
         * Lower value = More consistent sleep schedule (better)
         * Higher value = Irregular sleep pattern (worse)
         *
         * Computed as normalized standard deviation of daily sleep duration
         * over the recent 7-day period.
         *
         * This metric helps identify sleep schedule disruptions which can
         * contribute to health deviation and stress.
         */

        // Group by day and calculate total sleep per day
        val dailySleep = recentData.groupBy { dayKey(it.timestamp) }
            .mapValues {
                it.value.filter { d -> d.sleepDuration != null && d.sleepDuration > 0 }
                    .sumOf { d -> d.sleepDuration ?: 0L }.toFloat()
            }
            .values.filter { it > 0f }.toList()

        if (dailySleep.size < 2) {
            // Not enough data - return moderate variance
            return 0.5f
        }

        val mean = dailySleep.average().toFloat()
        val variance = dailySleep.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance)

        // Normalize by mean to get coefficient of variation
        // This makes variance comparable across different average sleep durations
        val normalizedVariance = (stdDev / mean).coerceIn(0f, 1f)

        Log.d("HealthDeviationRepo", "Sleep variance computed: $normalizedVariance (${dailySleep.size} days)")
        return normalizedVariance
    }
}



