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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.data.remote.HealthDeviationResponse
import java.util.Locale

/**
 * Health Deviation Card - displays PHBD-Net analysis results
 */
@Composable
fun HealthDeviationCard(
    uiState: HealthDeviationUiState,
    onAnalyze: () -> Unit
) {
    // Log current state for debugging
    LaunchedEffect(uiState) {
        Log.d("HealthDeviationCard", "📱 Current UI State: ${uiState.javaClass.simpleName}")
        if (uiState is HealthDeviationUiState.Error) {
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
                is HealthDeviationUiState.Idle -> {
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
                is HealthDeviationUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                    )
                }
                is HealthDeviationUiState.Success -> {
                    HealthDeviationResult(response = uiState.response)
                }
                is HealthDeviationUiState.Error -> {
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
 * Displays the health deviation analysis results
 */
@Composable
private fun HealthDeviationResult(response: HealthDeviationResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Health Deviation Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deviation Score:",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = String.format(Locale.US, "%.1f", response.health_deviation_score),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Stress Drift Level with color coding
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stress Drift Level:",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = response.stress_drift_level,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = getDriftLevelColor(response.stress_drift_level)
            )
        }

        // Confidence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Confidence:",
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${(response.confidence * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Partial data caption if confidence < 1.0
        if (response.confidence < 1.0f) {
            Text(
                text = "⚠ Based on partial data",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }

        // Top Contributors
        if (response.top_contributors.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Top Contributors:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = response.top_contributors.joinToString(", ") { formatContributor(it) },
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp
            )
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
 * Converts API field names to human-readable labels
 */
private fun formatContributor(contributor: String): String {
    return when (contributor) {
        "total_sleep_minutes" -> "Sleep"
        "steps_total" -> "Steps"
        "calories_burned" -> "Calories"
        "avg_heart_rate" -> "Heart Rate"
        "max_heart_rate" -> "Max HR"
        "distance_total" -> "Distance"
        "activity" -> "Activity"
        "is_sedentary" -> "Sedentary Time"
        else -> contributor.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
    }
}




