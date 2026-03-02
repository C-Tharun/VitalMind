package com.tharun.vitalmind.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
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
    val isCalmingZone: Boolean = false
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

            Log.d("StressTerrainRepository", "Fetching stress terrain data for last $dayCount days")

            // First, try to get stress score history with location data
            stressScoreHistoryDao.getHistoryForUser(userId).collect { stressHistory ->
                val recentHistory = stressHistory.filter {
                    it.timestamp >= startTime &&
                    it.timestamp <= endTime &&
                    it.latitude != null &&
                    it.longitude != null
                }

                if (recentHistory.isNotEmpty()) {
                    Log.d("StressTerrainRepository", "Found ${recentHistory.size} stress scores with location data")

                    // Convert stress score history to stress clusters
                    val stressClusters = convertStressHistoryToClusters(recentHistory, isStressed = true)
                    val calmingClusters = convertStressHistoryToClusters(recentHistory, isStressed = false)

                    emit(Pair(stressClusters, calmingClusters))
                    return@collect
                }

                // Fallback: Try health data with location (original method)
                Log.d("StressTerrainRepository", "No stress history with location found, trying health data...")
                healthDataDao.getDataForRange(userId, startTime, endTime).collect { healthData ->
                    val locatedData = healthData.filter {
                        it.latitude != null &&
                        it.longitude != null &&
                        it.heartRate != null &&
                        it.heartRate!! > 0
                    }

                    if (locatedData.isEmpty()) {
                        Log.d("StressTerrainRepository", "No health data with location information found")
                        emit(Pair(emptyList(), emptyList()))
                        return@collect
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
                }
            }
        } catch (e: Exception) {
            Log.e("StressTerrainRepository", "Error getting stress terrain data", e)
            emit(Pair(emptyList(), emptyList()))
        }
    }

    /**
     * Converts StressScoreHistory entries with location to StressCluster objects
     */
    private fun convertStressHistoryToClusters(
        history: List<StressScoreHistory>,
        isStressed: Boolean
    ): List<StressCluster> {
        val filteredHistory = if (isStressed) {
            // High stress: scores >= 60
            history.filter { it.stress_score >= 60f }
        } else {
            // Low stress (calming): scores < 40
            history.filter { it.stress_score < 40f }
        }

        if (filteredHistory.isEmpty()) return emptyList()

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

        // Create clusters
        return clusteredMap.map { (_, entries) ->
            val avgLat = entries.mapNotNull { it.latitude }.average()
            val avgLng = entries.mapNotNull { it.longitude }.average()
            val avgScore = entries.map { it.stress_score }.average().toFloat()
            val normalizedWeight = if (isStressed) {
                // High stress: normalize from 60-100 to 0-1
                ((avgScore - 60f) / 40f).coerceIn(0f, 1f)
            } else {
                // Calming: normalize from 0-40 to 1-0 (inverse)
                (1f - (avgScore / 40f)).coerceIn(0f, 1f)
            }

            StressCluster(
                latitude = avgLat,
                longitude = avgLng,
                weight = normalizedWeight,
                eventCount = entries.size,
                isCalmingZone = !isStressed
            )
        }.sortedByDescending { it.eventCount }
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

