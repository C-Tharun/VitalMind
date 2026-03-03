package com.tharun.vitalmind.ui.stress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.data.remote.StressResponse
import java.util.Locale

@Composable
fun StressScoreCard(
    uiState: StressUiState,
    onCalculate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stress Score",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Evaluate your current physiological stress level",
                style = MaterialTheme.typography.bodyMedium
            )
            when (uiState) {
                is StressUiState.Idle -> {
                    Button(onClick = onCalculate) {
                        Text("Calculate Stress")
                    }
                }
                is StressUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is StressUiState.Success -> {
                    StressScoreResult(response = uiState.response)
                }
                is StressUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onCalculate) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun StressScoreResult(response: StressResponse) {
    // Determine color and emoji based on mood
    val moodColor = when (response.mood.lowercase()) {
        "relaxed" -> Color(0xFF4CAF50) // Green
        "alert" -> Color(0xFFFFA726) // Orange
        "stressed" -> Color(0xFFF44336) // Red
        else -> MaterialTheme.colorScheme.primary
    }

    val moodEmoji = when (response.mood.lowercase()) {
        "relaxed" -> "😌"
        "alert" -> "😐"
        "stressed" -> "😰"
        else -> "❓"
    }

    val moodDescription = when (response.mood.lowercase()) {
        "relaxed" -> "You're in a calm state"
        "alert" -> "Mild stress detected"
        "stressed" -> "High stress detected"
        else -> response.stress_status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = moodColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top: Mood Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emoji Circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(moodColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = moodEmoji,
                        fontSize = 36.sp
                    )
                }

                // Mood Text
                Column {
                    Text(
                        text = response.mood,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = moodColor
                    )
                    Text(
                        text = moodDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = moodColor.copy(alpha = 0.3f))

            // Bottom: Additional Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = moodColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = response.stress_status,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = moodColor
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Stability",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = response.stress_stability,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Confidence Score (de-emphasized)
            Text(
                text = "Confidence: ${String.format(Locale.getDefault(), "%.1f", response.stress_score)}% • Level: ${response.stress_level}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

