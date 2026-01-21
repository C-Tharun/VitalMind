package com.tharun.vitalmind.data.repository

import android.util.Log
import com.tharun.vitalmind.data.remote.StressApiService
import com.tharun.vitalmind.data.remote.StressRequest
import com.tharun.vitalmind.data.remote.StressResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit API service for backend
interface RealStressApiService {
    @POST("/stress_analysis")
    suspend fun calculateStress(@Body request: StressRequest): StressResponse
}

class StressRepository(private val apiService: StressApiService? = null) {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://vitalmind-stress-api.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val realApi by lazy { retrofit.create(RealStressApiService::class.java) }

    suspend fun calculateStressScore(): StressResponse = withContext(Dispatchers.IO) {
        // TODO: Pull real data from repositories (heart rate, steps, etc.)
        val request = StressRequest(
            avg_heart_rate = 72.0f,
            max_heart_rate = 120.0f,
            steps_total = 5000,
            calories_total = 2200.0f,
            distance_total = 3.5f,
            sleep_minutes = 420,
            activity = "walking",
            hour_of_day = 14,
            is_sedentary = 0,
            recent_stress_scores = listOf(0.5f, 0.6f, 0.7f)
        )
        try {
            // Call the real backend API
            realApi.calculateStress(request)
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
