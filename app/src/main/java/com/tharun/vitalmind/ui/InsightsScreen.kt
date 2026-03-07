package com.tharun.vitalmind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: MainViewModel, navController: NavController? = null, listState: LazyListState) {
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
    // Prepare context when weather or baseline changes
    LaunchedEffect(weather, baselineInsights) {
        viewModel.prepareRecommendationContext()
    }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Insights",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Health Analysis",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "AI-powered insights from your data",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Insights Section
            item {
                SectionHeader(title = "Your Health vs Your Normal", icon = Icons.AutoMirrored.Filled.TrendingUp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Insight Cards
            items(baselineInsights.size) { idx ->
                val insight = baselineInsights[idx]
                val icon = when (insight.metric) {
                    MetricType.STEPS -> Icons.AutoMirrored.Filled.DirectionsWalk
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
                ModernInsightCard(
                    icon = icon,
                    title = insight.metricName,
                    message = deviationMsg,
                    caption = "Compared against your 7-day personal baseline",
                    deviationPercent = insight.deviationPercent,
                    aiExplanation = aiExplanations[idx],
                    aiExpanded = aiExpanded[idx] == true,
                    onExplain = {
                        // Format values appropriately for each metric
                        val (todayFormatted, baselineFormatted) = when (insight.metric) {
                            MetricType.SLEEP -> {
                                // Sleep is in minutes, convert to hours for display
                                val todayHours = "%.1f hours".format(insight.todayValue / 60f)
                                val baselineHours = "%.1f hours".format(insight.baseline / 60f)
                                Pair(todayHours, baselineHours)
                            }
                            MetricType.STEPS -> {
                                Pair("${insight.todayValue.toInt()} steps", "${insight.baseline.toInt()} steps")
                            }
                            MetricType.CALORIES -> {
                                Pair("${insight.todayValue.toInt()} kcal", "${insight.baseline.toInt()} kcal")
                            }
                            else -> {
                                Pair(insight.todayValue.toInt().toString(), insight.baseline.toInt().toString())
                            }
                        }
                        val prompt = "Explain in simple, non-medical language why a ${kotlin.math.abs(insight.deviationPercent).toInt()}% ${if (insight.deviationPercent < 0) "drop" else "increase"} in daily ${insight.metricName.lowercase()} compared to personal average may matter. Baseline: $baselineFormatted, Today: $todayFormatted"
                        viewModel.requestAIExplanation(idx, prompt)
                        aiExpanded[idx] = true
                    },
                    onExpandToggle = { aiExpanded[idx] = !(aiExpanded[idx] ?: false) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Stress Terrain Map Section
            item {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(title = "Stress Terrain Map", icon = Icons.Default.Map)
                Spacer(modifier = Modifier.height(12.dp))
                StressTerrainCard(navController)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // AI Recommendation Section
            item {
                SectionHeader(title = "AI Recommendations", icon = Icons.Default.SmartToy)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Weather & Location Card
            item {
                val currentWeather = weather
                val currentContext = recommendationContext
                if (currentWeather != null && currentContext != null) {
                    WeatherLocationCard(
                        location = currentWeather.location.name,
                        temperature = currentContext.temperatureC ?: 0f,
                        weatherCondition = currentContext.weatherCondition ?: "Unknown",
                        aqi = currentContext.aqi ?: "Unknown"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                AIRecommendationCard(
                    recommendation = aiRecommendation,
                    onGenerate = {
                        hasRequestedRecommendation = true
                        coroutineScope.launch {
                            viewModel.generateAIRecommendation()
                        }
                    },
                    isLoading = hasRequestedRecommendation && aiRecommendation == null
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ModernInsightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    caption: String,
    deviationPercent: Float,
    aiExplanation: String?,
    aiExpanded: Boolean,
    onExplain: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val statusIcon = when {
        kotlin.math.abs(deviationPercent) < 10 -> Icons.Default.CheckCircle
        deviationPercent > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.AutoMirrored.Filled.TrendingDown
    }
    val statusColor = when {
        kotlin.math.abs(deviationPercent) < 10 -> Color(0xFF4CAF50)
        kotlin.math.abs(deviationPercent) < 25 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Caption
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            // AI Explanation Section
            if (aiExplanation == null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onExplain,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.SmartToy,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ask AI to Explain")
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExpandToggle() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = "AI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "AI Explanation",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                if (aiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(visible = aiExpanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    aiExplanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
fun StressTerrainCard(navController: NavController?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Stress Terrain Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Visualize stress patterns by location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Uses historical heart rate data to identify stress zones and calming locations. No real-time tracking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController?.navigate("stress_terrain_map") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "View Map",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Stress Map")
                }
            }
        }
    }
}

@Composable
fun AIRecommendationCard(
    recommendation: String?,
    onGenerate: () -> Unit,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "Personalized Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "AI-generated health advice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (recommendation != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate New Recommendation")
                    }
                } else {
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = "Generate",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Recommendation")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Recommendations are AI-generated and should not replace medical advice",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun WeatherLocationCard(
    location: String,
    temperature: Float,
    weatherCondition: String,
    aqi: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weather Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getWeatherEmoji(weatherCondition),
                    fontSize = 32.sp
                )
            }

            // Weather Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${temperature.toInt()}°C",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        weatherCondition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Air Quality
                Surface(
                    color = getAQIColor(aqi).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Air Quality: ${aqi.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getAQIColor(aqi)
                    )
                }
            }
        }
    }
}

fun getWeatherEmoji(condition: String): String {
    return when {
        condition.contains("sunny", ignoreCase = true) -> "☀️"
        condition.contains("clear", ignoreCase = true) -> "🌤️"
        condition.contains("cloud", ignoreCase = true) -> "☁️"
        condition.contains("rain", ignoreCase = true) -> "🌧️"
        condition.contains("storm", ignoreCase = true) -> "⛈️"
        condition.contains("snow", ignoreCase = true) -> "❄️"
        condition.contains("fog", ignoreCase = true) -> "🌫️"
        else -> "🌡️"
    }
}

fun getAQIColor(aqi: String): Color {
    return when (aqi.lowercase()) {
        "good" -> Color(0xFF4CAF50)
        "moderate" -> Color(0xFFFFA726)
        "poor", "unhealthy" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
}
