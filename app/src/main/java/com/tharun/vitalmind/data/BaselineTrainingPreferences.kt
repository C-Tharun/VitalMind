package com.tharun.vitalmind.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent state for baseline model training status
 * Uses SharedPreferences to track per-user training state
 */
class BaselineTrainingPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "baseline_training_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_MODEL_TRAINED_PREFIX = "model_trained_"
        private const val KEY_LAST_TRAINING_DATE_PREFIX = "last_training_date_"
    }

    /**
     * Check if the baseline model has been trained on the server for this user
     */
    fun isModelTrained(userId: String): Boolean {
        return prefs.getBoolean("${KEY_MODEL_TRAINED_PREFIX}$userId", false)
    }

    /**
     * Mark that the baseline model has been trained on the server
     */
    fun setModelTrained(userId: String, trained: Boolean) {
        prefs.edit()
            .putBoolean("${KEY_MODEL_TRAINED_PREFIX}$userId", trained)
            .apply()
    }

    /**
     * Get the timestamp of the last model training
     * Returns 0 if never trained
     */
    fun getLastTrainingDate(userId: String): Long {
        return prefs.getLong("${KEY_LAST_TRAINING_DATE_PREFIX}$userId", 0L)
    }

    /**
     * Save the timestamp of the most recent model training
     */
    fun setLastTrainingDate(userId: String, timestamp: Long) {
        prefs.edit()
            .putLong("${KEY_LAST_TRAINING_DATE_PREFIX}$userId", timestamp)
            .apply()
    }

    /**
     * Check if retraining is needed based on time (30 days since last training)
     * NOTE: This only checks time. Use hasNewBaselineDataSinceLastTraining() to verify new data exists.
     */
    fun needsRetraining(userId: String): Boolean {
        val lastTrainingDate = getLastTrainingDate(userId)
        if (lastTrainingDate == 0L) return false  // Never trained, will train on first baseline ready

        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val timeSinceLastTraining = System.currentTimeMillis() - lastTrainingDate

        return timeSinceLastTraining >= thirtyDaysInMillis
    }

    /**
     * Reset training status for a user (useful for testing/debugging)
     */
    fun resetTrainingStatus(userId: String) {
        prefs.edit()
            .remove("${KEY_MODEL_TRAINED_PREFIX}$userId")
            .remove("${KEY_LAST_TRAINING_DATE_PREFIX}$userId")
            .apply()
    }
}


