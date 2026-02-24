package com.tharun.vitalmind.ui.healthdeviation

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tharun.vitalmind.data.repository.HealthDeviationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for Health Deviation (PHBD-Net) feature with personalized baseline support
 */
class HealthDeviationViewModel(
    private val repository: HealthDeviationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthDeviationUiStateExtended>(HealthDeviationUiStateExtended.Idle)
    val uiState: StateFlow<HealthDeviationUiStateExtended> = _uiState

    // Export state for showing feedback to user
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage

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

                    // Fetch baseline and today's metrics for quantitative comparison
                    val baselineMetrics = repository.getBaselineStatistics()
                    val todayMetrics = repository.getTodayMetrics()

                    _uiState.value = HealthDeviationUiStateExtended.Success(
                        response = response,
                        todayMetrics = todayMetrics,
                        baselineMetrics = baselineMetrics
                    )
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

    /**
     * Smart retry function - called from UI retry button
     *
     * Handles different error scenarios:
     * - If model missing (404), triggers retraining via checkBaselineStatus()
     * - Otherwise, checks current state and takes appropriate action
     */
    fun retryAnalysis() {
        Log.d("HealthDeviationVM", "🔄 retryAnalysis() called")

        val currentState = _uiState.value
        Log.d("HealthDeviationVM", "Current state: ${currentState.javaClass.simpleName}")

        // If currently in error state and model is not trained, need to retrain
        if (currentState is HealthDeviationUiStateExtended.Error && !repository.isModelTrained()) {
            Log.d("HealthDeviationVM", "Model not trained - triggering baseline check and training")
            checkBaselineStatus()
        } else {
            Log.d("HealthDeviationVM", "Retrying analysis")
            analyzeHealthDeviation()
        }
    }

    /**
     * Export baseline data to CSV file in Documents/VitalMindExports/
     *
     * ⚠️ CRITICAL: This is READ-ONLY - does NOT modify baseline data or training status
     *
     * Uses MediaStore (scoped storage) for Android 10+ compatibility
     * Saves to: Documents/VitalMindExports/vitalmind_baseline_user_{userId}_{YYYYMMDD}.csv
     *
     * @param context Android context needed for ContentResolver
     */
    fun exportBaselineData(context: Context) {
        Log.d("HealthDeviationVM", "📤 exportBaselineData() called")

        viewModelScope.launch {
            try {
                // Step 1: Get CSV content from repository (read-only operation)
                Log.d("HealthDeviationVM", "Fetching baseline data from repository...")
                val csvResult = repository.exportBaselineDataToCsv()

                if (csvResult.isFailure) {
                    val errorMsg = csvResult.exceptionOrNull()?.message ?: "Export failed"
                    Log.e("HealthDeviationVM", "❌ Export failed: $errorMsg")
                    _exportMessage.value = errorMsg
                    return@launch
                }

                val csvContent = csvResult.getOrNull() ?: ""
                if (csvContent.isEmpty()) {
                    Log.e("HealthDeviationVM", "❌ CSV content is empty")
                    _exportMessage.value = "No data to export"
                    return@launch
                }

                Log.d("HealthDeviationVM", "✅ CSV generated: ${csvContent.lines().size} lines")

                // Step 2: Generate filename with date
                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val dateString = dateFormat.format(Date())
                val userId = repository.hashCode().toString() // Using repository hashCode as simple identifier
                val filename = "vitalmind_baseline_${dateString}.csv"

                Log.d("HealthDeviationVM", "Saving to filename: $filename")

                // Step 3: Save to MediaStore (scoped storage for Android 10+)
                val contentResolver = context.contentResolver

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/VitalMindExports")
                    }
                }

                val uri = contentResolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    contentValues
                )

                if (uri == null) {
                    Log.e("HealthDeviationVM", "❌ Failed to create MediaStore entry")
                    _exportMessage.value = "Failed to create export file"
                    return@launch
                }

                Log.d("HealthDeviationVM", "MediaStore URI created: $uri")

                // Write CSV content to file
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                    outputStream.flush()
                    Log.d("HealthDeviationVM", "✅ CSV written to file successfully")
                }

                // Success!
                val successMsg = "Baseline exported successfully to Documents/VitalMindExports"
                Log.d("HealthDeviationVM", "✅ $successMsg")
                _exportMessage.value = successMsg

            } catch (e: Exception) {
                Log.e("HealthDeviationVM", "❌ Export error: ${e.message}", e)
                _exportMessage.value = "Export failed: ${e.message}"
            }
        }
    }

    /**
     * Clear export message after user has seen it
     */
    fun clearExportMessage() {
        _exportMessage.value = null
    }
}


