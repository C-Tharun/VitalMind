package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.data.remote.HealthDeviationResponse
import java.util.Locale

/**
 * Health Deviation Card - displays PHBD-Net analysis results with baseline support
 */
@Composable
fun HealthDeviationCard(
    uiState: HealthDeviationUiStateExtended,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    onExport: () -> Unit,
    exportMessage: String?
) {
    // Log current state for debugging
    LaunchedEffect(uiState) {
        Log.d("HealthDeviationCard", "📱 Current UI State: ${uiState.javaClass.simpleName}")
        if (uiState is HealthDeviationUiStateExtended.Error) {
            Log.e("HealthDeviationCard", "Error state message: ${uiState.message}")
        }
    }

    // Snackbar host state for showing export messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when export message changes
    LaunchedEffect(exportMessage) {
        exportMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Health Deviation Analysis",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Personalized baseline deviation using ML",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    when (uiState) {
                        is HealthDeviationUiStateExtended.Idle -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(8.dp)
                            )
                        }

                        is HealthDeviationUiStateExtended.CollectingBaseline -> {
                            BaselineCollectionView(
                                daysCollected = uiState.daysCollected,
                                daysNeeded = uiState.daysNeeded
                            )
                        }

                        is HealthDeviationUiStateExtended.TrainingModel -> {
                            TrainingModelView()
                        }

                        is HealthDeviationUiStateExtended.Ready -> {
                            Button(
                                onClick = {
                                    Log.d("HealthDeviationCard", "🔵 Analyze Deviation button clicked")
                                    onAnalyze()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Analyze Deviation")
                            }
                        }

                        is HealthDeviationUiStateExtended.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .padding(8.dp)
                                )
                                Text(
                                    text = "Analyzing your health patterns...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "This may take 30-60 seconds if the backend is starting up",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is HealthDeviationUiStateExtended.Success -> {
                            HealthDeviationResult(
                                response = uiState.response,
                                todayMetrics = uiState.todayMetrics,
                                baselineMetrics = uiState.baselineMetrics
                            )
                        }

                        is HealthDeviationUiStateExtended.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "⚠️ ${uiState.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Health deviation unavailable today",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        Log.d("HealthDeviationCard", "🔄 Retry button clicked")
                                        onRetry()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            // Export Baseline Data button (for research evaluation)
            // Only show when baseline data exists (not during Idle state)
            if (uiState !is HealthDeviationUiStateExtended.Idle) {
                OutlinedButton(
                    onClick = {
                        Log.d("HealthDeviationCard", "📤 Export Baseline button clicked")
                        onExport()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Baseline Data")
                }
            }
        }

        // Snackbar at bottom of screen
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

/**
 * Shows baseline collection progress
 */
@Composable
private fun BaselineCollectionView(daysCollected: Int, daysNeeded: Int) {
    val progress = daysCollected.toFloat() / daysNeeded.toFloat()
    val daysRemaining = daysNeeded - daysCollected

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔄 Personalizing your health baseline",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = "Collecting data for a personalized experience",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$daysCollected / $daysNeeded days collected",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Available in $daysRemaining ${if (daysRemaining == 1) "day" else "days"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Info message
        Text(
            text = "💡 Keep syncing your health data daily to build your personalized baseline",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Shows model training in progress
 */
@Composable
private fun TrainingModelView() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .padding(8.dp)
        )

        Text(
            text = "🧠 Training your personalized baseline model",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "This may take a moment...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Text(
            text = "Your baseline data is being processed by our AI to create a personalized health model",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Displays the health deviation analysis results with intelligent interpretation
 * and quantitative baseline comparisons
 */
@Composable
private fun HealthDeviationResult(
    response: HealthDeviationResponse,
    todayMetrics: TodayMetrics?,
    baselineMetrics: BaselineMetrics?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 🎯 PRIMARY: Drift Level Badge (Enhanced Visual Hierarchy)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = getDriftLevelColor(response.stress_drift_level).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Drift Level (Primary)
                Text(
                    text = "Health Drift: ${response.stress_drift_level.uppercase()}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = getDriftLevelColor(response.stress_drift_level)
                )

                // Score (Secondary)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Score: ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f", response.health_deviation_score),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = getDriftLevelColor(response.stress_drift_level)
                    )
                }

                // Confidence (Tertiary)
                Text(
                    text = "Confidence: ${(response.confidence * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (response.confidence >= 0.8f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
        }

        // 📊 DRIFT INTERPRETATION with Scientific Context
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "💡",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Text(
                            text = getEnhancedDriftInterpretation(response.stress_drift_level),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = getScientificContext(response.stress_drift_level),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Analytical Summary Sentence
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getAnalyticalSummary(
                        driftLevel = response.stress_drift_level,
                        topContributors = response.top_contributors
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 28.dp)
                )

                // Safety Context for HIGH drift
                if (response.stress_drift_level.lowercase() == "high") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "High drift reflects a significant change from your normal pattern and does not necessarily indicate a health issue.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 28.dp)
                    )
                }
            }
        }

        // Partial data warning
        if (response.confidence < 1.0f) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ ",
                    fontSize = 14.sp
                )
                Text(
                    text = "Sync more health metrics for better accuracy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }

        // 🔬 QUANTITATIVE FEATURE EXPLANATIONS
        if (response.top_contributors.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "📈 Contributing Factors",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                response.top_contributors.take(3).forEach { contributor ->
                    QuantitativeFeatureExplanation(
                        featureName = contributor,
                        todayMetrics = todayMetrics,
                        baselineMetrics = baselineMetrics
                    )
                }
            }
        }
    }
}

