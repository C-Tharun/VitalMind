package com.tharun.vitalmind.ui.healthdeviation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tharun.vitalmind.data.HealthDeviationHistory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDeviationHistoryScreen(
    viewModel: HealthDeviationHistoryViewModel,
    navController: NavController? = null
) {
    val history by viewModel.history.collectAsState()
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Filter entries for selected date
    val selectedDateEntries = remember(history, selectedDate) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetDate = dateFormat.format(Date(selectedDate))
        history.filter { it.date == targetDate }
    }

    // Get latest entry for the day (or overall if no date selection)
    val latestEntry = selectedDateEntries.firstOrNull()

    // Count drift levels for summary
    val lowCount = history.count { it.drift_level == "Low" }
    val mediumCount = history.count { it.drift_level == "Medium" }
    val highCount = history.count { it.drift_level == "High" }

    // Average scores
    val avgScore = if (history.isNotEmpty()) history.map { it.deviation_score }.average().toFloat() else 0f
    val avgConfidence = if (history.isNotEmpty()) history.map { it.confidence }.average().toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Health Deviation History", fontWeight = FontWeight.Bold)
                        Text(
                            "Last 30 Days Analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Select Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable date picker
                        DateSelector(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it },
                            availableDates = history.map {
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.date)?.time ?: 0L
                            }.distinct()
                        )
                    }
                }
            }

            // Summary Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "30-Day Overview",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Drift Level Distribution
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DriftLevelSummary(
                                emoji = "🟢",
                                label = "Low",
                                count = lowCount,
                                color = Color(0xFF4CAF50)
                            )
                            DriftLevelSummary(
                                emoji = "🟡",
                                label = "Medium",
                                count = mediumCount,
                                color = Color(0xFFFF9800)
                            )
                            DriftLevelSummary(
                                emoji = "🔴",
                                label = "High",
                                count = highCount,
                                color = Color(0xFFF44336)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(20.dp))

                        // Average Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(
                                label = "Avg Score",
                                value = String.format("%.1f", avgScore),
                                icon = Icons.Default.Timeline
                            )
                            StatItem(
                                label = "Avg Confidence",
                                value = "${(avgConfidence * 100).toInt()}%",
                                icon = Icons.Default.Verified
                            )
                            StatItem(
                                label = "Total Analyses",
                                value = history.size.toString(),
                                icon = Icons.Default.BarChart
                            )
                        }
                    }
                }
            }

            // Trend Visualization - only show if we have at least 2 valid data points
            if (history.size >= 2) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Deviation Score Trend",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Custom simple bar visualization - no external chart library
                            val validScores = remember(history) {
                                history
                                    .filter { it.deviation_score > 0 && it.deviation_score.isFinite() }
                                    .sortedBy { it.timestamp }
                                    .takeLast(10) // Show last 10 entries
                            }

                            if (validScores.isNotEmpty()) {
                                val maxScore = validScores.maxOf { it.deviation_score }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    verticalArrangement = Arrangement.Bottom
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        validScores.forEach { entry ->
                                            val barHeight = if (maxScore > 0) {
                                                (entry.deviation_score / maxScore).coerceIn(0f, 1f)
                                            } else 0f

                                            val barColor = when (entry.drift_level) {
                                                "Low" -> Color(0xFF4CAF50)
                                                "Medium" -> Color(0xFFFF9800)
                                                "High" -> Color(0xFFF44336)
                                                else -> MaterialTheme.colorScheme.primary
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 2.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .fillMaxHeight(barHeight)
                                                        .background(
                                                            barColor,
                                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                                        )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Simple legend
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Last ${validScores.size} analyses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No valid data to display",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Date Details
            if (latestEntry != null) {
                item {
                    DeviationDetailCard(entry = latestEntry)
                }
            } else if (selectedDateEntries.isEmpty() && history.isNotEmpty()) {
                item {
                    EmptyDateCard()
                }
            }

            // History List
            item {
                Text(
                    "All Analyses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (history.isEmpty()) {
                item {
                    EmptyHistoryCard()
                }
            } else {
                items(history) { entry ->
                    DeviationHistoryItem(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    availableDates: List<Long>
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
    val dayFormat = SimpleDateFormat("EEE", Locale.US)

    // Generate last 30 days
    val dates = remember {
        (0 until 30).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -daysAgo)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }.reversed()
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dates) { date ->
            val isSelected = isSameDay(date, selectedDate)
            val hasData = availableDates.any { isSameDay(it, date) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            hasData -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .then(
                        if (hasData) {
                            Modifier.clickable { onDateSelected(date) }
                        } else Modifier
                    )
            ) {
                Text(
                    dayFormat.format(Date(date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasData) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    dateFormat.format(Date(date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (hasData && !isSelected) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun DriftLevelSummary(
    emoji: String,
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviationDetailCard(entry: HealthDeviationHistory) {
    val driftColor = when (entry.drift_level) {
        "Low" -> Color(0xFF4CAF50)
        "Medium" -> Color(0xFFFF9800)
        "High" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    val driftEmoji = when (entry.drift_level) {
        "Low" -> "🟢"
        "Medium" -> "🟡"
        "High" -> "🔴"
        else -> "⚪"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = driftColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(driftEmoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "${entry.drift_level} Drift",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = driftColor
                        )
                        Text(
                            SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.US).format(Date(entry.timestamp)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        String.format("%.2f", entry.deviation_score),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = driftColor
                    )
                    Text(
                        "Score",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = driftColor.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricBadge(
                    icon = Icons.Default.DirectionsWalk,
                    label = "Steps",
                    value = entry.steps.toString()
                )
                MetricBadge(
                    icon = Icons.Default.Bedtime,
                    label = "Sleep",
                    value = "${entry.sleep_minutes / 60}h ${entry.sleep_minutes % 60}m"
                )
                MetricBadge(
                    icon = Icons.Default.Favorite,
                    label = "Avg HR",
                    value = "${entry.avg_heart_rate.toInt()} bpm"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confidence Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Confidence: ${(entry.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                LinearProgressIndicator(
                    progress = { entry.confidence },
                    modifier = Modifier
                        .width(100.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Top Contributors
            if (entry.top_contributors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Top Contributors:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val contributors = entry.top_contributors.split(",").take(3)
                contributors.forEach { contributor ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(driftColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatContributorName(contributor),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeviationHistoryItem(entry: HealthDeviationHistory) {
    val driftColor = when (entry.drift_level) {
        "Low" -> Color(0xFF4CAF50)
        "Medium" -> Color(0xFFFF9800)
        "High" -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(driftColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        String.format("%.1f", entry.deviation_score),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = driftColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        SimpleDateFormat("HH:mm", Locale.US).format(Date(entry.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.drift_level,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = driftColor
                )
                Text(
                    "${(entry.confidence * 100).toInt()}% conf.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyDateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📅", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Analysis for This Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select a different date to view analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Analysis History Yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Perform a health deviation analysis to see your history here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatContributorName(contributor: String): String {
    return when (contributor.trim()) {
        "total_sleep_minutes", "sleep_minutes" -> "Sleep Duration"
        "steps_total", "steps" -> "Steps"
        "avg_heart_rate" -> "Avg Heart Rate"
        "resting_heart_rate" -> "Resting Heart Rate"
        "hr_variance" -> "HR Variability"
        "calories_burned", "calories" -> "Calories"
        "sedentary_ratio" -> "Sedentary Time"
        "movement_variance" -> "Movement Patterns"
        "activity_load_index" -> "Activity Intensity"
        "sleep_consistency" -> "Sleep Consistency"
        else -> contributor.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}








