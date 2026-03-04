package com.tharun.vitalmind.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.tharun.vitalmind.notifications.NotificationScheduler
import com.tharun.vitalmind.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: DashboardState,
    listState: LazyListState = rememberLazyListState(),
    navController: NavController? = null
) {
    val context = LocalContext.current

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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    state.userName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "VitalMind User",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Notification Buttons Section
            if (navController != null) {
                item {
                    SectionTitle(title = "Notifications", icon = Icons.Default.Notifications)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Button(
                        onClick = { navController.navigate("notification_settings") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Notification Settings")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    OutlinedButton(
                        onClick = {
                            // Check notification permission first (Android 13+)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    Toast.makeText(
                                        context,
                                        "⚠️ Notification permission not granted! Go to Settings → Notifications",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@OutlinedButton
                                }
                            }

                            NotificationScheduler.triggerImmediateCheck(context)
                            Toast.makeText(context, "Notification check triggered! Check notification shade.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Test",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Test Notifications Now")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Health Stats Section
            item {
                SectionTitle(title = "Health Statistics", icon = Icons.AutoMirrored.Filled.ShowChart)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Stats Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        label = "Steps",
                        value = state.steps,
                        color = StepCountPurple
                    )
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Favorite,
                        label = "Heart Rate",
                        value = state.heartRate,
                        unit = "bpm",
                        color = ActivityRingRed
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Calories",
                        value = state.calories,
                        unit = "kcal",
                        color = LightGreen
                    )
                    ProfileStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Map,
                        label = "Distance",
                        value = state.distance,
                        unit = "km",
                        color = StepDistanceCyan
                    )
                }
            }

            // Detailed Info Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionTitle(title = "Detailed Information", icon = Icons.Default.Info)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        ProfileDataRow("Sleep Duration", state.sleepDuration)
                        ProfileDataRow("Last Activity", state.lastActivity)
                        ProfileDataRow("Weight", state.weight)
                        ProfileDataRow("Floors Climbed", state.floorsClimbed)
                        ProfileDataRowLast("Move Minutes", state.moveMinutes)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sign Out Section
            if (navController != null) {
                item {
                    SectionTitle(title = "Account", icon = Icons.Default.AccountCircle)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    OutlinedButton(
                        onClick = {
                            // Sign out from Google
                            val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                                context,
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                                ).build()
                            )
                            googleSignInClient.signOut().addOnCompleteListener {
                                Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                // Restart the app to show sign-in screen
                                (context as? android.app.Activity)?.recreate()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Sign Out")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    unit: String = "",
    color: Color
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        if (unit.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                unit,
                                style = MaterialTheme.typography.bodySmall,
                                color = color.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileDataRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

@Composable
private fun ProfileDataRowLast(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}
