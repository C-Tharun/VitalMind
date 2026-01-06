@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.tharun.vitalmind.data

import android.util.Log
import com.tharun.vitalmind.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class WeatherApiResponse(
    val location: Location,
    val current: Current
) {
    @Serializable
    data class Location(val name: String)
    @Serializable
    data class Current(
        val temp_c: Float,
        val condition: Condition,
        @SerialName("air_quality") val airQuality: AirQuality? = null
    ) {
        @Serializable
        data class Condition(val text: String)
        @Serializable
        data class AirQuality(
            @SerialName("us-epa-index") val usEpaIndex: Int? = null,
            @SerialName("pm2_5") val pm25: Float? = null
        )
    }
}

object WeatherApiService {
    private var cachedWeather: Pair<String, WeatherApiResponse>? = null

    suspend fun getTodayWeather(city: String = "auto:ip"): WeatherApiResponse? {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (cachedWeather?.first == today) return cachedWeather?.second
        val apiKey = BuildConfig.WEATHER_API_KEY
        if (apiKey.isNullOrBlank()) return null
        val client = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return try {
            val response: WeatherApiResponse = client.get("https://api.weatherapi.com/v1/current.json") {
                parameter("key", apiKey)
                parameter("q", city)
                parameter("aqi", "yes")
            }.body()
            cachedWeather = today to response
            response
        } catch (e: Exception) {
            Log.e("WeatherApiService", "Weather API error", e)
            null
        } finally {
            client.close()
        }
    }
}
