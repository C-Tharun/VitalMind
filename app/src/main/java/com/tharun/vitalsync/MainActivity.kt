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
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.tharun.vitalsync.ui.DashboardState
import com.tharun.vitalsync.ui.MainViewModel
import com.tharun.vitalsync.ui.MetricHistoryScreen
import com.tharun.vitalsync.ui.MetricType
import com.tharun.vitalsync.ui.theme.VitalSyncTheme
import java.text.SimpleDateFormat
import java.util.*

data class HealthMetric(val type: MetricType, val value: String, val unit: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val fitnessOptions: FitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ) // Required for aggregate steps
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ) // Required for aggregate calories
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ) // Required for aggregate distance
            .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VitalSyncTheme {
                val context = LocalContext.current
                val navController = rememberNavController()

                val activityPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        Log.d("MainActivity", "Activity Recognition permission granted.")
                        viewModel.syncData()
                    } else {
                        Log.e("MainActivity", "Activity Recognition permission denied.")
                    }
                }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        val state by viewModel.state.collectAsState()
                        var isSignedIn by remember { mutableStateOf(false) }

                        val signInLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                try {
                                    val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                                    Log.d("MainActivity", "Sign-in successful for account: ${account?.email}")
                                    isSignedIn = true
                                    viewModel.setUserIdAndName(account?.id ?: "guest", account?.displayName)
                                    // Now check for Activity Recognition permission
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        when (PackageManager.PERMISSION_GRANTED) {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) -> {
                                                viewModel.syncData()
                                            }
                                            else -> {
                                                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                            }
                                        }
                                    } else {
                                        viewModel.syncData() // No runtime permission needed for older versions
                                    }
                                } catch (e: ApiException) {
                                    Log.e("MainActivity", "Sign-In failed after result OK", e)
                                }
                            } else {
                                Log.e("MainActivity", "Sign-In failed with result code: ${result.resultCode}")
                            }
                        }

                        AppScreen(
                            isSignedIn = isSignedIn,
                            state = state,
                            onConnectClick = {
                                val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestId()
                                    .addExtension(fitnessOptions)
                                    .build()
                                val activity = context as? Activity ?: (context as ContextWrapper).baseContext as Activity
                                val googleSignInClient = GoogleSignIn.getClient(activity, signInOptions)
                                signInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            navController = navController
                        )

                        // Check for existing permissions on launch
                        LaunchedEffect(Unit) {
                            val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
                            if (account != null && GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                                Log.d("MainActivity", "Permissions already granted on launch for ${account.id}")
                                isSignedIn = true
                                viewModel.setUserIdAndName(account.id ?: "guest", account.displayName)
                                viewModel.syncData()
                            }
                        }
                    }
                    composable("history/{metricType}") { backStackEntry ->
                        val metricType = MetricType.valueOf(backStackEntry.arguments?.getString("metricType") ?: "STEPS")
                        MetricHistoryScreen(metricType = metricType, navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(isSignedIn: Boolean, state: DashboardState, onConnectClick: () -> Unit, navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("VitalSync") }) }
    ) {
        padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(
                visible = !isSignedIn,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ConnectScreen(onConnectClick)
            }
            AnimatedVisibility(
                visible = isSignedIn,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Dashboard(state, navController)
            }
        }
    }
}

@Composable
fun ConnectScreen(onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connect to Google Fit to see your health data.", modifier = Modifier.padding(bottom = 16.dp))
        Button(onClick = onConnectClick) {
            Text("Connect to Google Fit")
        }
    }
}

@Composable
fun Dashboard(state: DashboardState, navController: NavController) {
    val summaryMetrics = listOfNotNull(
        HealthMetric(MetricType.HEART_RATE, state.heartRate, "bpm", Icons.Default.Favorite),
        HealthMetric(MetricType.CALORIES, state.calories, "kcal", Icons.Default.LocalFireDepartment),
        HealthMetric(MetricType.STEPS, state.steps, "", Icons.AutoMirrored.Filled.DirectionsWalk),
        HealthMetric(MetricType.DISTANCE, state.distance, "km", Icons.Default.Map),
        // HealthMetric(MetricType.HEART_POINTS, state.heartPoints, "pts", Icons.AutoMirrored.Filled.TrendingUp), // Disabled
        HealthMetric(MetricType.SLEEP, state.sleepDuration, "", Icons.Default.Bedtime)
    )

    LazyColumn {
        item {
            Greeting(name = state.userName)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Today's Summary", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.height(480.dp) 
            ) {
                items(summaryMetrics) { metric ->
                    HealthSummaryCard(metric) { navController.navigate("history/${metric.type.name}") }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Last Activity", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            LastActivityCard(activity = state.lastActivity, time = state.lastActivityTime)
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Weekly Trends", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
        }
        item {
            WeeklyChart(state.weeklySteps, "Steps")
        }
        item {
            WeeklyChart(state.weeklyCalories, "Calories")
        }
    }
}

@Composable
fun Greeting(name: String) {
    Column {
        Text("Good Morning, $name", style = MaterialTheme.typography.headlineMedium)
        Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun HealthSummaryCard(metric: HealthMetric, onClick: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(1f).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = metric.icon, contentDescription = metric.type.name, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = metric.type.name.replaceFirstChar { it.uppercaseChar() }, fontWeight = FontWeight.Bold)
            Text(text = metric.value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(text = metric.unit, fontSize = 12.sp)
        }
    }
}

@Composable
fun LastActivityCard(activity: String, time: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, contentDescription = "Last Activity", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(activity, fontWeight = FontWeight.Bold)
                Text(time, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WeeklyChart(data: List<Pair<String, Float>>, title: String) {
    val chartModelProducer = ChartEntryModelProducer(data.mapIndexed { index, pair -> entryOf(index.toFloat(), pair.second) })
    val axisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ -> data.getOrNull(value.toInt())?.first ?: "" }

    Card(modifier = Modifier.fillMaxWidth().height(250.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (data.isNotEmpty()) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No weekly data available")
                }
            }
        }
    }
}
