package com.tharun.vitalsync.health

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.tharun.vitalsync.data.HealthData
import com.tharun.vitalsync.ui.MetricType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GoogleFitManager(private val context: Context) {

    private fun getGoogleAccount() = GoogleSignIn.getLastSignedInAccount(context)

    // Helper to get the sum of a field for today using readData
    private suspend fun fetchSumForToday(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, dataType: DataType, field: Field): Float? {
        val end = Instant.now()
        val start = ZonedDateTime.ofInstant(end, ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val request = DataReadRequest.Builder()
            .read(dataType)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()
        val response = Fitness.getHistoryClient(context, account).readData(request).await()
        val sum = response.getDataSet(dataType).dataPoints.sumOf { it.getValue(field).asFloat().toDouble() }
        return if (sum > 0) sum.toFloat() else null
    }

    suspend fun readTodaySummary(): HealthData {
        val account = getGoogleAccount() ?: throw IllegalStateException("Google Fit account not signed in.")

        val end = Instant.now()
        val start = ZonedDateTime.ofInstant(end, ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()

        val heartRate = fetchLatestHeartRate(account, start, end)
        val steps = fetchSumForToday(account, DataType.TYPE_STEP_COUNT_DELTA, Field.FIELD_STEPS)?.toInt()
        val calories = fetchSumForToday(account, DataType.TYPE_CALORIES_EXPENDED, Field.FIELD_CALORIES)
        val distance = fetchSumForToday(account, DataType.TYPE_DISTANCE_DELTA, Field.FIELD_DISTANCE)?.let { it / 1000 } // Convert to km
        val sleepDuration = fetchSleepDuration(account, start, end)
        val lastActivity = fetchLastActivity(account, start, end)

        return HealthData(
            userId = account.id ?: "guest",
            timestamp = System.currentTimeMillis(),
            heartRate = heartRate,
            steps = steps,
            calories = calories,
            distance = distance,
            heartPoints = 0, // Default value as it's disabled
            sleepDuration = sleepDuration,
            activityType = lastActivity
        )
    }

    suspend fun readHistoricalData(startTime: Long, endTime: Long, metricType: MetricType): List<HealthData> {
        val account = getGoogleAccount() ?: throw IllegalStateException("Google Fit account not signed in.")

        if (metricType == MetricType.HEART_RATE) {
            // Heart rate data can be very large, so we aggregate it into 1-minute buckets to improve performance.
            val readRequest = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM)
                .bucketByTime(1, TimeUnit.MINUTES)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account).readData(readRequest).await()
            return response.buckets.flatMap { bucket ->
                bucket.dataSets.flatMap { dataSet ->
                    dataSet.dataPoints.mapNotNull { dataPoint ->
                        try {
                            // Use the average heart rate for the bucket.
                            HealthData(
                                userId = account.id!!,
                                timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS),
                                heartRate = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat(),
                                steps = null, calories = null, distance = null, sleepDuration = null, activityType = null, heartPoints = null
                            )
                        } catch (e: Exception) {
                            Log.e("GoogleFitManager", "Error processing heart rate data point from bucket", e)
                            null
                        }
                    }
                }
            }
        }

        if (metricType == MetricType.SLEEP) {
            // Expand window: 6pm previous day to noon next day
            val cal = Calendar.getInstance()
            cal.timeInMillis = startTime
            cal.add(Calendar.HOUR_OF_DAY, -6) // 6pm previous day
            val sleepWindowStart = cal.timeInMillis
            cal.timeInMillis = endTime
            cal.add(Calendar.HOUR_OF_DAY, 12) // noon next day
            val sleepWindowEnd = cal.timeInMillis

            val request = SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval(sleepWindowStart, sleepWindowEnd, TimeUnit.MILLISECONDS)
                .build()
            try {
                val response = Fitness.getSessionsClient(context, account).readSession(request).await()
                // Aggregate all sleep segments per night
                val sleepSegments = response.sessions.flatMap { session ->
                    response.getDataSet(session).flatMap { dataSet ->
                        dataSet.dataPoints.mapNotNull { dataPoint ->
                            val segmentStart = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                            val segmentEnd = dataPoint.getEndTime(TimeUnit.MILLISECONDS)
                            val duration = segmentEnd - segmentStart
                            if (duration > 0) {
                                // Assign to the night based on start time (set to 6pm that day)
                                val nightCal = Calendar.getInstance()
                                nightCal.timeInMillis = segmentStart
                                if (nightCal.get(Calendar.HOUR_OF_DAY) < 18) {
                                    nightCal.add(Calendar.DATE, -1)
                                }
                                nightCal.set(Calendar.HOUR_OF_DAY, 18)
                                nightCal.set(Calendar.MINUTE, 0)
                                nightCal.set(Calendar.SECOND, 0)
                                nightCal.set(Calendar.MILLISECOND, 0)
                                val nightKey = nightCal.timeInMillis
                                Pair(nightKey, duration)
                            } else null
                        }
                    }
                }
                // Group by night and sum durations
                val sleepByNight = sleepSegments.groupBy({ it.first }, { it.second })
                return sleepByNight.map { (night, durations) ->
                    HealthData(
                        userId = account.id!!,
                        timestamp = night,
                        sleepDuration = TimeUnit.MILLISECONDS.toMinutes(durations.sum()),
                        heartRate = null, steps = null, calories = null, distance = null, activityType = null, heartPoints = null
                    )
                }.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("GoogleFitManager", "Error reading sleep sessions", e)
                return emptyList()
            }
        }

        val (dataType, field) = getDataType(metricType)

        val readRequest = DataReadRequest.Builder()
            .read(dataType)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val response = Fitness.getHistoryClient(context, account).readData(readRequest).await()
        val dataPoints = response.getDataSet(dataType).dataPoints

        return dataPoints.mapNotNull { dataPoint ->
            try {
                val timestamp = dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                val activityType = if (metricType == MetricType.ACTIVITY) dataPoint.getValue(field).asActivity() else null
                HealthData(
                    userId = account.id!!,
                    timestamp = timestamp,
                    heartRate = null, // Heart rate is handled above with aggregation
                    steps = if (metricType == MetricType.STEPS) dataPoint.getValue(field).asInt() else null,
                    calories = if (metricType == MetricType.CALORIES) dataPoint.getValue(field).asFloat() else null,
                    distance = if (metricType == MetricType.DISTANCE) dataPoint.getValue(field).asFloat() / 1000f else null,
                    sleepDuration = if (metricType == MetricType.SLEEP) (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS)) / 60000 else null,
                    activityType = activityType,
                    heartPoints = null
                )
            } catch (e: Exception) {
                Log.e("GoogleFitManager", "Error processing data point for $metricType", e)
                null
            }
        }
    }

    private fun getDataType(metricType: MetricType): Pair<DataType, Field> {
        return when (metricType) {
            MetricType.HEART_RATE -> DataType.TYPE_HEART_RATE_BPM to Field.FIELD_BPM
            MetricType.STEPS -> DataType.TYPE_STEP_COUNT_DELTA to Field.FIELD_STEPS // Use per-interval steps
            MetricType.CALORIES -> DataType.AGGREGATE_CALORIES_EXPENDED to Field.FIELD_CALORIES
            MetricType.DISTANCE -> DataType.AGGREGATE_DISTANCE_DELTA to Field.FIELD_DISTANCE
            MetricType.SLEEP -> DataType.TYPE_SLEEP_SEGMENT to Field.FIELD_SLEEP_SEGMENT_TYPE
            MetricType.ACTIVITY -> DataType.TYPE_ACTIVITY_SEGMENT to Field.FIELD_ACTIVITY
            MetricType.HEART_POINTS -> DataType.TYPE_HEART_POINTS to Field.FIELD_INTENSITY
            else -> throw IllegalArgumentException("Unsupported metric type: $metricType")
        }
    }

    private suspend fun fetchLatestHeartRate(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, start: Instant, end: Instant): Float? {
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()
        val response = Fitness.getHistoryClient(context, account).readData(request).await()
        // Get the latest heart rate value for today
        return response.getDataSet(DataType.TYPE_HEART_RATE_BPM).dataPoints.maxByOrNull { it.getEndTime(TimeUnit.MILLISECONDS) }?.getValue(Field.FIELD_BPM)?.asFloat()
    }

    private suspend fun fetchSleepDuration(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, start: Instant, end: Instant): Long? {
        val request = SessionReadRequest.Builder()
            .readSessionsFromAllApps()
            .includeSleepSessions()
            .read(DataType.TYPE_SLEEP_SEGMENT)
            .setTimeInterval(start.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()
        try {
            val response = Fitness.getSessionsClient(context, account).readSession(request).await()
            val totalSleepMillis = response.sessions.sumOf { session ->
                response.getDataSet(session).sumOf { dataSet ->
                    dataSet.dataPoints.sumOf { dp ->
                        dp.getEndTime(TimeUnit.MILLISECONDS) - dp.getStartTime(TimeUnit.MILLISECONDS)
                    }.toDouble()
                }.toLong()
            }
            return if (totalSleepMillis > 0) TimeUnit.MILLISECONDS.toMinutes(totalSleepMillis) else null
        } catch (e: Exception) {
            Log.e("GoogleFitManager", "Error fetching sleep duration using sessions", e)
            return null
        }
    }

    private suspend fun fetchLastActivity(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, start: Instant, end: Instant): String? {
        // Ensure the range covers the full day (00:00 to now)
        val dayStart = ZonedDateTime.ofInstant(end, ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_ACTIVITY_SEGMENT)
            .setTimeRange(dayStart.toEpochMilli(), end.toEpochMilli(), TimeUnit.MILLISECONDS)
            .build()
        val response = Fitness.getHistoryClient(context, account).readData(request).await()
        val dataPoints = response.getDataSet(DataType.TYPE_ACTIVITY_SEGMENT).dataPoints
        val lastDataPoint = dataPoints.maxByOrNull { it.getEndTime(TimeUnit.MILLISECONDS) }
        if (lastDataPoint == null) {
            Log.w("GoogleFitManager", "No activity found for the day in fetchLastActivity")
            return null // or return "No activity recorded" if you want a string
        }
        return lastDataPoint.getValue(Field.FIELD_ACTIVITY).asActivity()
    }
}
