package com.tharun.vitalmind.ui.stress

import androidx.compose.foundation.clickable // Added missing import
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.tharun.vitalmind.data.StressScoreHistory
import com.tharun.vitalmind.ui.theme.rememberChartStyle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StressHistoryScreen(viewModel: StressHistoryViewModel, navController: NavController? = null) {
    val history by viewModel.history.collectAsState()
    var selectedDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val selectedDayEntries = history.filter {
        val cal = Calendar.getInstance()
        cal.timeInMillis = selectedDate
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.add(Calendar.DATE, 1)
        val dayEnd = cal.timeInMillis
        it.timestamp in dayStart until dayEnd
    }

    val avgScore = if (selectedDayEntries.isNotEmpty()) selectedDayEntries.map { it.stress_score }.average() else 0.0
    val minScore = selectedDayEntries.minOfOrNull { it.stress_score } ?: 0f
    val maxScore = selectedDayEntries.maxOfOrNull { it.stress_score } ?: 0f

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (navController != null) {
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
                    }
                    Text(
                        text = "Stress Score History",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            DateSelector(selectedDate = selectedDate, onDateSelected = { selectedDate = it })
            Spacer(modifier = Modifier.height(16.dp))
            if (selectedDayEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No stress score history available for this period.")
                }
            } else {
                // Summary
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Average Score: " + String.format(Locale.getDefault(), "%.2f", avgScore), fontWeight = FontWeight.Bold)
                        Text("Min: ${minScore}", style = MaterialTheme.typography.bodyMedium)
                        Text("Max: ${maxScore}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Chart
                if (selectedDayEntries.size > 1) {
                    val chartModelProducer = ChartEntryModelProducer(selectedDayEntries.mapIndexed { index, item -> entryOf(index.toFloat(), item.stress_score) })
                    ProvideChartStyle(rememberChartStyle()) {
                        Chart(
                            chart = columnChart(
                                columns = listOf(
                                    LineComponent(
                                        color = Color(0xFF4361EE).value.toInt(), // Use value.toInt() for Int type
                                        thicknessDp = 8f,
                                        shape = Shapes.roundedCornerShape(topRightPercent = 50, topLeftPercent = 50)
                                    )
                                )
                            ),
                            chartModelProducer = chartModelProducer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // List
                LazyColumn {
                    items(selectedDayEntries) { item ->
                        StressHistoryCard(item)
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
fun StressHistoryCard(item: StressScoreHistory) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Score: ${item.stress_score}", fontWeight = FontWeight.Bold)
            Text("Level: ${item.stress_level}")
            Text("Status: ${item.stress_status}")
            Text("Stability: ${item.stress_stability}")
            Text("Mood: ${item.mood}")
            Text("Time: ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.timestamp))}")
        }
    }
}
