package com.tharun.vitalmind.data.repository

import android.util.Log
import com.tharun.vitalmind.data.HealthDataRepository
import com.tharun.vitalmind.data.remote.StressRequest
import com.tharun.vitalmind.data.remote.StressResponse
import kotlinx.coroutines.Dispatchers
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
    @POST("/stress_analysis")
    suspend fun calculateStress(@Body request: StressRequest): StressResponse
}

class StressRepository(
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
    private val realApi by lazy { retrofit.create(RealStressApiService::class.java) }

    suspend fun calculateStressScore(): StressResponse = withContext(Dispatchers.IO) {
        try {
            // Get the latest HealthData for the user
            val healthDataList = healthDataRepository.getHealthData(userId).first()
            val latest = healthDataList.maxByOrNull { it.timestamp }
            if (latest == null) {
                Log.e("StressRepository", "No synced health data available for user $userId")
                return@withContext StressResponse(
                    stress_level = "Error",
                    stress_score = -1f,
                    stress_status = "Unavailable",
                    stress_stability = "Unknown",
                    mood = "Unknown"
                )
            }
            val now = java.util.Calendar.getInstance()
            val request = StressRequest(
                avg_heart_rate = latest.heartRate ?: 0f,
                max_heart_rate = latest.heartRate ?: 0f, // If you have max, use it
                steps_total = latest.steps ?: 0,
                calories_total = latest.calories ?: 0f,
                distance_total = latest.distance ?: 0f,
                sleep_minutes = (latest.sleepDuration ?: 0L).toInt(),
                activity = latest.activityType ?: "unknown",
                hour_of_day = now.get(java.util.Calendar.HOUR_OF_DAY),
                is_sedentary = if ((latest.steps ?: 0) < 1000) 1 else 0,
                recent_stress_scores = null // You can implement history if needed
            )
            Log.d("StressRepository", "Sending stress request: $request")
            realApi.calculateStress(request)
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
}
