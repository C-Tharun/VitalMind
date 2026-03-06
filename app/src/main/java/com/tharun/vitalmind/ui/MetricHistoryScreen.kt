package com.tharun.vitalmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.column.ColumnChart
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.tharun.vitalmind.data.HealthData
import com.tharun.vitalmind.ui.theme.rememberChartStyle
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricHistoryScreen(
    metricType: MetricType,
    navController: NavController,
    viewModel: MainViewModel
) {
    val historyData by viewModel.historyState.collectAsState()
    val heartRateHistory by viewModel.heartRateHistory.collectAsState()
    val stepsHistory by viewModel.stepsHistory.collectAsState()
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(metricType, selectedDate) {
        viewModel.loadHistory(metricType, selectedDate)
    }

    // Pre-calculate sleep data if needed
    val sleepDataCalc = remember(metricType, historyData, selectedDate) {
        if (metricType == MetricType.SLEEP) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedDate
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DATE, 1)
            val dayEnd = cal.timeInMillis

            // Filter sleep records where wake-up time falls within this day
            val selectedDateSleepData = historyData.filter { data ->
                val sleepEnd = data.timestamp + (data.sleepDuration ?: 0L) * 60 * 1000
                sleepEnd > dayStart && sleepEnd <= dayEnd
            }

            // Sum all sleep durations (entire sessions attributed to wake-up day)
            val selectedTotalSleep: Int = selectedDateSleepData.sumOf {
                it.sleepDuration ?: 0L
            }.toInt()

            Triple(dayStart, dayEnd, Pair(selectedDateSleepData, selectedTotalSleep))
        } else {
            null
        }
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
                        text = "${metricType.displayName} History",
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
                DateSelector(selectedDate = selectedDate, onDateSelected = { selectedDate = it })
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (metricType) {
                MetricType.HEART_RATE -> {
                    if (heartRateHistory.dailySummary == null && heartRateHistory.hourlyData.isEmpty() && heartRateHistory.rawData.isEmpty()) {
                        item {
                            EmptyStateCard(
                                emoji = "💓",
                                title = "No heart rate data",
                                subtitle = "Start tracking to see your heart rate history"
                            )
                        }
                    } else {
                        item {
                            heartRateHistory.dailySummary?.let { summary ->
                                DailyHeartRateSummary(summary)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        item {
                            if (heartRateHistory.hourlyData.isNotEmpty()) {
                                HourlyHeartRateChart(heartRateHistory.hourlyData)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        item {
                            Text(
                                "Readings (${heartRateHistory.rawData.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(heartRateHistory.rawData) { data ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(data.timestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${data.heartRate?.toInt() ?: 0}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF44336)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "bpm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                MetricType.STEPS -> {
                    if (stepsHistory.totalSteps == 0 && stepsHistory.chartData.isEmpty()) {
                        item {
                            EmptyStateCard(
                                emoji = "👟",
                                title = "No step data",
                                subtitle = "Start walking to track your steps"
                            )
                        }
                    } else {
                        // Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Daily Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "${stepsHistory.totalSteps}",
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9C27B0)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "steps",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Chart
                        item {
                            if (stepsHistory.chartData.isNotEmpty()) {
                                StepsBarChart(stepsHistory.chartData)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Section Header
                        item {
                            Text(
                                "Hourly Breakdown (${stepsHistory.listData.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // List Items
                        items(stepsHistory.listData) { data ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(data.timestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${data.steps}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9C27B0)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                MetricType.SLEEP -> {
                    val sleepInfo = sleepDataCalc ?: Triple(0L, 0L, Pair(emptyList(), 0))
                    val dayStart = sleepInfo.first
                    val dayEnd = sleepInfo.second
                    val selectedDateSleepData = sleepInfo.third.first
                    val selectedTotalSleep = sleepInfo.third.second

                    if (selectedTotalSleep == 0 && selectedDateSleepData.isEmpty()) {
                        item {
                            EmptyStateCard(
                                emoji = "😴",
                                title = "No sleep data",
                                subtitle = "Track your sleep to see patterns"
                            )
                        }
                    } else {
                        // Summary Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Total Sleep",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "${selectedTotalSleep / 60}h ${selectedTotalSleep % 60}m",
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                    }

                                    // Sleep Stage Duration Summary
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        "Sleep Stages",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Calculate duration for each stage
                                    val lightSleep = selectedDateSleepData.filter {
                                        it.activityType.equals("Light sleep", ignoreCase = true)
                                    }.sumOf { it.sleepDuration ?: 0L }

                                    val deepSleep = selectedDateSleepData.filter {
                                        it.activityType.equals("Deep sleep", ignoreCase = true)
                                    }.sumOf { it.sleepDuration ?: 0L }

                                    val remSleep = selectedDateSleepData.filter {
                                        it.activityType.equals("REM sleep", ignoreCase = true)
                                    }.sumOf { it.sleepDuration ?: 0L }

                                    val awake = selectedDateSleepData.filter {
                                        it.activityType.equals("Awake", ignoreCase = true)
                                    }.sumOf { it.sleepDuration ?: 0L }

                                    // Display stage durations
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (lightSleep > 0) {
                                            SleepStageDuration("Light", lightSleep, Color(0xFF81D4FA))
                                        }
                                        if (deepSleep > 0) {
                                            SleepStageDuration("Deep", deepSleep, Color(0xFF29B6F6))
                                        }
                                        if (remSleep > 0) {
                                            SleepStageDuration("REM", remSleep, Color(0xFF039BE5))
                                        }
                                        if (awake > 0) {
                                            SleepStageDuration("Awake", awake, Color(0xFFE0E0E0))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Chart
                        item {
                            if (selectedDateSleepData.isNotEmpty()) {
                                SleepStagesChart(sleepData = selectedDateSleepData)
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Section Header
                        item {
                            Text(
                                "Sessions (${selectedDateSleepData.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // List Items
                        items(selectedDateSleepData) { data ->
                            val sleepStart = data.timestamp
                            val overlap = overlapMinutes(data, dayStart, dayEnd)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(sleepStart)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${overlap / 60}h ${overlap % 60}m",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                }
                            }
                        }
                    }
                }

                MetricType.CALORIES, MetricType.DISTANCE -> {
                    if (historyData.isEmpty()) {
                        item {
                            val emoji = if (metricType == MetricType.CALORIES) "🔥" else "🗺️"
                            val title = if (metricType == MetricType.CALORIES) "No calorie data" else "No distance data"
                            EmptyStateCard(
                                emoji = emoji,
                                title = title,
                                subtitle = "Start tracking to see your history"
                            )
                        }
                    } else {
                        // Summary Card
                        item {
                            val totalValue = if (metricType == MetricType.CALORIES) {
                                historyData.sumOf { (it.calories ?: 0f).toDouble() }
                            } else {
                                historyData.sumOf { (it.distance ?: 0f).toDouble() }
                            }
                            val color = if (metricType == MetricType.CALORIES) Color(0xFF4CAF50) else Color(0xFF00BCD4)
                            val unit = if (metricType == MetricType.CALORIES) "kcal" else "km"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = color.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Daily Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            String.format(Locale.US, "%.1f", totalValue),
                                            fontSize = 40.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            unit,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Section Header
                        item {
                            Text(
                                "Entries (${historyData.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        // List Items
                        items(historyData) { data ->
                            val color = if (metricType == MetricType.CALORIES) Color(0xFF4CAF50) else Color(0xFF00BCD4)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(data.timestamp)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    val value = if (metricType == MetricType.CALORIES) {
                                        "${data.calories?.toInt() ?: 0}"
                                    } else {
                                        String.format(Locale.US, "%.2f", data.distance ?: 0f)
                                    }
                                    Text(
                                        text = value,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (metricType == MetricType.CALORIES) "kcal" else "km",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Handle ACTIVITY, WEIGHT, FLOORS_CLIMBED, MOVE_MINUTES
                    item {
                        EmptyStateCard(
                            emoji = "📊",
                            title = "No data available",
                            subtitle = "This metric is not yet supported"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(selectedDate: Long, onDateSelected: (Long) -> Unit) {
    val dates = (0..30).map {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -it)
        cal.timeInMillis
    }.asReversed()

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = dates.size - 1)

    LazyRow(state = listState, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        items(dates) { date ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = date
            val isSelected = cal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.DAY_OF_YEAR) &&
                    cal.get(Calendar.YEAR) == Calendar.getInstance().apply { timeInMillis = selectedDate }.get(Calendar.YEAR)

            Surface(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable { onDateSelected(date) }
                    .clip(RoundedCornerShape(16.dp)),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isSelected) 2.dp else 0.5.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("d", Locale.getDefault()).format(Date(date)),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(date)),
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun DailyHeartRateSummary(summary: HeartRateDailySummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Daily Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("${summary.min.toInt()}-${summary.max.toInt()} bpm", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (summary.max - summary.min) / (220f - 40f) }, // Example range
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HourlyHeartRateChart(hourlyData: List<HourlyHeartRateData>) {
    val chartModelProducer = ChartEntryModelProducer(
        hourlyData.mapIndexed { index, data -> entryOf(index.toFloat(), data.min) },
        hourlyData.mapIndexed { index, data -> entryOf(index.toFloat(), data.max - data.min) }
    )

    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        try {
            val dataPoint = hourlyData[value.toInt()]
            SimpleDateFormat("h a", Locale.getDefault()).format(Date(dataPoint.timestamp))
        } catch (_: IndexOutOfBoundsException) {
            ""
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Min/max heart rate per hour", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ProvideChartStyle(rememberChartStyle()) {
                Chart(
                    chart = columnChart(
                        columns = listOf(
                            LineComponent(
                                color = Color.Transparent.toArgb(),
                                thicknessDp = 8f
                            ),
                            LineComponent(
                                color = Color(0xFFF9844A).toArgb(), // Orange color similar to image
                                thicknessDp = 8f,
                                shape = Shapes.roundedCornerShape(allPercent = 50)
                            )
                        ),
                        mergeMode = ColumnChart.MergeMode.Stack
                    ),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter),
                )
            }
        }
    }
}

@Composable
fun StepsBarChart(chartData: List<HealthData>) {
    val chartModelProducer = ChartEntryModelProducer(
        chartData.mapIndexed { index, data ->
            entryOf(index.toFloat(), data.steps?.toFloat() ?: 0f)
        }
    )

    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        try {
            val dataPoint = chartData[value.toInt()]
            val cal = Calendar.getInstance()
            cal.timeInMillis = dataPoint.timestamp
            if (cal.get(Calendar.MINUTE) == 0) {
                SimpleDateFormat("h a", Locale.getDefault()).format(Date(dataPoint.timestamp))
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Steps per interval", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ProvideChartStyle(rememberChartStyle()) {
                Chart(
                    chart = columnChart(
                        columns = listOf(
                            LineComponent(
                                color = Color(0xFF4361EE).toArgb(), // Blue color
                                thicknessDp = 8f,
                                shape = Shapes.roundedCornerShape(topRightPercent = 50, topLeftPercent = 50)
                            )
                        )
                    ),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter),
                )
            }
        }
    }
}

// Helper function to calculate overlap in minutes between a sleep session and a day
fun overlapMinutes(data: HealthData, dayStart: Long, dayEnd: Long): Int {
    val sleepStart = data.timestamp
    var sleepEnd = data.timestamp + (data.sleepDuration ?: 0L) * 60 * 1000
    // Fix: If sleepEnd is before sleepStart, add 24 hours (in ms) to sleepEnd
    if (sleepEnd < sleepStart) {
        sleepEnd += 24 * 60 * 60 * 1000
    }
    val overlapStart = maxOf(sleepStart, dayStart)
    val overlapEnd = minOf(sleepEnd, dayEnd)
    return if (overlapEnd > overlapStart) ((overlapEnd - overlapStart) / 60000).toInt() else 0
}

@Composable
fun SleepStagesChart(sleepData: List<HealthData>) {
    val validSleepData = sleepData.filter { (it.sleepDuration ?: 0L) > 0 }
    if (validSleepData.isEmpty()) return

    val totalDuration = validSleepData.sumOf { it.sleepDuration ?: 0L }
    if (totalDuration == 0L) return

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sleep Stages", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(8.dp))) {
                validSleepData.sortedBy { it.timestamp }.forEach { segment ->
                    val stage = segment.activityType
                    val duration = segment.sleepDuration ?: 0L
                    val color = when (stage) {
                        "Awake" -> Color(0xFFE0E0E0)
                        "Light sleep" -> Color(0xFF81D4FA)
                        "Deep sleep" -> Color(0xFF29B6F6)
                        "REM sleep" -> Color(0xFF039BE5)
                        "Sleep" -> Color(0xFFB3E5FC)
                        else -> Color.Gray
                    }
                    Box(
                        modifier = Modifier
                            .weight(duration.toFloat())
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SleepStageLegendItem("Light", Color(0xFF81D4FA))
                SleepStageLegendItem("Deep", Color(0xFF29B6F6))
                SleepStageLegendItem("REM", Color(0xFF039BE5))
                SleepStageLegendItem("Awake", Color(0xFFE0E0E0))
            }
        }
    }
}

@Composable
fun SleepStageLegendItem(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(10.dp)
            .background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = name, fontSize = 12.sp)
    }
}

@Composable
fun EmptyStateCard(
    emoji: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        shape = RoundedCornerShape(24.dp),
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
            Text(
                emoji,
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SleepStageDuration(name: String, durationMinutes: Long, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${durationMinutes / 60}h ${durationMinutes % 60}m",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

val MetricType.displayName: String
    get() = when (this) {
        MetricType.STEPS -> "Steps"
        MetricType.HEART_RATE -> "Heart Rate"
        MetricType.CALORIES -> "Calories"
        MetricType.DISTANCE -> "Distance"
        MetricType.SLEEP -> "Sleep"
        MetricType.ACTIVITY -> "Activity"
        MetricType.WEIGHT -> "Weight"
        MetricType.FLOORS_CLIMBED -> "Floors Climbed"
        MetricType.MOVE_MINUTES -> "Move Minutes"
    }

