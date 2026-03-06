package com.tharun.vitalmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

/**
 * Full-screen Compose screen for displaying the Stress Terrain Map.
 * Shows physiological stress intensity as a heatmap overlay with:
 * - Stress zones (high heart rate deviation)
 * - Calming zones (low heart rate)
 * - Toggle between zone types
 * - Info dialog explaining the calculation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StressTerrainMapScreen(
    navController: NavController,
    viewModel: StressTerrainViewModel
) {
    val state by viewModel.state.collectAsState()
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedCluster by remember { mutableStateOf<com.tharun.vitalmind.data.StressCluster?>(null) }

    // Calculate map center from actual clusters
    val mapCenter = remember(state.stressClusters, state.calmingClusters) {
        val allClusters = state.stressClusters + state.calmingClusters
        if (allClusters.isNotEmpty()) {
            val avgLat = allClusters.map { it.latitude }.average()
            val avgLng = allClusters.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        } else {
            LatLng(37.7749, -122.4194) // Fallback to default
        }
    }

    // Calculate zoom level to fit all markers
    val zoomLevel = remember(state.stressClusters, state.calmingClusters) {
        val allClusters = state.stressClusters + state.calmingClusters
        if (allClusters.size > 1) {
            // Calculate bounds and adjust zoom
            val lats = allClusters.map { it.latitude }
            val lngs = allClusters.map { it.longitude }
            val latSpan = (lats.maxOrNull() ?: 0.0) - (lats.minOrNull() ?: 0.0)
            val lngSpan = (lngs.maxOrNull() ?: 0.0) - (lngs.minOrNull() ?: 0.0)
            val maxSpan = maxOf(latSpan, lngSpan)

            when {
                maxSpan < 0.01 -> 15f  // Very close markers
                maxSpan < 0.05 -> 13f  // Close markers
                maxSpan < 0.1 -> 12f   // Medium spread
                maxSpan < 0.5 -> 10f   // Wide spread
                else -> 8f             // Very wide spread
            }
        } else {
            14f // Single marker or default
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mapCenter, zoomLevel)
    }

    // Update camera position when clusters change (after map is ready)
    LaunchedEffect(mapCenter, zoomLevel) {
        // Only update if we have actual clusters
        if (state.stressClusters.isNotEmpty() || state.calmingClusters.isNotEmpty()) {
            try {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(mapCenter, zoomLevel)
            } catch (e: Exception) {
                // Ignore if camera not ready yet
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                        text = if (state.showStressZones) "Stress Zones (Last 30 Days)" else "Calming Zones (Last 30 Days)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "How it works",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Map layer
            if (state.stressClusters.isNotEmpty() || state.calmingClusters.isNotEmpty()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                ) {
                    // Add markers for stress zones
                    val visibleClusters = if (state.showStressZones) state.stressClusters else state.calmingClusters
                    for (cluster in visibleClusters) {
                        val markerPosition = LatLng(cluster.latitude, cluster.longitude)
                        val markerState = rememberMarkerState(position = markerPosition)

                        Marker(
                            state = markerState,
                            title = if (state.showStressZones) {
                                "Stress Zone"
                            } else {
                                "Calming Zone"
                            },
                            snippet = "Tap for details",
                            onClick = {
                                selectedCluster = cluster
                                true
                            }
                        )
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading stress terrain data...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No stress data available for last 30 days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "To build your stress terrain map:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "1. Go to Home page",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "2. Click 'Calculate Stress' button",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "3. Allow location permission",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "4. Repeat daily to build your map",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshData() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Refresh Data")
                            }
                        }
                    }
                }
            }

            // Control panel overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    "View Mode",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Toggle button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!state.showStressZones) viewModel.toggleZoneType()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.showStressZones) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Stress",
                            color = if (state.showStressZones) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Button(
                        onClick = {
                            if (state.showStressZones) viewModel.toggleZoneType()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!state.showStressZones) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Calm",
                            color = if (!state.showStressZones) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (state.showStressZones) {
                                "Stress Zones: ${state.stressClusters.size}"
                            } else {
                                "Calming Zones: ${state.calmingClusters.size}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Cluster detail card overlay
            selectedCluster?.let { cluster ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(0.95f),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (cluster.isCalmingZone) "🌿 Calming Zone" else "⚡ Stress Zone",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (cluster.isCalmingZone)
                                        Color(0xFF4CAF50)
                                    else
                                        Color(0xFFFF5722)
                                )
                                Text(
                                    text = "Based on ${cluster.eventCount} ${if (cluster.eventCount == 1) "occurrence" else "occurrences"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedCluster = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Mood card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (cluster.mood.lowercase()) {
                                        "relaxed" -> Color(0xFFE8F5E9)
                                        "alert" -> Color(0xFFFFF3E0)
                                        "stressed" -> Color(0xFFFFEBEE)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = cluster.mood,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = when (cluster.mood.lowercase()) {
                                            "relaxed" -> Color(0xFF2E7D32)
                                            "alert" -> Color(0xFFE65100)
                                            "stressed" -> Color(0xFFC62828)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = "Mood",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Level card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = cluster.stressLevel,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Level",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Intensity bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Intensity",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(cluster.weight * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { cluster.weight },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = if (cluster.isCalmingZone) Color(0xFF4CAF50) else Color(0xFFFF5722),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date information
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "📅 Occurrence Dates",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = cluster.occurrenceDates.take(5).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                if (cluster.occurrenceDates.size > 5) {
                                    Text(
                                        text = "+ ${cluster.occurrenceDates.size - 5} more dates",
                                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Location info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 ${String.format("%.5f", cluster.latitude)}, ${String.format("%.5f", cluster.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error message
            state.errorMessage?.let { errorMsg ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // Info dialog
    if (showInfoDialog) {
        Dialog(
            onDismissRequest = { showInfoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "How is the Stress Terrain Map calculated?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(
                        "This map visualizes your physiological stress patterns across locations using the last 30 days of stress score history data.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Data Source",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Uses stress scores calculated from the Home page\n" +
                        "• Displays data from the last 30 days\n" +
                        "• Location data captured when 'Calculate Stress' is clicked\n" +
                        "• Aggregates multiple readings at similar locations",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Stress Zone Detection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Clusters locations where you felt 'Alert' or 'Stressed'\n" +
                        "• Groups nearby locations (within ~500m) together\n" +
                        "• Shows frequency and intensity of stress at each location\n" +
                        "• Displays occurrence dates and patterns",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Calming Zone Detection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Clusters locations where you felt 'Relaxed'\n" +
                        "• Indicates places with a calming effect on your well-being\n" +
                        "• Helps identify environments that promote relaxation",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Privacy & Data",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• All data stored locally on your device\n" +
                        "• Shows last 30 days of stress history\n" +
                        "• Location only captured when you click 'Calculate Stress'\n" +
                        "• Data aggregated by location cluster for privacy",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = { showInfoDialog = false },
                        modifier = Modifier
                            .align(Alignment.End)
                            .height(40.dp)
                    ) {
                        Text("Got it")
                    }
                }
            }
        }
    }
}


