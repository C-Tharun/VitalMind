package com.tharun.vitalmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val historyData by viewModel.historyState.collectAsState()
    val selectedDate = rememberSaveable { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(selectedDate.value) {
        viewModel.loadHistory(MetricType.ACTIVITY, selectedDate.value)
    }

    // Group activities by type
    val activityCounts = remember(historyData) {
        historyData.groupBy { it.activityType ?: "Unknown" }
            .mapValues { it.value.size }
    }

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Activity History",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Date Selector
            item {
                DateSelector(selectedDate = selectedDate.value, onDateSelected = { selectedDate.value = it })
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (historyData.isEmpty()) {
                item {
                    EmptyStateCard(
                        emoji = "🏃",
                        title = "No activities recorded",
                        subtitle = "Start tracking to see your activity history"
                    )
                }
            } else {
                // Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Daily Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "${historyData.size}",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "activities",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            if (activityCounts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Activity Breakdown",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activityCounts.entries.take(5).forEach { (type, count) ->
                                        ActivityTypeSummary(type, count)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Section Header
                item {
                    Text(
                        "All Activities (${historyData.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Activity Cards
                items(historyData) { activity ->
                    ActivityCard(activity)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityTypeSummary(type: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(getActivityColor(type), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                type,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = getActivityColor(type)
        )
    }
}

@Composable
fun ActivityCard(activity: com.tharun.vitalmind.data.HealthData) {
    val activityType = activity.activityType ?: "Unknown"
    val color = getActivityColor(activityType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getActivityEmoji(activityType),
                    fontSize = 24.sp
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activityType,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = color
                )
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(activity.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getActivityColor(activityType: String): Color {
    return when (activityType.lowercase()) {
        "walking", "walk" -> Color(0xFF4CAF50)
        "running", "run" -> Color(0xFFF44336)
        "cycling", "bike", "biking" -> Color(0xFF2196F3)
        "swimming" -> Color(0xFF00BCD4)
        "hiking" -> Color(0xFF8BC34A)
        "yoga" -> Color(0xFF9C27B0)
        "gym", "workout", "exercise" -> Color(0xFFFF9800)
        "sleep", "light sleep", "deep sleep", "rem sleep" -> Color(0xFF3F51B5)
        else -> Color(0xFF607D8B)
    }
}

fun getActivityEmoji(activityType: String): String {
    return when (activityType.lowercase()) {
        "walking", "walk" -> "🚶"
        "running", "run" -> "🏃"
        "cycling", "bike", "biking" -> "🚴"
        "swimming" -> "🏊"
        "hiking" -> "🥾"
        "yoga" -> "🧘"
        "gym", "workout", "exercise" -> "💪"
        "sleep", "light sleep", "deep sleep", "rem sleep", "awake" -> "😴"
        else -> "🏃"
    }
}