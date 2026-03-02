package com.tharun.vitalmind.data.repository

import android.util.Log
import com.tharun.vitalmind.data.HealthDataRepository
import com.tharun.vitalmind.data.StressScoreHistory
import com.tharun.vitalmind.data.StressScoreHistoryDao
import com.tharun.vitalmind.data.remote.StressRequest
import com.tharun.vitalmind.data.remote.StressResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import retrofit2.HttpException

// Retrofit API service for backend
interface RealStressApiService {
    @POST("stress_analysis")
    suspend fun calculateStress(@Body request: StressRequest): StressResponse
}

class StressRepository(
    private val healthDataRepository: HealthDataRepository,
    private val userId: String,
    private val stressScoreHistoryDao: StressScoreHistoryDao
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://vitalmind-stress-api.onrender.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val realApi by lazy { retrofit.create(RealStressApiService::class.java) }

    suspend fun calculateStressScore(latitude: Double? = null, longitude: Double? = null): StressResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("StressRepository", "=== Starting Stress Analysis ===")
            Log.d("StressRepository", "User ID: $userId")
            if (latitude != null && longitude != null) {
                Log.d("StressRepository", "📍 Location: lat=$latitude, lng=$longitude")
            }

            // Get all health data for the user
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            Log.d("StressRepository", "Retrieved ${healthDataList.size} health data records")

            if (healthDataList.isEmpty()) {
                Log.e("StressRepository", "No synced health data available for user $userId")
                return@withContext StressResponse(
                    stress_level = "Error",
                    stress_score = -1f,
                    stress_status = "Unavailable",
                    stress_stability = "Unknown",
                    mood = "Unknown"
                )
            }

            // Get TODAY's data (matching MainViewModel and HealthDeviationRepository logic)
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000

            val todayData = healthDataList.filter { it.timestamp >= todayStart && it.timestamp < todayEnd }
            Log.d("StressRepository", "Today's data records: ${todayData.size}")

            // Aggregate TODAY's data (like Dashboard does)
            val todaySteps = todayData.sumOf { it.steps ?: 0 }
            val todayCalories = todayData.sumOf { it.calories?.toDouble() ?: 0.0 }.toFloat()
            val todayDistance = todayData.sumOf { it.distance?.toDouble() ?: 0.0 }.toFloat()
            val todayHeartRates = todayData.mapNotNull { it.heartRate }

            // Calculate sleep overlapping with today
            var totalSleepMinutes = 0L
            val sleepRecords = todayData.filter { it.sleepDuration != null && it.sleepDuration > 0 }
            for (record in sleepRecords) {
                val sleepStart = record.timestamp
                val sleepEnd = sleepStart + (record.sleepDuration!! * 60 * 1000)
                val overlapStart = maxOf(sleepStart, todayStart)
                val overlapEnd = minOf(sleepEnd, todayEnd)
                if (overlapEnd > overlapStart) {
                    totalSleepMinutes += (overlapEnd - overlapStart) / (60 * 1000)
                }
            }

            // Get most recent activity
            val latestActivity = todayData.filter { it.activityType != null }
                .maxByOrNull { it.timestamp }?.activityType

            // Compute average and max heart rate from TODAY's data
            val avgHeartRate = if (todayHeartRates.isNotEmpty()) {
                todayHeartRates.average().toFloat()
            } else {
                // Fallback to recent 7-day data
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recentData = healthDataList.filter { it.timestamp >= sevenDaysAgo }
                val recentHR = recentData.mapNotNull { it.heartRate }
                if (recentHR.isNotEmpty()) recentHR.average().toFloat() else 70f
            }

            val maxHeartRate = if (todayHeartRates.isNotEmpty()) {
                todayHeartRates.maxOrNull() ?: avgHeartRate
            } else {
                avgHeartRate
            }

            // Map activity to valid backend types
            // Backend typically accepts: still, walking, running, biking, in_vehicle, etc.
            // If no activity or "unknown", default to "still" (represents low/no activity)
            val validActivity = when {
                latestActivity.isNullOrBlank() -> "still"
                latestActivity.equals("unknown", ignoreCase = true) -> "still"
                else -> latestActivity.lowercase()
            }

            Log.d("StressRepository", "TODAY's aggregated data:")
            Log.d("StressRepository", "  Steps: $todaySteps")
            Log.d("StressRepository", "  Calories: $todayCalories")
            Log.d("StressRepository", "  Distance: $todayDistance")
            Log.d("StressRepository", "  Sleep: $totalSleepMinutes min")
            Log.d("StressRepository", "  Avg HR: $avgHeartRate")
            Log.d("StressRepository", "  Max HR: $maxHeartRate")
            Log.d("StressRepository", "  Activity: $validActivity (original: $latestActivity)")

            val now = java.util.Calendar.getInstance()
            val request = StressRequest(
                avg_heart_rate = avgHeartRate,
                max_heart_rate = maxHeartRate,
                steps_total = todaySteps,
                calories_total = todayCalories,
                distance_total = todayDistance,
                sleep_minutes = totalSleepMinutes.toInt(),
                activity = validActivity,
                hour_of_day = now.get(java.util.Calendar.HOUR_OF_DAY),
                is_sedentary = if (todaySteps < 1000) 1 else 0,
                recent_stress_scores = null // You can implement history if needed
            )

            Log.d("StressRepository", "📤 Sending stress request: $request")
            val response = realApi.calculateStress(request)
            Log.d("StressRepository", "📥 Received response: $response")

            // Persist to DB if successful
            val history = StressScoreHistory(
                userId = userId,
                timestamp = System.currentTimeMillis(),
                stress_score = response.stress_score,
                stress_level = response.stress_level,
                stress_status = response.stress_status,
                stress_stability = response.stress_stability,
                mood = response.mood,
                request_json = Gson().toJson(request),
                latitude = latitude,
                longitude = longitude
            )
            stressScoreHistoryDao.insert(history)
            Log.d("StressRepository", "💾 Saved stress score with location: lat=$latitude, lng=$longitude")
            response
        } catch (e: SocketTimeoutException) {
            Log.e("StressRepository", "Timeout while calculating stress score: ${e.message}", e)
            StressResponse(
                stress_level = "Error",
                stress_score = -1f,
                stress_status = "Timeout",
                stress_stability = "Unknown",
                mood = "Unknown"
            )
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("StressRepository", "HTTP error: ${e.code()} - $errorBody", e)
            StressResponse(
                stress_level = "Error",
                stress_score = -1f,
                stress_status = "HTTP ${e.code()}",
                stress_stability = errorBody ?: "Unknown",
                mood = "Unknown"
            )
        } catch (e: Exception) {
            Log.e("StressRepository", "Error calculating stress score: ${e.message}", e)
            // Fallback: return a default error response or handle as needed
            StressResponse(
                stress_level = "Error",
                stress_score = -1f,
                stress_status = "Unavailable",
                stress_stability = "Unknown",
                mood = "Unknown"
            )
        }
    }

    fun getStressScoreHistory(userId: String): Flow<List<StressScoreHistory>> =
        stressScoreHistoryDao.getHistoryForUser(userId)
}
