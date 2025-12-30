package com.tharun.vitalsync.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalsync.data.AppDatabase
import com.tharun.vitalsync.data.HealthData
import com.tharun.vitalsync.data.HealthDataRepository
import com.tharun.vitalsync.health.GoogleFitManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val healthDataDao = AppDatabase.getDatabase(application).healthDataDao()
    private val googleFitManager = GoogleFitManager(application)
    private val repository = HealthDataRepository(healthDataDao, googleFitManager)

    private val _userId = MutableStateFlow<String?>(null)
    private val _userName = MutableStateFlow("User")

    private val _historyState = MutableStateFlow<List<HealthData>>(emptyList())
    val historyState: StateFlow<List<HealthData>> = _historyState

    val state: StateFlow<DashboardState> = _userId.flatMapLatest { userId ->
        if (userId != null) {
            repository.getHealthData(userId).map { data ->
                val today = data.firstOrNull()
                val weeklySteps = getWeeklyData(data, "E") { it.steps?.toFloat() ?: 0f }
                val weeklyCalories = getWeeklyData(data, "E") { it.calories ?: 0f }

                DashboardState(
                    userName = _userName.value,
                    heartRate = today?.heartRate?.toString() ?: "--",
                    calories = today?.calories?.let { String.format("%.0f", it) } ?: "--",
                    steps = today?.steps?.toString() ?: "--",
                    distance = today?.distance?.let { String.format("%.2f", it) } ?: "--",
                    heartPoints = "0", // Disabled
                    sleepDuration = today?.sleepDuration?.let { "${it / 60}h ${it % 60}m" } ?: "--",
                    lastActivity = today?.activityType ?: "None",
                    lastActivityTime = today?.timestamp?.let { SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(it)) } ?: "",
                    weeklySteps = weeklySteps,
                    weeklyCalories = weeklyCalories,
                    weeklyHeartPoints = emptyList() // Disabled
                )
            }
        } else {
            flowOf(DashboardState())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    fun setUserIdAndName(userId: String, userName: String?) {
        _userId.value = userId
        _userName.value = userName?.split(" ")?.first() ?: "User"
    }

    fun syncData() {
        _userId.value?.let {
            viewModelScope.launch {
                repository.syncData(it)
            }
        }
    }

    fun loadHistory(metricType: MetricType, timeRange: String) {
        _userId.value?.let { userId ->
            val now = System.currentTimeMillis()
            val startTime = when (timeRange) {
                "Today" -> now - TimeUnit.DAYS.toMillis(1)
                "7 Days" -> now - TimeUnit.DAYS.toMillis(7)
                "30 Days" -> now - TimeUnit.DAYS.toMillis(30)
                else -> now - TimeUnit.DAYS.toMillis(1)
            }

            // Observe the database for changes
            viewModelScope.launch {
                repository.getHealthDataForRange(userId, startTime, now).collect { databaseData ->
                    val filteredData = databaseData.filter {
                        when (metricType) {
                            MetricType.HEART_RATE -> it.heartRate != null
                            MetricType.STEPS -> it.steps != null
                            MetricType.CALORIES -> it.calories != null
                            MetricType.DISTANCE -> it.distance != null
                            MetricType.SLEEP -> it.sleepDuration != null
                            MetricType.HEART_POINTS -> false // Heart points are disabled
                        }
                    }
                    _historyState.value = filteredData
                }
            }

            // Trigger a background sync from the network
            viewModelScope.launch {
                try {
                    repository.syncHistoricalData(userId, startTime, now, metricType)
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to sync history for $metricType", e)
                }
            }
        }
    }

    private fun getWeeklyData(data: List<HealthData>, format: String, valueSelector: (HealthData) -> Float): List<Pair<String, Float>> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)

        return data.filter { 
            cal.timeInMillis = it.timestamp
            cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.DAY_OF_YEAR) > today - 7
        }.groupBy {
            cal.timeInMillis = it.timestamp
            SimpleDateFormat(format, Locale.getDefault()).format(cal.time)
        }.mapValues { entry ->
            entry.value.maxOf(valueSelector)
        }.map { it.key to it.value }
    }
}
