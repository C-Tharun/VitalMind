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
    object Ready : HealthDeviationUiStateExtended()
    object Loading : HealthDeviationUiStateExtended()
    data class Success(val response: com.tharun.vitalmind.data.remote.HealthDeviationResponse) : HealthDeviationUiStateExtended()
    data class Error(val message: String) : HealthDeviationUiStateExtended()
}

