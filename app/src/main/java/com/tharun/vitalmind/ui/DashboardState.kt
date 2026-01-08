package com.tharun.vitalmind.ui

data class DashboardState(
    val userId: String = "",
    val userName: String = "User",
    val heartRate: String = "--",
    val calories: String = "--",
    val steps: String = "--",
    val distance: String = "--",
    val sleepDuration: String = "--",
    val lastActivity: String = "None",
    val lastActivityTime: String = "",
    val weeklySteps: List<Pair<String, Float>> = emptyList(),
    val weeklyCalories: List<Pair<String, Float>> = emptyList(),
    val weeklyDistance: List<Pair<String, Float>> = emptyList(),
    val weeklyHeartRate: List<Pair<String, Float>> = emptyList(),
    val weeklySleep: List<Pair<String, Float>> = emptyList(),
    val weeklyActivity: List<Pair<String, String>> = emptyList(),
    val weight: String = "--",
    val floorsClimbed: String = "--",
    val moveMinutes: String = "--",
    val bodyTemperature: String = "--",
    val bloodOxygenSaturation: String = "--"
)
