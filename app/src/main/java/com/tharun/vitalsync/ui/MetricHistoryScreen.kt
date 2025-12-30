package com.tharun.vitalsync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.tharun.vitalsync.ui.theme.rememberChartStyle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricHistoryScreen(
    metricType: MetricType,
    navController: NavController,
    viewModel: MainViewModel
) {
    val historyData by viewModel.historyState.collectAsState()
    val initialTimeRange = if (metricType == MetricType.SLEEP) "7 Days" else "Today"
    var selectedTimeRange by remember { mutableStateOf(initialTimeRange) }

    LaunchedEffect(metricType, selectedTimeRange) {
        viewModel.loadHistory(metricType, selectedTimeRange)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "${metricType.name.lowercase().replaceFirstChar { it.uppercase() }} History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            val tabs = if (metricType == MetricType.SLEEP) {
                listOf("7 Days", "30 Days")
            } else {
                listOf("Today", "7 Days", "30 Days")
            }
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTimeRange),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                tabs.forEach {
                    title ->
                    Tab(selected = selectedTimeRange == title, onClick = { selectedTimeRange = title }) {
                        Text(title, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (historyData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history data available for this period.")
                }
            } else {
                val chartModelProducer = ChartEntryModelProducer(historyData.mapIndexed { index, data ->
                    val value = when(metricType) {
                        MetricType.HEART_RATE -> data.heartRate ?: 0f
                        MetricType.STEPS -> data.steps?.toFloat() ?: 0f
                        MetricType.CALORIES -> data.calories ?: 0f
                        MetricType.DISTANCE -> data.distance ?: 0f
                        MetricType.HEART_POINTS -> data.heartPoints?.toFloat() ?: 0f
                        MetricType.SLEEP -> (data.sleepDuration?.toFloat() ?: 0f) / 60f // Show hours in chart
                    }
                    entryOf(index.toFloat(), value)
                })

                val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, chartValues ->
                    try {
                        val dataPoint = historyData[value.toInt()]
                        val format = when(selectedTimeRange) {
                            "Today" -> if (metricType == MetricType.STEPS) "h a" else "h a"
                            else -> "d MMM"
                        }
                        SimpleDateFormat(format, Locale.getDefault()).format(Date(dataPoint.timestamp))
                    } catch (e: IndexOutOfBoundsException) {
                        ""
                    }
                }

                ProvideChartStyle(rememberChartStyle()) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                LineChart.LineSpec(
                                    lineColor = primaryColor.toArgb(),
                                    lineBackgroundShader = verticalGradient(
                                        arrayOf(
                                            primaryColor.copy(alpha = 0.5f),
                                            primaryColor.copy(alpha = 0f)
                                        )
                                    )
                                )
                            )
                        ),
                        chartModelProducer = chartModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter)
                    )
                }


                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(historyData) {
                        data ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            val format = when {
                                metricType == MetricType.SLEEP -> "EEE, d MMM"
                                metricType == MetricType.STEPS && selectedTimeRange != "Today" -> "EEE, d MMM"
                                selectedTimeRange == "Today" -> "h:mm a"
                                else -> "EEE, d MMM"
                            }
                            Text(
                                text = SimpleDateFormat(format, Locale.getDefault()).format(Date(data.timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            val valueText = when(metricType) {
                                MetricType.HEART_RATE -> data.heartRate?.let { "${String.format(Locale.US, "%.0f", it)} bpm" }
                                MetricType.STEPS -> data.steps?.let { "$it steps" }
                                MetricType.CALORIES -> data.calories?.let { "$it kcal" }
                                MetricType.DISTANCE -> data.distance?.let { "${String.format(Locale.US, "%.2f", it)} km" }
                                MetricType.SLEEP -> data.sleepDuration?.let { "${it / 60}h ${it % 60}m" }
                                else -> ""
                            }
                            Text(text = valueText ?: "", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Divider()
                    }
                }
            }
        }
    }
}
