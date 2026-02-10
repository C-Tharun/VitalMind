package com.tharun.vitalmind.data.repository

import android.util.Log
import com.tharun.vitalmind.data.HealthData
import com.tharun.vitalmind.data.HealthDataRepository
import com.tharun.vitalmind.data.remote.HealthDeviationRequest
import com.tharun.vitalmind.data.remote.HealthDeviationResponse
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
}

/**
 * Repository for Health Deviation (PHBD-Net) feature
 */
class HealthDeviationRepository(
    private val healthDataRepository: HealthDataRepository,
    private val userId: String
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)  // Increased for cold starts
        .readTimeout(60, TimeUnit.SECONDS)     // Increased for cold starts
        .writeTimeout(60, TimeUnit.SECONDS)    // Increased for cold starts
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
     * Analyzes health deviation based on current health data
     * Returns Result.success or Result.failure
     */
    suspend fun getHealthDeviation(): Result<HealthDeviationResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthDeviationRepo", "=== Starting Health Deviation Analysis ===")
            Log.d("HealthDeviationRepo", "User ID: $userId")

            // Get all health data for the user to compute derived metrics
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            Log.d("HealthDeviationRepo", "Retrieved ${healthDataList.size} health data records")

            if (healthDataList.isEmpty()) {
                Log.e("HealthDeviationRepo", "❌ No synced health data available for user $userId")
                return@withContext Result.failure(Exception("No health data available. Please sync with Google Fit first."))
            }

            // Get the latest record
            val latest = healthDataList.maxByOrNull { it.timestamp }!!
            Log.d("HealthDeviationRepo", "Latest health data timestamp: ${latest.timestamp}")

            // Get recent records for computing derived metrics (last 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val recentData = healthDataList.filter { it.timestamp >= sevenDaysAgo }
            Log.d("HealthDeviationRepo", "Recent data (7 days): ${recentData.size} records")

            // Compute derived metrics with fallbacks
            val avgHeartRate = computeAvgHeartRate(recentData, latest)
            val restingHeartRate = computeRestingHeartRate(recentData, avgHeartRate)
            val hrVariance = computeHeartRateVariance(recentData)
            val stepsTotal = latest.steps ?: 0
            val totalSleepMinutes = computeAvgSleepMinutes(recentData, latest)
            val caloriesBurned = latest.calories ?: computeEstimatedCalories(stepsTotal)
            val sedentaryRatio = computeSedentaryRatio(recentData, latest)
            val movementVariance = computeMovementVariance(recentData)
            val activityLoadIndex = computeActivityLoadIndex(stepsTotal, caloriesBurned)
            val sleepConsistency = computeSleepConsistency(recentData)

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
            Log.d("HealthDeviationRepo", "  sleep_consistency: $sleepConsistency")

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
                sleep_consistency = sleepConsistency,
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
            Result.failure(Exception("Request timeout. Please try again."))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("HealthDeviationRepo", "🌐 HTTP error ${e.code()}: $errorBody", e)
            Log.e("HealthDeviationRepo", "Response: ${e.response()}")
            Result.failure(Exception("Server error: ${e.code()}"))
        } catch (e: Exception) {
            Log.e("HealthDeviationRepo", "❌ Unexpected error: ${e.javaClass.simpleName} - ${e.message}", e)
            Log.e("HealthDeviationRepo", "Stack trace:", e)
            Result.failure(e)
        }
    }

    /**
     * Helper functions to compute derived metrics from health data
     */

    private fun computeAvgHeartRate(recentData: List<HealthData>, latest: HealthData): Float {
        val heartRates = recentData.mapNotNull { it.heartRate }
        return if (heartRates.isNotEmpty()) {
            heartRates.average().toFloat()
        } else {
            latest.heartRate ?: 70f  // Default fallback
        }
    }

    private fun computeRestingHeartRate(recentData: List<HealthData>, avgHeartRate: Float): Float {
        // Resting heart rate is typically lower than average
        // Estimate as 85% of average heart rate
        val heartRates = recentData.mapNotNull { it.heartRate }
        return if (heartRates.isNotEmpty()) {
            heartRates.minOrNull() ?: (avgHeartRate * 0.85f)
        } else {
            avgHeartRate * 0.85f  // Estimate
        }
    }

    private fun computeHeartRateVariance(recentData: List<HealthData>): Float {
        val heartRates = recentData.mapNotNull { it.heartRate }
        if (heartRates.size < 2) return 5f  // Default variance

        val mean = heartRates.average()
        val variance = heartRates.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun computeAvgSleepMinutes(recentData: List<HealthData>, latest: HealthData): Int {
        val sleepData = recentData.mapNotNull { it.sleepDuration }
        return if (sleepData.isNotEmpty()) {
            sleepData.average().toInt()
        } else {
            latest.sleepDuration?.toInt() ?: 420  // Default 7 hours
        }
    }

    private fun computeEstimatedCalories(steps: Int): Float {
        // Rough estimate: ~0.04 calories per step
        return steps * 0.04f
    }

    private fun computeSedentaryRatio(recentData: List<HealthData>, latest: HealthData): Float {
        // Sedentary ratio = proportion of time with low activity
        val activeMinutes = recentData.mapNotNull { it.moveMinutes }.average()
        val totalMinutes = 24 * 60f  // Total minutes in a day

        return if (activeMinutes > 0) {
            1f - (activeMinutes.toFloat() / totalMinutes)
        } else {
            val steps = latest.steps ?: 0
            if (steps < 1000) 0.9f else if (steps < 5000) 0.7f else 0.5f
        }
    }

    private fun computeMovementVariance(recentData: List<HealthData>): Float {
        val steps = recentData.mapNotNull { it.steps?.toFloat() }
        if (steps.size < 2) return 1000f  // Default variance

        val mean = steps.average()
        val variance = steps.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun computeActivityLoadIndex(steps: Int, calories: Float): Float {
        // Normalize activity load: combines steps and calories
        val stepScore = (steps / 10000f).coerceIn(0f, 1f)
        val calorieScore = (calories / 2500f).coerceIn(0f, 1f)
        return (stepScore + calorieScore) / 2f
    }

    private fun computeSleepConsistency(recentData: List<HealthData>): Float {
        val sleepData = recentData.mapNotNull { it.sleepDuration?.toFloat() }
        if (sleepData.size < 2) return 0.5f  // Moderate consistency as default

        val mean = sleepData.average().toFloat()
        val variance = sleepData.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = sqrt(variance)

        // Lower variance = higher consistency
        // Normalize: high consistency = close to 1.0
        val ratio = (stdDev / mean).coerceIn(0f, 1f)
        return (1f - ratio).coerceIn(0f, 1f)
    }
}