/**
 * Enhanced drift interpretation with more scientific context
 */
private fun getEnhancedDriftInterpretation(level: String): String {
    return when (level.lowercase()) {
        "low" -> "Today's deviation is within your normal variability range."
        "medium" -> "Today's deviation slightly exceeds your typical variability."
        "high" -> "Today's deviation significantly exceeds your normal baseline pattern."
        else -> "Analysis completed based on your personalized baseline model."
    }
}

/**
 * Scientific context for drift level
 */
private fun getScientificContext(level: String): String {
    return when (level.lowercase()) {
        "low" -> "Your physiological metrics are consistent with your established baseline. Continue your current routines."
        "medium" -> "Some metrics show moderate variation. This is often normal but worth monitoring over the next few days."
        "high" -> "Multiple metrics deviate from baseline. Consider reviewing sleep quality, activity patterns, and stress levels."
        else -> ""
    }
}

/**
 * Analytical summary sentence based on drift level and top contributors
 */
private fun getAnalyticalSummary(driftLevel: String, topContributors: List<String>): String {
    return when (driftLevel.lowercase()) {
        "high" -> {
            if (topContributors.isNotEmpty()) {
                val topFeature = formatContributor(topContributors.first())
                "Your elevated $topFeature appears to be the primary driver of today's drift."
            } else {
                "Multiple factors are contributing to today's elevated drift."
            }
        }
        "medium" -> "A few metrics are moderately contributing to your deviation."
        "low" -> "Minor variations detected, but overall pattern remains stable."
        else -> ""
    }
}

/**
 * Displays quantitative comparison between today's value and baseline
 */
