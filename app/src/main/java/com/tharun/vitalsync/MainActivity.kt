package com.tharun.vitalsync

import android.Manifest
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.tharun.vitalsync.ui.ActivityHistoryScreen
import com.tharun.vitalsync.ui.ConnectScreen
import com.tharun.vitalsync.ui.DashboardState
import com.tharun.vitalsync.ui.MainViewModel
import com.tharun.vitalsync.ui.MetricHistoryScreen
import com.tharun.vitalsync.ui.MetricType
import com.tharun.vitalsync.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class HealthMetric(
    val type: MetricType,
    val value: String,
    val unit: String,
    val icon: ImageVector,
    val color: Color
)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
            .build()
    }

    private fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        return status == ConnectionResult.SUCCESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VitalSyncTheme {
                val context = LocalContext.current
                val navController = rememberNavController()
                var signInError by remember { mutableStateOf<String?>(null) }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        val state by viewModel.state.collectAsState()
                        var isSignedIn by remember { mutableStateOf(false) }
                        var hasPermission by remember { mutableStateOf(false) }
                        var isSyncTriggered by remember { mutableStateOf(false) }

                        val activity = context as? Activity ?: (context as ContextWrapper).baseContext as Activity

                        // Permission launcher callback
                        val activityPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission(),
                        ) { isGranted ->
                            hasPermission = isGranted
                            if (!isGranted) {
                                Log.e("MainActivity", "Activity Recognition permission denied.")
                                signInError = "Activity Recognition permission denied."
                            }
                        }

                        val signInLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                try {
                                    val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                                    if (account == null) {
                                        signInError = "Google Sign-In failed: account is null."
                                        isSignedIn = false
                                        return@rememberLauncherForActivityResult
                                    }
                                    Log.d("MainActivity", "Sign-in successful for account: ${account.email}")
                                    viewModel.setUserIdAndName(account.id ?: "guest", account.displayName)
                                    isSignedIn = true
                                    signInError = null

                                    // Check permission after sign-in
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                        }
                                    } else {
                                        hasPermission = true
                                    }
                                } catch (e: ApiException) {
                                    Log.e("MainActivity", "Sign-In failed after result OK, code: ${e.statusCode}", e)
                                    isSignedIn = false
                                    signInError = when (e.statusCode) {
                                        10 -> "Google developer configuration error. Check OAuth client ID and SHA1."
                                        7 -> "Network error. Please check your connection."
                                        12501 -> "Sign-in cancelled."
                                        else -> "Google Sign-In failed: ${e.localizedMessage} (code ${e.statusCode})"
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Sign-In failed with unexpected error", e)
                                    isSignedIn = false
                                    signInError = "Unexpected error during sign-in: ${e.localizedMessage}"
                                }
                            } else {
                                Log.e("MainActivity", "Sign-In failed with result code: ${result.resultCode}")
                                isSignedIn = false
                                signInError = "Sign-In failed or cancelled."
                            }
                        }

                        AppScreen(
                            isSignedIn = isSignedIn,
                            state = state,
                            onConnectClick = {
                                if (!isGooglePlayServicesAvailable(activity)) {
                                    signInError = "Google Play Services is not available or out of date."
                                    return@AppScreen
                                }
                                signInError = null
                                val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestId()
                                    .addExtension(fitnessOptions)
                                    .build()
                                val googleSignInClient = GoogleSignIn.getClient(activity, signInOptions)
                                signInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            navController = navController
                        )

                        // Show error message if any
                        signInError?.let { errorMsg ->
                            LaunchedEffect(errorMsg) {
                                Log.e("MainActivity", "User-visible error: $errorMsg")
                            }
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                Text(errorMsg, color = Color.Red, modifier = Modifier.padding(16.dp))
                            }
                        }

                        // Check for existing login and permission on launch
                        LaunchedEffect(Unit) {
                            if (!isGooglePlayServicesAvailable(activity)) {
                                signInError = "Google Play Services is not available or out of date."
                                return@LaunchedEffect
                            }
                            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                            if (account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                                Log.d("MainActivity", "Permissions already granted on launch for ${account.id}")
                                viewModel.setUserIdAndName(account.id ?: "guest", account.displayName)
                                isSignedIn = true
                                signInError = null
                                hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                                } else {
                                    true
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPermission) {
                                    activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                }
                            }
                        }

                        // Single reliable trigger for data sync
                        LaunchedEffect(isSignedIn, hasPermission) {
                            if (isSignedIn && hasPermission && !isSyncTriggered && signInError == null) {
                                viewModel.syncAllData()
                                isSyncTriggered = true
                            }
                        }
                    }
                    composable("history/{metricType}") { backStackEntry ->
                        val metricType = MetricType.valueOf(backStackEntry.arguments?.getString("metricType") ?: "STEPS")
                        MetricHistoryScreen(metricType = metricType, navController = navController, viewModel = viewModel)
                    }
                    composable("activityHistory") {
                        ActivityHistoryScreen(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppScreen(
    isSignedIn: Boolean,
    state: DashboardState,
    onConnectClick: () -> Unit,
    navController: NavController
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedVisibility(visible = !isSignedIn) {
                ConnectScreen(onConnectClick)
            }
            AnimatedVisibility(visible = isSignedIn) {
                Dashboard(state, navController)
            }
        }
    }
}

