package com.tharun.vitalmind.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import android.util.Log

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

    // Default map center (San Francisco)
    val defaultCenter = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCenter, 12f)
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
                        text = if (state.showStressZones) "Stress Zones" else "Calming Zones",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
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
                    for ((index, cluster) in visibleClusters.withIndex()) {
                        val markerPosition = LatLng(cluster.latitude, cluster.longitude)
                        val markerState = rememberMarkerState(position = markerPosition)

                        Marker(
                            state = markerState,
                            title = if (state.showStressZones) {
                                "Stress Zone"
                            } else {
                                "Calming Zone"
                            },
                            snippet = "Intensity: ${(cluster.weight * 100).toInt()}% | Events: ${cluster.eventCount}"
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading stress terrain data...", style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Text(
                                "No stress data available yet.\nCollect more health data to generate the map.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(32.dp)
                            )
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
                        "This map visualizes your physiological stress patterns across locations using historical data.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Stress Detection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Calculates your personal baseline heart rate for different activities and times of day\n" +
                        "• Identifies stress events when your heart rate significantly exceeds your baseline\n" +
                        "• Excludes vigorous activities (running, sports) from stress detection\n" +
                        "• Clusters events spatially using a 500m grid",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Calming Zones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Locations where your heart rate consistently drops below baseline\n" +
                        "• Indicates places with a relaxing effect on your physiology",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        "Privacy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )

                    Text(
                        "• Uses only locally stored historical data\n" +
                        "• No real-time location tracking\n" +
                        "• Data aggregated by location cluster, not individual points",
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


