package com.tharun.vitalmind.ui.healthdeviation

/**
 * Baseline collection status for personalized health deviation analysis
 */
enum class BaselineStatus {
    COLLECTING,  // Still collecting baseline data (< 10 days)
    READY        // Baseline ready (>= 10 days)
}

/**
 * Extended UI states for Health Deviation with baseline support
 */
sealed class HealthDeviationUiStateExtended {
    object Idle : HealthDeviationUiStateExtended()
    data class CollectingBaseline(val daysCollected: Int, val daysNeeded: Int) : HealthDeviationUiStateExtended()
    object TrainingModel : HealthDeviationUiStateExtended()  // Training baseline model on server
    object Ready : HealthDeviationUiStateExtended()
    object Loading : HealthDeviationUiStateExtended()
    data class Success(
        val response: com.tharun.vitalmind.data.remote.HealthDeviationResponse,
        val todayMetrics: TodayMetrics? = null,
        val baselineMetrics: BaselineMetrics? = null
    ) : HealthDeviationUiStateExtended()
    data class Error(val message: String) : HealthDeviationUiStateExtended()
}

/**
 * Today's aggregated health metrics for quantitative comparison
 */
data class TodayMetrics(
    val steps: Int,
    val sleepMinutes: Int,
    val calories: Float,
    val avgHeartRate: Float,
    val restingHeartRate: Float
)

/**
 * Baseline statistics for comparison
 */
data class BaselineMetrics(
    val avgSteps: Float,
    val avgSleepMinutes: Float,
    val avgCalories: Float,
    val avgHeartRate: Float,
    val avgRestingHeartRate: Float
)

