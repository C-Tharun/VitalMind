package com.tharun.vitalmind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import com.tharun.vitalmind.ui.MetricType
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: MainViewModel, navController: NavController? = null) {
    val baselineInsights by viewModel.baselineInsights.collectAsState()
    val aiExplanations by viewModel.aiExplanations.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val aiRecommendation by viewModel.aiRecommendation.collectAsState()
    val aiExpanded = remember { mutableStateMapOf<Int, Boolean>() }
    val recommendationContext by viewModel.recommendationContext.collectAsState()
    var hasRequestedRecommendation by remember { mutableStateOf(false) }

    // Trigger baseline computation on load
    LaunchedEffect(Unit) {
        viewModel.fetchWeatherIfNeeded()
        viewModel.computeBaselineInsights()
    }
    // Prepare context when weather or baseline changes (but don't auto-generate recommendation)
    LaunchedEffect(weather, baselineInsights) {
        viewModel.prepareRecommendationContext()
    }

    val coroutineScope = rememberCoroutineScope()
    val isLoading = aiRecommendation == null

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
                        onExpandToggle = { aiExpanded[idx] = !(aiExpanded[idx] ?: false) },
                        showAiIcon = true // Show AI icon for cards with AI explanation
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI-generated explanation (prototype)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
                // Stress Terrain Map section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "🗺️ Stress Terrain Map",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Visualize your physiological stress patterns across locations.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = "Uses historical heart rate data to identify stress zones and calming locations. No real-time tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = {
                                    navController?.navigate("stress_terrain_map")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = "View Map",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View Map")
                            }
                        }
                    }
                }
                // Smart AI Recommendations section as the last item in the LazyColumn
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "\uD83E\uDD14 Smart AI Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!hasRequestedRecommendation && aiRecommendation == null) {
                                // Show button to request recommendation
                                Button(
                                    onClick = {
                                        hasRequestedRecommendation = true
                                        coroutineScope.launch {
                                            viewModel.generateAIRecommendation()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.SmartToy,
                                        contentDescription = "AI",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Click here for AI recommendations", style = MaterialTheme.typography.labelLarge)
                                }
                            } else {
                                // Show recommendation with refresh button
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.SmartToy,
                                        contentDescription = "AI",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiRecommendation ?: "Loading recommendation...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // Refresh icon
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            viewModel.generateAIRecommendation()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Recommendation",
                                            tint = if (isLoading) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                if (isLoading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // Weather info widget
                            val w = weather
                            if (w != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(w.location.name, style = MaterialTheme.typography.labelLarge)
                                            Text(w.current.condition.text, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${w.current.temp_c}°C", style = MaterialTheme.typography.titleMedium)
                                            w.current.airQuality?.usEpaIndex?.let {
                                                val aqiLabel = when (it) {
                                                    1, 2 -> "Good"
                                                    3 -> "Moderate"
                                                    4, 5 -> "Unhealthy"
                                                    6 -> "Hazardous"
                                                    else -> "Unknown"
                                                }
                                                Text("AQI: $aqiLabel", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AI-generated recommendation (prototype)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
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
    onExpandToggle: () -> Unit,
    showAiIcon: Boolean = false
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium)
                        if (showAiIcon) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Explain")
                }
            }
            if (aiExplanation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onExpandToggle) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (aiExpanded) "Hide explanation" else "Show explanation")
                }
                AnimatedVisibility(visible = aiExpanded) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
}
