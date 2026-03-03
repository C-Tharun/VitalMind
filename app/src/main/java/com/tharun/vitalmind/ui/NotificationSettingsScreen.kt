package com.tharun.vitalmind.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * Settings screen for managing notification preferences
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
    }

    // State for each notification type
    var alertsEnabled by remember {
        mutableStateOf(prefs.getBoolean("alerts_enabled", true))
    }
    var recoveryEnabled by remember {
        mutableStateOf(prefs.getBoolean("recovery_enabled", true))
    }
    var remindersEnabled by remember {
        mutableStateOf(prefs.getBoolean("reminders_enabled", true))
    }
    var suggestionsEnabled by remember {
        mutableStateOf(prefs.getBoolean("suggestions_enabled", true))
    }

    // Save preference helper
    fun savePreference(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Notification Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Manage which types of health notifications you receive",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 🚨 Alerts Section
            NotificationCategoryCard(
                title = "🚨 Health Alerts",
                description = "Critical health warnings and anomaly detection",
                examples = listOf(
                    "Elevated heart rate",
                    "Low sleep detection",
                    "Activity below normal",
                    "High stress levels",
                    "Health deviation alerts"
                ),
                enabled = alertsEnabled,
                onToggle = { enabled ->
                    alertsEnabled = enabled
                    savePreference("alerts_enabled", enabled)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🟢 Recovery Section
            NotificationCategoryCard(
                title = "🟢 Recovery Notifications",
                description = "Positive health improvements and recoveries",
                examples = listOf(
                    "Heart rate normalized",
                    "Stress levels improved",
                    "Better sleep detected"
                ),
                enabled = recoveryEnabled,
                onToggle = { enabled ->
                    recoveryEnabled = enabled
                    savePreference("recovery_enabled", enabled)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🎯 Reminders Section
            NotificationCategoryCard(
                title = "🎯 Goal Reminders",
                description = "Daily goals and activity reminders",
                examples = listOf(
                    "Step goal progress",
                    "Inactivity alerts",
                    "Sleep debt warnings"
                ),
                enabled = remindersEnabled,
                onToggle = { enabled ->
                    remindersEnabled = enabled
                    savePreference("reminders_enabled", enabled)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 💡 Suggestions Section
            NotificationCategoryCard(
                title = "💡 Smart Suggestions",
                description = "Context-aware health tips and recommendations",
                examples = listOf(
                    "Good weather for outdoor activity",
                    "Optimal time for exercise"
                ),
                enabled = suggestionsEnabled,
                onToggle = { enabled ->
                    suggestionsEnabled = enabled
                    savePreference("suggestions_enabled", enabled)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ About Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Notifications are checked daily at 7 PM and every 6 hours. " +
                                "High-priority alerts are sent immediately when detected. " +
                                "You won't receive duplicate notifications for the same issue within 24 hours.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCategoryCard(
    title: String,
    description: String,
    examples: List<String>,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Examples:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }
    }
}

