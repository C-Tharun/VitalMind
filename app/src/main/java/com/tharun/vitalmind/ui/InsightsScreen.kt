package com.tharun.vitalmind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val insights = remember(state) {
        listOf(
            InsightCardData(
                icon = Icons.Filled.DirectionsWalk,
                title = "Activity Insight",
                message = if ((state.steps.toIntOrNull() ?: 0) < 5000) "You have been less active today compared to your weekly average." else "You are maintaining good consistency in your daily steps.",
                caption = "Based on your recent activity history"
            ),
            InsightCardData(
                icon = Icons.Filled.Hotel,
                title = "Sleep Insight",
                message = if ((state.sleepDuration.takeIf { it != "--" }?.substringBefore("h")?.toIntOrNull() ?: 0) < 7) "Your sleep duration was lower than usual last night." else "You are getting a healthy amount of sleep.",
                caption = "Based on your recent sleep history"
            ),
            InsightCardData(
                icon = Icons.Filled.TrendingUp,
                title = "Motivation",
                message = "It looks like a good time for a short walk or cycling.",
                caption = "Based on your recent activity history"
            )
        )
    }
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(insights.size) { idx ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        InsightCard(insights[idx])
                    }
                }
            }
        }
    }
}

data class InsightCardData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val caption: String
)

@Composable
fun InsightCard(data: InsightCardData) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                data.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(data.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(data.message, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(data.caption, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
