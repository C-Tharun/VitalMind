package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.repository.HealthDeviationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Health Deviation (PHBD-Net) feature with personalized baseline support
 */
class HealthDeviationViewModel(
    private val repository: HealthDeviationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthDeviationUiStateExtended>(HealthDeviationUiStateExtended.Idle)
    val uiState: StateFlow<HealthDeviationUiStateExtended> = _uiState

    init {
        /**
         * Baseline Collection Strategy:
         *
         * - Check baseline status immediately to determine UI state
         * - Attempt to collect today's baseline data
         *
         * Why baseline collection runs in init():
         * - Ensures baseline is updated each time user opens Home screen
         * - Repository has safety checks to prevent premature collection
         * - If data is insufficient (e.g., Google Fit not synced yet), collection is skipped
         * - Will retry on next app open or sync
         *
         * Race condition safety:
         * - collectTodaysBaseline() checks for sufficient data before saving
         * - Requires minimum: 3 HR samples OR steps/calories present
         * - Silent failure if conditions not met (no UI impact)
         *
         * Why resting HR is derived:
         * - Google Fit API doesn't provide explicit "resting heart rate"
         * - We derive it from HR samples during sleep or low-activity periods
         * - Uses lowest 15th percentile of rest-period HR readings
         * - This is physiologically accurate and defensible
         */
        checkBaselineStatus()
        collectTodaysBaseline()
    }

    /**
     * Check if baseline is ready or still collecting
     * Also triggers training if baseline is ready but model not yet trained
     */
    fun checkBaselineStatus() {
        Log.d("PHBD_TRAINING", "🔍 Checking baseline status")
        viewModelScope.launch {
            try {
                val (status, daysCollected) = repository.getBaselineStatus()
                Log.d("PHBD_TRAINING", "📊 Baseline status: $status, Days: $daysCollected/${HealthDeviationRepository.MINIMUM_BASELINE_DAYS}")

                when (status) {
                    BaselineStatus.COLLECTING -> {
                        Log.d("PHBD_TRAINING", "Baseline still collecting - training not triggered")
                        _uiState.value = HealthDeviationUiStateExtended.CollectingBaseline(
                            daysCollected = daysCollected,
                            daysNeeded = HealthDeviationRepository.MINIMUM_BASELINE_DAYS
                        )
                    }
                    BaselineStatus.READY -> {
                        // ✅ CRITICAL: Check if model needs training
                        Log.d("PHBD_TRAINING", "Baseline READY. Checking training status...")
                        checkAndTriggerTraining()
                    }
                }
            } catch (e: Exception) {
                Log.e("HealthDeviationVM", "❌ Error checking baseline status: ${e.message}", e)
                _uiState.value = HealthDeviationUiStateExtended.Error("Failed to check baseline status")
            }
        }
    }

    /**
     * Check if training is needed and trigger it
     *
     * Training is triggered ONLY when:
     * 1. Baseline is READY (10+ days) AND model not yet trained
     * 2. OR 30 days passed since last training AND 10+ new baseline days exist
     *
     * This prevents infinite retraining loops.
     */
    private suspend fun checkAndTriggerTraining() {
        val isModelTrained = repository.isModelTrained()
        val needsRetraining = repository.needsRetraining()

        Log.d("PHBD_TRAINING", "Training status check:")
        Log.d("PHBD_TRAINING", "   isModelTrained = $isModelTrained")
        Log.d("PHBD_TRAINING", "   needsRetraining (30+ days) = $needsRetraining")

        // Case 1: Never trained before
        if (!isModelTrained) {
            Log.d("PHBD_TRAINING", "✅ Model not trained yet - triggering initial training")
            trainBaselineModel()
            return
        }

        // Case 2: Model trained but 30+ days passed - check if new data exists
        if (needsRetraining) {
            Log.d("PHBD_TRAINING", "⏰ 30 days passed since last training - checking for new baseline data")

            val hasNewData = repository.hasNewBaselineDataSinceLastTraining()
            Log.d("PHBD_TRAINING", "   hasNewBaselineData = $hasNewData")

            if (hasNewData) {
                Log.d("PHBD_TRAINING", "✅ New baseline data available - triggering retraining")
                trainBaselineModel()
            } else {
                Log.d("PHBD_TRAINING", "⚠️ No new baseline data - skipping retraining")
                Log.d("PHBD_TRAINING", "   Model remains trained, UI set to Ready")
                _uiState.value = HealthDeviationUiStateExtended.Ready
            }
            return
        }

        // Case 3: Model already trained and up-to-date
        Log.d("PHBD_TRAINING", "✅ Model already trained and up-to-date")
        Log.d("PHBD_TRAINING", "   No training needed, UI set to Ready")
        _uiState.value = HealthDeviationUiStateExtended.Ready
    }

    /**
     * Train the baseline model on the server
     */
    private fun trainBaselineModel() {
        Log.d("PHBD_TRAINING", "🚀 Starting baseline model training")
        _uiState.value = HealthDeviationUiStateExtended.TrainingModel

        viewModelScope.launch {
            repository.trainBaselineOnServer()
                .onSuccess { response ->
                    Log.d("PHBD_TRAINING", "✅ Training completed successfully: $response")
                    _uiState.value = HealthDeviationUiStateExtended.Ready
                }
                .onFailure { error ->
                    Log.e("PHBD_TRAINING", "❌ Baseline training failed: ${error.message}", error)
                    // Don't set Ready - show error instead
                    _uiState.value = HealthDeviationUiStateExtended.Error(
                        "Training failed: ${error.message}. Please try again later."
                    )
                }
        }
    }

    /**
     * Collect today's baseline data (called automatically and can be manually triggered)
     */
    fun collectTodaysBaseline() {
        viewModelScope.launch {
            repository.collectTodaysBaseline()
                .onSuccess {
                    Log.d("HealthDeviationVM", "✅ Today's baseline collected")
                    // Refresh status after collection
                    checkBaselineStatus()
                }
                .onFailure { error ->
                    Log.w("HealthDeviationVM", "⚠️ Could not collect today's baseline: ${error.message}")
                }
        }
    }

    /**
     * Analyze health deviation - called from UI
     *
     * Requirements:
     * 1. Baseline must be READY (10+ days)
     * 2. Model must be trained on server
     *
     * If model missing on backend (404), triggers automatic retraining.
     */
    fun analyzeHealthDeviation() {
        Log.d("HealthDeviationVM", "🔄 analyzeHealthDeviation() called")

        // Check if baseline is ready
        if (_uiState.value !is HealthDeviationUiStateExtended.Ready) {
            Log.w("HealthDeviationVM", "⚠️ Cannot analyze - baseline not ready or model not trained")
            return
        }

        // ✅ CRITICAL: Double-check that model is trained
        if (!repository.isModelTrained()) {
            Log.e("HealthDeviationVM", "❌ Cannot analyze - model not trained on server")
            _uiState.value = HealthDeviationUiStateExtended.Error("Model not trained. Please wait for training to complete.")
            return
        }

        _uiState.value = HealthDeviationUiStateExtended.Loading
        Log.d("HealthDeviationVM", "State changed to Loading")

        viewModelScope.launch {
            Log.d("HealthDeviationVM", "Launching coroutine to call repository")
            repository.getHealthDeviation()
                .onSuccess { response ->
                    Log.d("HealthDeviationVM", "✅ Success! Response: $response")
                    _uiState.value = HealthDeviationUiStateExtended.Success(response)
                }
                .onFailure { error ->
                    Log.e("HealthDeviationVM", "❌ Error occurred: ${error.message}", error)

                    // ✅ CRITICAL: Check if model missing on backend (404)
                    // Repository already resets training status on 404
                    if (error.message?.contains("Model missing on server") == true) {
                        Log.d("HealthDeviationVM", "🔄 Model missing on backend - triggering retraining")
                        // Trigger automatic retraining
                        checkBaselineStatus()
                    } else {
                        _uiState.value = HealthDeviationUiStateExtended.Error(
                            error.message ?: "Unknown error occurred"
                        )
                    }
                }
        }
    }
}


