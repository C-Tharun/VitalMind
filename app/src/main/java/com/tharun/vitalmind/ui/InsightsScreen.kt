package com.tharun.vitalmind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import com.tharun.vitalmind.ui.MetricType
import androidx.compose.material.icons.filled.Info
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val baselineInsights by viewModel.baselineInsights.collectAsState()
    val aiExplanations by viewModel.aiExplanations.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val aiExpanded = remember { mutableStateMapOf<Int, Boolean>() }
    // Trigger baseline computation on load
    LaunchedEffect(Unit) { viewModel.computeBaselineInsights() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Insights") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "AI-powered insights (prototype)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Health vs Your Normal",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(baselineInsights.size) { idx ->
                    val insight = baselineInsights[idx]
                    val icon = when (insight.metric) {
                        MetricType.STEPS -> Icons.Filled.DirectionsWalk
                        MetricType.SLEEP -> Icons.Filled.Hotel
                        MetricType.CALORIES -> Icons.Filled.LocalFireDepartment
                        else -> Icons.Filled.Info
                    }
                    val deviationMsg = when {
                        insight.status == "Consistent" -> "Consistent with your usual ${insight.metricName.lowercase()}"
                        insight.deviationPercent < 0 -> "${kotlin.math.abs(insight.deviationPercent).toInt()}% below your usual ${insight.metricName.lowercase()}"
                        insight.deviationPercent > 0 -> "${insight.deviationPercent.toInt()}% above your usual ${insight.metricName.lowercase()}"
                        else -> "Consistent with your usual ${insight.metricName.lowercase()}"
                    }
                    InsightCardWithExplain(
                        icon = icon,
                        title = insight.metricName,
                        message = deviationMsg,
                        caption = "Compared against your 7-day personal baseline",
                        aiExplanation = aiExplanations[idx],
                        aiExpanded = aiExpanded[idx] == true,
                        onExplain = {
                            val prompt = "Explain in simple, non-medical language why a ${insight.deviationPercent.toInt()}% ${if (insight.deviationPercent < 0) "drop" else "increase"} in daily ${insight.metricName.lowercase()} compared to personal average may matter. Baseline: ${insight.baseline.toInt()}, Today: ${insight.todayValue.toInt()}"
                            android.util.Log.d("VitalMind", "prompt sent to ai: $prompt")
                            viewModel.requestAIExplanation(idx, prompt)
                            aiExpanded[idx] = true
                        },
                        onExpandToggle = { aiExpanded[idx] = !(aiExpanded[idx] ?: false) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI-generated explanation (prototype)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun InsightCardWithExplain(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    caption: String,
    aiExplanation: String?,
    aiExpanded: Boolean,
    onExplain: () -> Unit,
    onExpandToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(caption, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
                }
            }
            // Move Explain button to its own row for visibility
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onExplain
                ) {
                    Text("Explain")
                }
            }
            if (aiExplanation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onExpandToggle) {
                    Text(if (aiExpanded) "Hide explanation" else "Show explanation")
                }
                AnimatedVisibility(visible = aiExpanded) {
                    Text(
                        aiExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
