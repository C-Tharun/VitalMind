package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onAnalyze: () -> Unit
) {
    // Log current state for debugging
    LaunchedEffect(uiState) {
        Log.d("HealthDeviationCard", "📱 Current UI State: ${uiState.javaClass.simpleName}")
        if (uiState is HealthDeviationUiStateExtended.Error) {
            Log.e("HealthDeviationCard", "Error state message: ${uiState.message}")
        }
    }

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
                    HealthDeviationResult(response = uiState.response)
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
                                onAnalyze()
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
            progress = progress,
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
 */
@Composable
private fun HealthDeviationResult(response: HealthDeviationResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Enhanced Deviation Score Visualization
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", response.health_deviation_score),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = getDriftLevelColor(response.stress_drift_level)
                )
                Text(
                    text = "Deviation Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Drift Level Badge
                Surface(
                    color = getDriftLevelColor(response.stress_drift_level),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = response.stress_drift_level.uppercase(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Human-Readable Interpretation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "💡",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = getDriftLevelInterpretation(response.stress_drift_level),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // Confidence Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Analysis Confidence:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(response.confidence * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (response.confidence >= 0.8f) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                if (response.confidence < 1.0f) {
                    Text(
                        text = " ⚠️",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // Partial data caption
        if (response.confidence < 1.0f) {
            Text(
                text = "Based on partial data — sync more health metrics for better accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Feature-Specific Explanations
        if (response.top_contributors.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Key Contributing Factors:",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                response.top_contributors.take(3).forEach { contributor ->
                    FeatureExplanationItem(featureName = contributor)
                }
            }
        }
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
 * Returns human-readable interpretation based on drift level
 */
private fun getDriftLevelInterpretation(level: String): String {
    return when (level.lowercase()) {
        "low" -> "Your health metrics are consistent with your usual baseline patterns. Great job maintaining your routine!"
        "medium" -> "Some of your physiological metrics show moderate deviation from your personal baseline. This is normal variation, but consider checking your recent habits."
        "high" -> "Your current health patterns significantly differ from your baseline. Consider reviewing sleep quality, movement patterns, and recovery time. Small adjustments can help."
        else -> "Analysis completed based on your personalized baseline model."
    }
}

/**
 * Generates human-readable explanation for a feature contributor
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
 * Displays a single feature explanation with icon and text
 */
@Composable
private fun FeatureExplanationItem(featureName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatContributor(featureName),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = generateFeatureExplanation(featureName),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
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




