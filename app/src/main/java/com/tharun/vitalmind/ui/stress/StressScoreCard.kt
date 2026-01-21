package com.tharun.vitalmind.ui.stress

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tharun.vitalmind.data.remote.StressResponse

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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Score: ${response.stress_score}", fontWeight = FontWeight.SemiBold)
        Text("Level: ${response.stress_level}")
        Text("Status: ${response.stress_status}")
        Text("Stability: ${response.stress_stability}")
        Text("Mood: ${response.mood}")
    }
}

