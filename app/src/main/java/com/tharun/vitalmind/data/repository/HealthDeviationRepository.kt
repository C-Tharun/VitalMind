package com.tharun.vitalmind.data.repository

import android.util.Log
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

    private val api by lazy { retrofit.create(HealthDeviationApiService::class.java) }

    /**
     * Analyzes health deviation based on current health data
     * Returns Result.success or Result.failure
     */
    suspend fun getHealthDeviation(): Result<HealthDeviationResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("HealthDeviationRepo", "=== Starting Health Deviation Analysis ===")
            Log.d("HealthDeviationRepo", "User ID: $userId")

            // Get the latest HealthData for the user
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            Log.d("HealthDeviationRepo", "Retrieved ${healthDataList.size} health data records")

            val latest = healthDataList.maxByOrNull { it.timestamp }

            if (latest == null) {
                Log.e("HealthDeviationRepo", "❌ No synced health data available for user $userId")
                return@withContext Result.failure(Exception("No health data available"))
            }

            Log.d("HealthDeviationRepo", "Latest health data timestamp: ${latest.timestamp}")
            Log.d("HealthDeviationRepo", "Health data: HR=${latest.heartRate}, Steps=${latest.steps}, Cal=${latest.calories}, Sleep=${latest.sleepDuration}")

            val now = java.util.Calendar.getInstance()
            val request = HealthDeviationRequest(
                avg_heart_rate = latest.heartRate ?: 0f,
                max_heart_rate = latest.heartRate ?: 0f,
                steps_total = latest.steps ?: 0,
                calories_burned = latest.calories ?: 0f,
                distance_total = latest.distance ?: 0f,
                total_sleep_minutes = (latest.sleepDuration ?: 0L).toInt(),
                activity = latest.activityType ?: "unknown",
                hour_of_day = now.get(java.util.Calendar.HOUR_OF_DAY),
                is_sedentary = if ((latest.steps ?: 0) < 1000) 1 else 0
            )

            Log.d("HealthDeviationRepo", "📤 Sending request to API: $request")

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
}