@Composable
fun Dashboard(state: DashboardState, navController: NavController) {
    val summaryMetrics = listOfNotNull(
        HealthMetric(MetricType.STEPS, state.steps, "", Icons.AutoMirrored.Filled.DirectionsWalk, StepCountPurple),
        HealthMetric(MetricType.DISTANCE, state.distance, "km", Icons.Default.Map, StepDistanceCyan),
        HealthMetric(MetricType.HEART_RATE, state.heartRate, "bpm", Icons.Default.Favorite, ActivityRingRed),
        HealthMetric(MetricType.CALORIES, state.calories, "kcal", Icons.Default.LocalFireDepartment, LightGreen),
        HealthMetric(MetricType.SLEEP, state.sleepDuration, "", Icons.Default.Bedtime, StepCountPurple)
    )

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            ActivityRing(progress = state.calories.toFloatOrNull() ?: 0f, goal = 3000f)
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(580.dp) // Adjusted height for 3 rows
            ) {
                items(summaryMetrics) { metric ->
                    NewHealthSummaryCard(metric) { navController.navigate("history/${metric.type.name}") }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = "Last Activity", icon = Icons.Default.History)
            Spacer(modifier = Modifier.height(16.dp))
            LastActivityCard(activity = state.lastActivity, time = state.lastActivityTime) {
                navController.navigate("activityHistory")
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SectionTitle(title = "Weekly Trends", icon = Icons.Default.ShowChart)
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            WeeklyChart(state.weeklySteps, "Steps")
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            WeeklyChart(state.weeklyCalories, "Calories")
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ActivityRing(progress: Float, goal: Float) {
    val progressValue = (progress / goal).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.DarkGray,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 30f)
                    )
                    drawArc(
                        color = ActivityRingRed,
                        startAngle = -90f,
                        sweepAngle = 360 * progressValue,
                        useCenter = false,
                        style = Stroke(width = 30f)
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Move",
                    tint = ActivityRingRed,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text("Move", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${progress.toInt()}/${goal.toInt()} KCAL",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ActivityRingRed
                )
            }
        }
    }
}


@Composable
fun NewHealthSummaryCard(metric: HealthMetric, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    metric.type.name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.titlecase() } },
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "details", tint = LightGreen)
            }

            Column {
                Text("Today", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = metric.color
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (metric.unit.isNotEmpty()) {
                        Text(
                            text = metric.unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = metric.color.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 8.dp) // Aligns with baseline of big text
                        )
                    }
                }
            }

            // Placeholder for the small chart
            Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val heights = remember { List(15) { Random().nextFloat() } }
                    heights.forEach {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(it)
                                .background(metric.color.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LastActivityCard(activity: String, time: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = "Last Activity", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(activity, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun WeeklyChart(data: List<Pair<String, Float>>, title: String) {
    val chartModelProducer = ChartEntryModelProducer(data.mapIndexed { index, pair -> entryOf(index.toFloat(), pair.second) })
    val axisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ -> data.getOrNull(value.toInt())?.first ?: "" }

    Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (data.isNotEmpty()) {
                ProvideChartStyle(rememberChartStyle()) {
                    Chart(
                        chart = columnChart(
                            columns = listOf(
                                LineComponent(
                                    color = MaterialTheme.colorScheme.primary.toArgb(),
                                    thicknessDp = 16f,
                                    shape = Shapes.roundedCornerShape(25)
                                )
                            )
                        ),
                        chartModelProducer = chartModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter)
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No weekly data available")
                }
            }
        }
    }
}
