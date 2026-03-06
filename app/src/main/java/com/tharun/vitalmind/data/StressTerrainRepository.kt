package com.tharun.vitalmind.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.*
import kotlin.math.*

/**
 * Data class representing a stress event at a specific location
 */
data class StressEvent(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val heartRate: Float,
    val baselineHeartRate: Float,
    val activityType: String?,
    val stressIntensity: Float // normalized 0-1
)

/**
 * Data class representing aggregated stress data for a location cluster
 */
data class StressCluster(
    val latitude: Double,
    val longitude: Double,
    val weight: Float, // 0-1 representing stress intensity
    val eventCount: Int,
    val isCalmingZone: Boolean = false,
    // Additional detailed information for UI display
    val averageStressScore: Float = 0f,
    val stressLevel: String = "",
    val mood: String = "",
    val firstOccurrence: Long = 0L,
    val lastOccurrence: Long = 0L,
    val occurrenceDates: List<String> = emptyList()
)

/**
 * Repository for stress terrain analysis.
 * Uses rule-based logic to detect stress events and aggregate them spatially.
 */
class StressTerrainRepository(
    private val healthDataDao: HealthDataDao,
    private val stressScoreHistoryDao: StressScoreHistoryDao
) {

    companion object {
        // Thresholds for stress detection
        private const val STRESS_THRESHOLD_BPM = 20f // Heart rate deviation above baseline to trigger stress
        private const val GRID_CELL_SIZE_METERS = 500f // ~500m grid cells for clustering
        private const val CALMING_ZONE_THRESHOLD_BPM = -15f // Heart rate drop below baseline
        private const val MIN_ACTIVITY_INTENSITY = 3 // Exclude very vigorous activities from stress detection
    }

    /**
     * Calculates baseline heart rate for each activity type within a time-of-day window.
     * Returns a map of (activityType + timeWindow) -> baselineHeartRate
     */
    private fun calculateBaselineHeartRates(
        healthData: List<HealthData>
    ): Map<String, Float> {
        val baselineMap = mutableMapOf<String, Float>()

        // Group data by activity type and time-of-day window
        val groupedData = healthData.groupBy { data ->
            val cal = Calendar.getInstance().apply { timeInMillis = data.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val timeWindow = (hour / 4) // 6 windows per day (4-hour windows)
            val activity = data.activityType ?: "REST"
            "$activity:$timeWindow"
        }

        // Calculate average heart rate for each group
        for ((key, dataList) in groupedData) {
            val heartRateValues = dataList.mapNotNull { it.heartRate }.filter { it > 0 }
            if (heartRateValues.isNotEmpty()) {
                baselineMap[key] = heartRateValues.average().toFloat()
            }
        }

        return baselineMap
    }

    /**
     * Detects stress events from health data using rule-based logic
     */
    private fun detectStressEvents(
        healthData: List<HealthData>,
        baselineHeartRates: Map<String, Float>
    ): List<StressEvent> {
        val stressEvents = mutableListOf<StressEvent>()

        for (data in healthData) {
            val heartRate = data.heartRate ?: continue
            if (heartRate <= 0) continue

            val latitude = data.latitude ?: continue
            val longitude = data.longitude ?: continue
            val activityType = data.activityType ?: "REST"

            // Get time-of-day window
            val cal = Calendar.getInstance().apply { timeInMillis = data.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val timeWindow = (hour / 4)
            val baselineKey = "$activityType:$timeWindow"

            // Get baseline for this activity + time window
            val baseline = baselineHeartRates[baselineKey] ?: 70f // Default baseline

            // Detect stress: heart rate exceeds baseline by threshold AND activity is not vigorous
            val deviation = heartRate - baseline
            val isNotVigorousActivity = !isVigorousActivity(activityType)

            if (deviation > STRESS_THRESHOLD_BPM && isNotVigorousActivity) {
                val stressIntensity = (deviation / (baseline * 0.5f)).coerceIn(0f, 1f)
                stressEvents.add(
                    StressEvent(
                        timestamp = data.timestamp,
                        latitude = latitude,
                        longitude = longitude,
                        heartRate = heartRate,
                        baselineHeartRate = baseline,
                        activityType = activityType,
                        stressIntensity = stressIntensity
                    )
                )
            }
        }

        return stressEvents
    }

    /**
     * Identifies calming zones where heart rate consistently drops below baseline
     */
    private fun identifyCalmingZones(
        healthData: List<HealthData>,
        baselineHeartRates: Map<String, Float>
    ): List<StressEvent> {
        val calmingEvents = mutableListOf<StressEvent>()

        // Group data by location (simplified using lat/lng rounding)
        val locationGroups = healthData.filter {
            it.latitude != null && it.longitude != null
        }.groupBy { data ->
            val lat = data.latitude!! // Now safe because we filtered above
            val lng = data.longitude!! // Now safe because we filtered above
            val roundedLat = (lat * 100).toInt() / 100.0
            val roundedLng = (lng * 100).toInt() / 100.0
            "$roundedLat:$roundedLng"
        }

        for ((_, dataList) in locationGroups) {
            if (dataList.isEmpty()) continue

            val heartRateValues = dataList.mapNotNull { it.heartRate }.filter { it > 0 }
            if (heartRateValues.isEmpty()) continue

            val avgHeartRate = heartRateValues.average().toFloat() // Convert Double to Float
            val firstData = dataList.first()
            val activityType = firstData.activityType ?: "REST"
            val cal = Calendar.getInstance().apply { timeInMillis = firstData.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val timeWindow = (hour / 4)
            val baselineKey = "$activityType:$timeWindow"
            val baseline = baselineHeartRates[baselineKey] ?: 70f

            // Calming zone: average heart rate drops below baseline
            if ((baseline - avgHeartRate) > CALMING_ZONE_THRESHOLD_BPM) {
                val calmingIntensity = ((baseline - avgHeartRate) / baseline).coerceIn(0f, 1f)
                calmingEvents.add(
                    StressEvent(
                        timestamp = firstData.timestamp,
                        latitude = firstData.latitude ?: 0.0,
                        longitude = firstData.longitude ?: 0.0,
                        heartRate = avgHeartRate,
                        baselineHeartRate = baseline,
                        activityType = activityType,
                        stressIntensity = calmingIntensity
                    )
                )
            }
        }

        return calmingEvents
    }

    /**
     * Clusters stress events spatially using a grid-based approach
     */
    private fun clusterStressEvents(
        stressEvents: List<StressEvent>,
        isCalming: Boolean = false
    ): List<StressCluster> {
        if (stressEvents.isEmpty()) return emptyList()

        // Group events into grid cells
        val cellSize = GRID_CELL_SIZE_METERS / 111000.0 // Convert meters to degrees (~111km per degree)
        val clusteredMap = mutableMapOf<String, MutableList<StressEvent>>()

        for (event in stressEvents) {
            val cellLat = (event.latitude / cellSize).toInt() * cellSize
            val cellLng = (event.longitude / cellSize).toInt() * cellSize
            val cellKey = "$cellLat:$cellLng"
            clusteredMap.computeIfAbsent(cellKey) { mutableListOf() }.add(event)
        }

        // Aggregate data per cluster
        val clusters = mutableListOf<StressCluster>()
        for ((_, events) in clusteredMap) {
            val avgLat = events.map { it.latitude }.average()
            val avgLng = events.map { it.longitude }.average()
            val avgIntensity = events.map { it.stressIntensity }.average().toFloat()

            clusters.add(
                StressCluster(
                    latitude = avgLat,
                    longitude = avgLng,
                    weight = avgIntensity,
                    eventCount = events.size,
                    isCalmingZone = isCalming
                )
            )
        }

        return clusters.sortedByDescending { it.eventCount }
    }

    /**
     * Determines if an activity type is vigorous (exclude from stress detection)
     */
    private fun isVigorousActivity(activityType: String): Boolean {
        val vigorousActivities = setOf(
            "RUNNING",
            "CYCLING",
            "SPORTS",
            "HIKING",
            "WORKOUT",
            "EXERCISE"
        )
        return vigorousActivities.any { activityType.uppercase().contains(it) }
    }

    /**
     * Fetches stress terrain map data for a user over the last N days
     * Now also uses StressScoreHistory data with location information
     */
    fun getStressTerrainData(
        userId: String,
        dayCount: Int = 30
    ): Flow<Pair<List<StressCluster>, List<StressCluster>>> = flow {
        try {
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -dayCount)
            val startTime = cal.timeInMillis

            Log.d("StressTerrainRepository", "=== STRESS TERRAIN MAP DEBUG ===")
            Log.d("StressTerrainRepository", "User ID: $userId")
            Log.d("StressTerrainRepository", "Fetching stress terrain data for last $dayCount days")

            // First, get all stress score history - use first() to get current value without blocking
            val stressHistory = try {
                stressScoreHistoryDao.getHistoryForUser(userId).first()
            } catch (e: NoSuchElementException) {
                emptyList()
            }

            Log.d("StressTerrainRepository", "Total stress history entries: ${stressHistory.size}")

            // Filter to recent entries with location
            val recentHistory = stressHistory.filter {
                it.timestamp >= startTime &&
                it.timestamp <= endTime &&
                it.latitude != null &&
                it.longitude != null
            }

            Log.d("StressTerrainRepository", "Stress history with location in date range: ${recentHistory.size}")
            recentHistory.forEach { entry ->
                Log.d("StressTerrainRepository", "  - Score: ${entry.stress_score}, Lat: ${entry.latitude}, Lng: ${entry.longitude}, Time: ${entry.timestamp}")
            }

            if (recentHistory.isNotEmpty()) {
                Log.d("StressTerrainRepository", "Found ${recentHistory.size} stress scores with location data")

                // Convert stress score history to stress clusters
                val stressClusters = convertStressHistoryToClusters(recentHistory, isStressed = true)
                val calmingClusters = convertStressHistoryToClusters(recentHistory, isStressed = false)

                Log.d("StressTerrainRepository", "Created ${stressClusters.size} stress clusters and ${calmingClusters.size} calming clusters")

                emit(Pair(stressClusters, calmingClusters))
                return@flow
            }

            // Fallback: Try health data with location (original method)
            Log.d("StressTerrainRepository", "No stress history with location found, trying health data...")
            val healthData = try {
                healthDataDao.getDataForRange(userId, startTime, endTime).first()
            } catch (e: NoSuchElementException) {
                emptyList()
            }

            val locatedData = healthData.filter {
                it.latitude != null &&
                it.longitude != null &&
                it.heartRate != null &&
                it.heartRate!! > 0
            }

            if (locatedData.isEmpty()) {
                Log.d("StressTerrainRepository", "No health data with location information found")
                emit(Pair(emptyList(), emptyList()))
                return@flow
            }

            Log.d("StressTerrainRepository", "Processing ${locatedData.size} health data points with location")

            // Calculate baselines
            val baselineHeartRates = calculateBaselineHeartRates(locatedData)
            Log.d("StressTerrainRepository", "Calculated ${baselineHeartRates.size} baseline heart rate groups")

            // Detect stress events
            val stressEvents = detectStressEvents(locatedData, baselineHeartRates)
            Log.d("StressTerrainRepository", "Detected ${stressEvents.size} stress events")

            // Identify calming zones
            val calmingEvents = identifyCalmingZones(locatedData, baselineHeartRates)
            Log.d("StressTerrainRepository", "Identified ${calmingEvents.size} calming events")

            // Cluster spatial data
            val stressClusters = clusterStressEvents(stressEvents, isCalming = false)
            val calmingClusters = clusterStressEvents(calmingEvents, isCalming = true)

            Log.d("StressTerrainRepository", "Created ${stressClusters.size} stress clusters and ${calmingClusters.size} calming clusters")

            emit(Pair(stressClusters, calmingClusters))
        } catch (e: Exception) {
            Log.e("StressTerrainRepository", "Error getting stress terrain data", e)
            emit(Pair(emptyList(), emptyList()))
        }
    }

    /**
     * Converts StressScoreHistory entries with location to StressCluster objects
     * Uses mood field (Relaxed/Alert/Stressed) instead of stress_score (which is just ML confidence)
     */
    private fun convertStressHistoryToClusters(
        history: List<StressScoreHistory>,
        isStressed: Boolean
    ): List<StressCluster> {
        Log.d("StressTerrainRepository", "Converting ${history.size} history entries to clusters (isStressed=$isStressed)")

        val filteredHistory = if (isStressed) {
            // Stressed mood = High stress zones (mood == "Stressed" or "Alert")
            history.filter {
                it.mood.equals("Stressed", ignoreCase = true) ||
                it.mood.equals("Alert", ignoreCase = true)
            }
        } else {
            // Relaxed mood = Calming zones (mood == "Relaxed")
            history.filter { it.mood.equals("Relaxed", ignoreCase = true) }
        }

        Log.d("StressTerrainRepository", "Filtered to ${filteredHistory.size} entries based on mood (isStressed=$isStressed)")
        filteredHistory.forEach { entry ->
            Log.d("StressTerrainRepository", "  - Mood: ${entry.mood}, Level: ${entry.stress_level}, Lat: ${entry.latitude}, Lng: ${entry.longitude}")
        }

        if (filteredHistory.isEmpty()) {
            Log.d("StressTerrainRepository", "No ${if (isStressed) "stress" else "calming"} zones found")
            return emptyList()
        }

        // Group by location (grid-based clustering)
        val cellSize = GRID_CELL_SIZE_METERS / 111000.0 // Convert meters to degrees
        val clusteredMap = mutableMapOf<String, MutableList<StressScoreHistory>>()

        for (entry in filteredHistory) {
            val lat = entry.latitude ?: continue
            val lng = entry.longitude ?: continue
            val cellLat = (lat / cellSize).toInt() * cellSize
            val cellLng = (lng / cellSize).toInt() * cellSize
            val cellKey = "$cellLat:$cellLng"
            clusteredMap.computeIfAbsent(cellKey) { mutableListOf() }.add(entry)
        }

        Log.d("StressTerrainRepository", "Grouped into ${clusteredMap.size} spatial clusters")

        // Create clusters
        val clusters = clusteredMap.map { (_, entries) ->
            val avgLat = entries.mapNotNull { it.latitude }.average()
            val avgLng = entries.mapNotNull { it.longitude }.average()

            // Calculate weight based on stress_level (0=Low, 1=Medium, 2=High)
            // Convert stress_level strings to numeric values
            val stressLevels = entries.mapNotNull { entry ->
                when (entry.stress_level.lowercase()) {
                    "low", "0" -> 0f
                    "medium", "1" -> 1f
                    "high", "2" -> 2f
                    else -> when (entry.mood.lowercase()) {
                        "relaxed" -> 0f
                        "alert" -> 1f
                        "stressed" -> 2f
                        else -> null
                    }
                }
            }

            val avgLevel = if (stressLevels.isNotEmpty()) {
                stressLevels.average().toFloat()
            } else {
                if (isStressed) 1.5f else 0f
            }

            val normalizedWeight = if (isStressed) {
                // Stress zones: normalize 0-2 to 0-1 (0.5=Alert, 1.0=Stressed)
                (avgLevel / 2f).coerceIn(0f, 1f)
            } else {
                // Calming zones: all relaxed entries get high weight (inverted for visibility)
                1f - (avgLevel / 2f).coerceIn(0f, 1f)
            }

            // Calculate average stress score
            val avgStressScore = entries.map { it.stress_score }.average().toFloat()

            // Get most common mood and stress level
            val moodCounts = entries.groupingBy { it.mood }.eachCount()
            val mostCommonMood = moodCounts.maxByOrNull { it.value }?.key ?: "Unknown"

            val levelCounts = entries.groupingBy { it.stress_level }.eachCount()
            val mostCommonLevel = levelCounts.maxByOrNull { it.value }?.key ?: "Unknown"

            // Get time range
            val timestamps = entries.map { it.timestamp }
            val firstOccurrence = timestamps.minOrNull() ?: 0L
            val lastOccurrence = timestamps.maxOrNull() ?: 0L

            // Format occurrence dates
            val dateFormatter = java.text.SimpleDateFormat("MMM dd", Locale.getDefault())
            val occurrenceDates = timestamps.map {
                dateFormatter.format(Date(it))
            }.distinct().sorted()

            StressCluster(
                latitude = avgLat,
                longitude = avgLng,
                weight = normalizedWeight,
                eventCount = entries.size,
                isCalmingZone = !isStressed,
                averageStressScore = avgStressScore,
                stressLevel = mostCommonLevel,
                mood = mostCommonMood,
                firstOccurrence = firstOccurrence,
                lastOccurrence = lastOccurrence,
                occurrenceDates = occurrenceDates
            )
        }.sortedByDescending { it.eventCount }

        Log.d("StressTerrainRepository", "Created ${clusters.size} ${if (isStressed) "stress" else "calming"} clusters")
        clusters.forEach { cluster ->
            Log.d("StressTerrainRepository", "  - Cluster: Lat=${cluster.latitude}, Lng=${cluster.longitude}, Weight=${cluster.weight}, Events=${cluster.eventCount}")
        }

        return clusters
    }

    /**
     * Converts stress clusters to heatmap-compatible data (LatLng + weight)
     */
    fun clustersToHeatmapData(clusters: List<StressCluster>): List<Pair<LatLng, Double>> {
        return clusters.map { cluster ->
            LatLng(cluster.latitude, cluster.longitude) to cluster.weight.toDouble()
        }
    }
}