@Composable
private fun QuantitativeFeatureExplanation(
    featureName: String,
    todayMetrics: TodayMetrics?,
    baselineMetrics: BaselineMetrics?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Feature Name
            Text(
                text = formatContributor(featureName),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Quantitative Explanation
            if (todayMetrics != null && baselineMetrics != null) {
                val explanation = generateQuantitativeExplanation(
                    featureName = featureName,
                    todayMetrics = todayMetrics,
                    baselineMetrics = baselineMetrics
                )

                if (explanation != null) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                } else {
                    // Fallback to generic explanation
                    Text(
                        text = generateFeatureExplanation(featureName),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // No metrics available - show generic explanation
                Text(
                    text = generateFeatureExplanation(featureName),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Generates quantitative explanation comparing today vs baseline
 */
private fun generateQuantitativeExplanation(
    featureName: String,
    todayMetrics: TodayMetrics,
    baselineMetrics: BaselineMetrics
): String? {
    return when (featureName) {
        "total_sleep_minutes", "sleep_minutes" -> {
            val today = todayMetrics.sleepMinutes
            val baseline = baselineMetrics.avgSleepMinutes
            if (baseline == 0f) return null

            val diff = today - baseline
            val hours = kotlin.math.abs(diff / 60f)
            val minutes = kotlin.math.abs(diff % 60f).toInt()

            if (diff < 0) {
                "Your sleep was ${hours.toInt()}h ${minutes}m shorter than your baseline (${(baseline / 60).toInt()}h ${(baseline % 60).toInt()}m avg)."
            } else if (diff > 0) {
                "Your sleep was ${hours.toInt()}h ${minutes}m longer than your baseline (${(baseline / 60).toInt()}h ${(baseline % 60).toInt()}m avg)."
            } else {
                "Your sleep duration matched your baseline."
            }
        }

        "steps_total", "steps" -> {
            val today = todayMetrics.steps.toFloat()
            val baseline = baselineMetrics.avgSteps
            if (baseline == 0f) return null

            val multiplier = today / baseline
            val percent = ((today - baseline) / baseline * 100)
            val diff = kotlin.math.abs(today - baseline).toInt()

            when {
                today < baseline -> {
                    // Steps lower than baseline
                    if (kotlin.math.abs(percent) > 300) {
                        val inverseMultiplier = baseline / today
                        "Your step count was ${String.format(Locale.US, "%.1f", inverseMultiplier)}× lower than your usual level (${baseline.toInt()} avg)."
                    } else {
                        "Your step count was $diff steps lower (${kotlin.math.abs(percent).toInt()}% below baseline of ${baseline.toInt()})."
                    }
                }
                today > baseline -> {
                    // Steps higher than baseline
                    if (percent > 300) {
                        "Your step count was ${String.format(Locale.US, "%.1f", multiplier)}× your usual level (${baseline.toInt()} avg)."
                    } else {
                        "Your step count was $diff steps higher (${percent.toInt()}% above baseline of ${baseline.toInt()})."
                    }
                }
                else -> "Your step count matched your baseline."
            }
        }

        "calories_burned", "calories" -> {
            val today = todayMetrics.calories
            val baseline = baselineMetrics.avgCalories
            if (baseline == 0f) return null

            val multiplier = today / baseline
            val percent = ((today - baseline) / baseline * 100)
            val diff = kotlin.math.abs(today - baseline).toInt()

            when {
                today < baseline -> {
                    if (kotlin.math.abs(percent) > 300) {
                        val inverseMultiplier = baseline / today
                        "Calorie expenditure was ${String.format(Locale.US, "%.1f", inverseMultiplier)}× lower than usual."
                    } else {
                        "Calorie expenditure was ${diff} kcal lower (${kotlin.math.abs(percent).toInt()}% below baseline)."
                    }
                }
                today > baseline -> {
                    if (percent > 300) {
                        "Calorie expenditure was ${String.format(Locale.US, "%.1f", multiplier)}× your usual level."
                    } else {
                        "Calorie expenditure was ${diff} kcal higher (${percent.toInt()}% above baseline)."
                    }
                }
                else -> "Calorie expenditure matched your baseline."
            }
        }

        "avg_heart_rate", "heart_rate" -> {
            val today = todayMetrics.avgHeartRate
            val baseline = baselineMetrics.avgHeartRate
            if (baseline == 0f) return null

            val diff = (today - baseline).toInt()

            if (diff < 0) {
                "Average heart rate was ${kotlin.math.abs(diff)} bpm lower than baseline (${baseline.toInt()} bpm)."
            } else if (diff > 0) {
                "Average heart rate was ${diff} bpm higher than baseline (${baseline.toInt()} bpm)."
            } else {
                "Heart rate matched your baseline."
            }
        }

        "resting_heart_rate" -> {
            val today = todayMetrics.restingHeartRate
            val baseline = baselineMetrics.avgRestingHeartRate
            if (baseline == 0f) return null

            val diff = (today - baseline).toInt()

            if (diff < 0) {
                "Resting HR was ${kotlin.math.abs(diff)} bpm lower than baseline (${baseline.toInt()} bpm)."
            } else if (diff > 0) {
                "Resting HR was ${diff} bpm higher than baseline (${baseline.toInt()} bpm)."
            } else {
                "Resting heart rate matched your baseline."
            }
        }

        "sedentary_ratio" -> {
            "Your sedentary time pattern differed from your typical daily routine."
        }

        "movement_variance" -> {
            "Your movement pattern varied from your normal activity distribution."
        }

        "activity_load_index" -> {
            "Your overall activity intensity differed from your usual exertion level."
        }

        "sleep_consistency", "hr_variance" -> {
            // These are variance metrics - harder to explain quantitatively
            null
        }

        else -> null
    }
}

/**
 * Returns color based on drift level
 */
private fun getDriftLevelColor(level: String): Color {
    return when (level.lowercase()) {
        "low" -> Color(0xFF4CAF50)      // Green
        "medium" -> Color(0xFFFF9800)   // Orange
        "high" -> Color(0xFFF44336)     // Red
        else -> Color.Gray
    }
}

/**
 * Generates human-readable explanation for a feature contributor
 * Fallback for when quantitative comparison is not available
 */
private fun generateFeatureExplanation(featureName: String): String {
    return when (featureName) {
        "steps_total" -> "Your step count differs from your usual daily average"
        "sedentary_ratio" -> "Your sedentary time was higher or lower than your typical pattern"
        "movement_variance" -> "Your movement pattern differed from your normal routine"
        "total_sleep_minutes" -> "Your sleep duration deviated from your baseline"
        "avg_heart_rate" -> "Your average heart rate varied from your normal range"
        "resting_heart_rate" -> "Your resting heart rate is outside your typical range"
        "hr_variance" -> "Your heart rate variability shows unusual fluctuation"
        "calories_burned" -> "Your calorie expenditure differs from your usual activity level"
        "activity_load_index" -> "Your overall activity intensity varied from baseline"
        "sleep_consistency" -> "Your sleep schedule shows more variation than usual"
        "max_heart_rate" -> "Your maximum heart rate reached an unusual level"
        "distance_total" -> "Your total distance covered differs from your norm"
        "activity" -> "Your activity type or intensity varied from typical patterns"
        "is_sedentary" -> "Your sedentary periods differ from your usual behavior"
        else -> "This metric shows deviation from your personal baseline"
    }
}


/**
 * Converts API field names to human-readable labels
 */
private fun formatContributor(contributor: String): String {
    return when (contributor) {
        "total_sleep_minutes" -> "Sleep Duration"
        "steps_total" -> "Step Count"
        "calories_burned" -> "Calories Burned"
        "avg_heart_rate" -> "Heart Rate"
        "resting_heart_rate" -> "Resting HR"
        "hr_variance" -> "Heart Rate Variability"
        "max_heart_rate" -> "Max Heart Rate"
        "distance_total" -> "Distance"
        "activity" -> "Activity Type"
        "activity_load_index" -> "Activity Intensity"
        "sedentary_ratio" -> "Sedentary Time"
        "movement_variance" -> "Movement Pattern"
        "sleep_consistency" -> "Sleep Schedule"
        "is_sedentary" -> "Sedentary Behavior"
        else -> contributor.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
    }
}




