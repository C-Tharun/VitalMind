package com.tharun.vitalmind

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.tharun.vitalmind.ui.ActivityHistoryScreen
import com.tharun.vitalmind.ui.ConnectScreen
import com.tharun.vitalmind.ui.DashboardState
import com.tharun.vitalmind.ui.MainViewModel
import com.tharun.vitalmind.ui.MetricHistoryScreen
import com.tharun.vitalmind.ui.MetricType
import com.tharun.vitalmind.ui.InsightsScreen
import com.tharun.vitalmind.ui.VitalMindAIScreen
import com.tharun.vitalmind.ui.StressTerrainViewModel
import com.tharun.vitalmind.ui.StressTerrainMapScreen
import com.tharun.vitalmind.ui.theme.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import com.tharun.vitalmind.ui.stress.StressScoreCard
import com.tharun.vitalmind.ui.stress.StressHistoryScreen
import com.tharun.vitalmind.ui.stress.StressHistoryViewModel
import com.tharun.vitalmind.data.AppDatabase
import com.tharun.vitalmind.ui.healthdeviation.HealthDeviationCard
import com.tharun.vitalmind.ui.healthdeviation.HealthDeviationViewModel
import com.tharun.vitalmind.data.repository.HealthDeviationRepository

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
            .addDataType(DataType.TYPE_WEIGHT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_MOVE_MINUTES, FitnessOptions.ACCESS_READ)
            .build()
    }

    private fun isGooglePlayServicesAvailable(activity: Activity): Boolean {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
        return status == ConnectionResult.SUCCESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VitalMindTheme {
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

                        // Location permission launcher
                        val locationPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission(),
                        ) { isGranted ->
                            if (isGranted) {
                                // Request location and fetch weather
                                try {
                                    val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(activity)
                                    fused.lastLocation.addOnSuccessListener { loc ->
                                        if (loc != null) {
                                            viewModel.fetchWeatherForLocation(loc.latitude, loc.longitude)
                                        } else {
                                            viewModel.fetchWeatherIfNeeded("auto:ip")
                                        }
                                    }.addOnFailureListener {
                                        viewModel.fetchWeatherIfNeeded("auto:ip")
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Failed to get location", e)
                                    viewModel.fetchWeatherIfNeeded("auto:ip")
                                }
                            } else {
                                // Fallback to IP-based weather
                                viewModel.fetchWeatherIfNeeded("auto:ip")
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
                            navController = navController,
                            viewModel = viewModel // Pass viewModel to AppScreen
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

                        // In MainActivity, after sign-in and permission, trigger syncLast7DaysData
                        LaunchedEffect(isSignedIn, hasPermission) {
                            if (isSignedIn && hasPermission && signInError == null) {
                                viewModel.syncLast7DaysData()
                            }
                        }

                        // Fetch weather after sign-in using location
                        LaunchedEffect(isSignedIn) {
                            if (isSignedIn) {
                                // Check location permissions and request if needed
                                val hasCoarse = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val hasFine = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (!hasCoarse && !hasFine) {
                                    locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                } else {
                                    // Already have permission, fetch location and weather
                                    try {
                                        val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(activity)
                                        fused.lastLocation.addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                viewModel.fetchWeatherForLocation(loc.latitude, loc.longitude)
                                            } else {
                                                viewModel.fetchWeatherIfNeeded("auto:ip")
                                            }
                                        }.addOnFailureListener {
                                            viewModel.fetchWeatherIfNeeded("auto:ip")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Failed to get location", e)
                                        viewModel.fetchWeatherIfNeeded("auto:ip")
                                    }
                                }
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
                    composable("insights") {
                        InsightsScreen(viewModel = viewModel, navController = navController, listState = rememberLazyListState())
                    }
                    composable("stress_terrain_map") {
                        val stressTerrainViewModel: StressTerrainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val state by viewModel.state.collectAsState()
                        stressTerrainViewModel.setUserId(state.userId)
                        StressTerrainMapScreen(navController = navController, viewModel = stressTerrainViewModel)
                    }
                    composable("vitalmind_ai") {
                        val state by viewModel.state.collectAsState()
                        VitalMindAIScreen(dashboardState = state, listState = rememberLazyListState())
                    }
                    composable("stress_history") {
                        val context = LocalContext.current
                        val db = AppDatabase.getDatabase(context)
                        val state by viewModel.state.collectAsState()
                        val stressRepo = com.tharun.vitalmind.data.repository.StressRepository(
                            healthDataRepository = viewModel.repository,
                            userId = state.userId,
                            stressScoreHistoryDao = db.stressScoreHistoryDao()
                        )
                        val stressHistoryViewModel = remember(state.userId) {
                            StressHistoryViewModel(stressRepo, state.userId)
                        }
                        StressHistoryScreen(viewModel = stressHistoryViewModel)
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
    navController: NavController, // Not nullable
    viewModel: MainViewModel
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
                MainNavigation(viewModel = viewModel, navController = navController)
            }
        }
    }
}

@Composable
fun MainNavigation(viewModel: MainViewModel, navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val state by viewModel.state.collectAsState()

    // Create a LazyListState for each tab (for scroll tracking)
    val homeListState = rememberLazyListState()
    val insightsListState = rememberLazyListState()
    val aiListState = rememberLazyListState()
    val profileListState = rememberLazyListState()

    // Track scroll progress for smooth animation (0f = expanded, 1f = shrunk)
    var scrollProgress by remember { mutableStateOf(0f) }
    var lastScrollOffset by remember { mutableStateOf(0) }
    var isScrollingDown by remember { mutableStateOf(false) }

    // Pick the correct listState for the current tab
    val currentListState = when (selectedTab) {
        0 -> homeListState
        1 -> insightsListState
        2 -> aiListState
        3 -> profileListState
        else -> homeListState
    }

    // Smoothly animate based on scroll amount with better responsiveness
    LaunchedEffect(selectedTab, currentListState.firstVisibleItemIndex, currentListState.firstVisibleItemScrollOffset) {
        val currentOffset = currentListState.firstVisibleItemIndex * 1000 + currentListState.firstVisibleItemScrollOffset
        val scrollDelta = currentOffset - lastScrollOffset

        // Determine scroll direction
        if (scrollDelta > 5) {
            isScrollingDown = true
        } else if (scrollDelta < -5) {
            isScrollingDown = false
        }

        // Gradually adjust progress based on scroll amount
        // Faster response for better UX
        val progressChange = when {
            isScrollingDown -> (scrollDelta / 300f).coerceIn(0f, 0.15f)
            else -> -(kotlin.math.abs(scrollDelta) / 300f).coerceIn(0f, 0.15f)
        }

        scrollProgress = (scrollProgress + progressChange).coerceIn(0f, 1f)
        lastScrollOffset = currentOffset
    }

    // Reset scroll progress when changing tabs
    LaunchedEffect(selectedTab) {
        scrollProgress = 0f
        lastScrollOffset = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> Dashboard(state, navController, homeListState, viewModel)
            1 -> InsightsScreen(viewModel = viewModel, navController = navController, listState = insightsListState)
            2 -> VitalMindAIScreen(dashboardState = state, listState = aiListState)
            3 -> ProfileScreen(state = state)
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            BottomBlurredNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                scrollProgress = scrollProgress
            )
        }
    }
}

@Composable
fun BottomBlurredNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit, scrollProgress: Float = 0f) {
    // Smoothly interpolate values based on scroll progress (0f = expanded, 1f = shrunk)
    // Use lerp (linear interpolation) for smooth transitions
    val navBarHeight = androidx.compose.ui.unit.lerp(70.dp, 60.dp, scrollProgress)
    val navBarPadding = androidx.compose.ui.unit.lerp(24.dp, 16.dp, scrollProgress)

    val cornerRadius = androidx.compose.ui.unit.lerp(32.dp, 28.dp, scrollProgress)

    // Determine if we should show labels based on progress threshold
    val showLabels = scrollProgress < 0.3f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = navBarPadding, start = navBarPadding, end = navBarPadding)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .height(navBarHeight),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // All icons always visible
            NavBarItem(
                modifier = Modifier,
                icon = Icons.Default.Home,
                label = "Home",
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                showLabel = showLabels
            )
            NavBarItem(
                modifier = Modifier,
                icon = Icons.Default.Insights,
                label = "Insights",
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                showLabel = showLabels
            )
            NavBarItem(
                modifier = Modifier,
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "AI",
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                showLabel = showLabels
            )
            NavBarItem(
                modifier = Modifier,
                icon = Icons.Default.Person,
                label = "Profile",
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                showLabel = showLabels
            )
        }
    }
}

@Composable
fun NavBarItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showLabel: Boolean = true
) {
    val iconColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "iconColor"
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor"
    )

    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(if (showLabel) 26.dp else 24.dp)
        )
        AnimatedVisibility(visible = showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(title: String, showBackButton: Boolean = true, onBackClick: (() -> Unit)? = null) {
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
            if (showBackButton) {
                IconButton(
                    onClick = { onBackClick?.invoke() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Back",
                        modifier = Modifier.size(28.dp).rotate(180f),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(state: DashboardState) {
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
                                        androidx.compose.foundation.shape.CircleShape
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
        }
    }
}

@Composable
fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
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
fun ProfileDataRow(label: String, value: String) {
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
fun ProfileDataRowLast(label: String, value: String) {
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

@Composable
fun MultiMetricHeartRings(
    steps: Float,
    stepsGoal: Float,
    kcal: Float,
    kcalGoal: Float,
    distance: Float,
    distanceGoal: Float,
    onGoalsChange: (Float, Float, Float) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    if (showGoalDialog) {
        var stepsInput by remember { mutableStateOf(stepsGoal.toInt().toString()) }
        var kcalInput by remember { mutableStateOf(kcalGoal.toInt().toString()) }
        var distanceInput by remember { mutableStateOf(distanceGoal.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Goals", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stepsInput,
                        onValueChange = { stepsInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Steps Goal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = kcalInput,
                        onValueChange = { kcalInput = it.filter { c -> c.isDigit() } },
                        label = { Text("Calories Goal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = distanceInput,
                        onValueChange = { distanceInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Distance Goal (km)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val s = stepsInput.toFloatOrNull()
                    val k = kcalInput.toFloatOrNull()
                    val d = distanceInput.toFloatOrNull()
                    if (s != null && k != null && d != null && s > 0 && k > 0 && d > 0) {
                        onGoalsChange(s, k, d)
                        showGoalDialog = false
                    }
                }) { Text("Set Goals") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }
    val stepsProgress = (steps / stepsGoal).coerceIn(0f, 1f)
    val kcalProgress = (kcal / kcalGoal).coerceIn(0f, 1f)
    val distanceProgress = (distance / distanceGoal).coerceIn(0f, 1f)
    val ringColors = listOf(StepCountPurple, ActivityRingRed, StepDistanceCyan)
    val ringProgress = listOf(stepsProgress, kcalProgress, distanceProgress)
    val ringValues = listOf(steps, kcal, distance)
    val ringGoals = listOf(stepsGoal, kcalGoal, distanceGoal)
    val ringUnits = listOf("steps", "kcal", "km")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showGoalDialog = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Activity Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Track your daily progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showGoalDialog = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Set Goals",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Heart visualization
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val baseStroke = 28f
                        val gap = 8f
                        val strokes = listOf(baseStroke, baseStroke - 8f, baseStroke - 16f)

                        fun scaleForRing(ringIndex: Int): Float {
                            val outer = baseStroke / 2 + ringIndex * (gap + 8f)
                            val maxDim = width.coerceAtMost(height)
                            return (maxDim - 2 * outer) / maxDim
                        }
                        for (i in 0..2) {
                            val scale = scaleForRing(i)
                            val stroke = strokes[i]
                            val bgPath = heartPath(width, height, scale)
                            drawPath(bgPath, color = Color.LightGray.copy(alpha = 0.2f), style = Stroke(width = stroke))
                            if (ringProgress[i] > 0f) {
                                val path = heartPath(width, height, scale)
                                val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
                                pathMeasure.setPath(path, false)
                                val length = pathMeasure.length
                                val progressPath = androidx.compose.ui.graphics.Path()
                                pathMeasure.getSegment(0f, length * ringProgress[i], progressPath, true)
                                drawPath(progressPath, color = ringColors[i], style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            }
                        }
                    }
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Activity",
                        tint = ActivityRingRed,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Progress indicators
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (i in 0..2) {
                        GoalProgressItem(
                            color = ringColors[i],
                            label = ringUnits[i],
                            current = ringValues[i].toInt(),
                            goal = ringGoals[i].toInt(),
                            progress = ringProgress[i]
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalProgressItem(color: Color, label: String, current: Int, goal: Int, progress: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "$current/$goal",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

fun heartPath(width: Float, height: Float, scale: Float): androidx.compose.ui.graphics.Path {
    val w = width * scale
    val h = height * scale
    val dx = (width - w) / 2
    val dy = (height - h) / 2
    return androidx.compose.ui.graphics.Path().apply {
        moveTo(width / 2, h * 0.8f + dy)
        cubicTo(
            w * 1.1f + dx, h * 0.55f + dy,
            w * 0.8f + dx, h * 0.1f + dy,
            width / 2, h * 0.3f + dy
        )
        cubicTo(
            w * 0.2f + dx, h * 0.1f + dy,
            -w * 0.1f + dx, h * 0.55f + dy,
            width / 2, h * 0.8f + dy
        )
        close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(state: DashboardState, navController: NavController, listState: LazyListState, viewModel: MainViewModel) {
    var stepsGoal by remember { mutableStateOf(8000f) }
    var kcalGoal by remember { mutableStateOf(3000f) }
    var distanceGoal by remember { mutableStateOf(5f) }
    val steps = state.steps.toFloatOrNull() ?: 0f
    val kcal = state.calories.toFloatOrNull() ?: 0f
    val distance = state.distance.toFloatOrNull() ?: 0f
    val summaryMetrics = listOfNotNull(
        HealthMetric(MetricType.STEPS, state.steps, "", Icons.AutoMirrored.Filled.DirectionsWalk, StepCountPurple),
        HealthMetric(MetricType.DISTANCE, state.distance, "km", Icons.Default.Map, StepDistanceCyan),
        HealthMetric(MetricType.HEART_RATE, state.heartRate, "bpm", Icons.Default.Favorite, ActivityRingRed),
        HealthMetric(MetricType.CALORIES, state.calories, "kcal", Icons.Default.LocalFireDepartment, LightGreen),
        HealthMetric(MetricType.SLEEP, state.sleepDuration, "", Icons.Default.Bedtime, StepCountPurple)
    )
    // --- Stress Score Feature ---
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val stressViewModel = remember(state.userId) {
        com.tharun.vitalmind.ui.stress.StressViewModel(
            com.tharun.vitalmind.data.repository.StressRepository(
                healthDataRepository = viewModel.repository,
                userId = state.userId,
                stressScoreHistoryDao = db.stressScoreHistoryDao()
            )
        )
    }
    val stressUiState by stressViewModel.uiState.collectAsState()

    // --- Health Deviation Feature ---
    val baselineTrainingPreferences = remember {
        com.tharun.vitalmind.data.BaselineTrainingPreferences(context)
    }
    val healthDeviationViewModel = remember(state.userId) {
        HealthDeviationViewModel(
            HealthDeviationRepository(
                healthDataRepository = viewModel.repository,
                baselineDao = db.healthDeviationBaselineDao(),
                trainingPreferences = baselineTrainingPreferences,
                userId = state.userId
            )
        )
    }
    val healthDeviationUiState by healthDeviationViewModel.uiState.collectAsState()
    val healthDeviationExportMessage by healthDeviationViewModel.exportMessage.collectAsState()

    // Clear export message after it's shown
    LaunchedEffect(healthDeviationExportMessage) {
        healthDeviationExportMessage?.let {
            // Message will be shown in snackbar, clear after 3 seconds
            kotlinx.coroutines.delay(3000)
            healthDeviationViewModel.clearExportMessage()
        }
    }

    // Remember chart producers to prevent recreation on scroll
    val stepsChartProducer = remember(state.weeklySteps) {
        ChartEntryModelProducer(state.weeklySteps.mapIndexed { index, pair -> entryOf(index.toFloat(), pair.second) })
    }
    val caloriesChartProducer = remember(state.weeklyCalories) {
        ChartEntryModelProducer(state.weeklyCalories.mapIndexed { index, pair -> entryOf(index.toFloat(), pair.second) })
    }

    Scaffold(
        topBar = {
            ModernTopAppBar(title = "Home", showBackButton = false)
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
        ) {
            item {
                // Welcome Section with gradient background
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "Welcome back,",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.userName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Let's check your health today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // Daily Goals Section
                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                MultiMetricHeartRings(
                    steps = steps,
                    stepsGoal = stepsGoal,
                    kcal = kcal,
                    kcalGoal = kcalGoal,
                    distance = distance,
                    distanceGoal = distanceGoal,
                    onGoalsChange = { s, k, d ->
                        stepsGoal = s
                        kcalGoal = k
                        distanceGoal = d
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Health Metrics Section
                Text(
                    text = "Health Metrics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Health metrics grid items (individually added for proper scrolling)
            items(summaryMetrics.chunked(2)) { rowMetrics ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowMetrics.forEach { metric ->
                        Box(modifier = Modifier.weight(1f)) {
                            NewHealthSummaryCard(metric) { navController.navigate("history/${metric.type.name}") }
                        }
                    }
                    // Add empty space if odd number of items in last row
                    if (rowMetrics.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Health Insights Section
                Text(
                    text = "Health Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // --- Stress Score Card ---
                StressScoreCard(
                    uiState = stressUiState,
                    onCalculate = { stressViewModel.calculateStress() }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("stress_history") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("View Stress History", fontWeight = FontWeight.SemiBold)
                                Text("Track your stress over time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // --- Health Deviation Card ---
                HealthDeviationCard(
                    uiState = healthDeviationUiState,
                    onAnalyze = { healthDeviationViewModel.analyzeHealthDeviation() },
                    onRetry = { healthDeviationViewModel.retryAnalysis() },
                    onExport = { healthDeviationViewModel.exportBaselineData(context) },
                    exportMessage = healthDeviationExportMessage
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Recent Activity Section
                SectionTitle(title = "Recent Activity", icon = Icons.Default.History)
                Spacer(modifier = Modifier.height(12.dp))
                LastActivityCard(activity = state.lastActivity, time = state.lastActivityTime) {
                    navController.navigate("activityHistory")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // Weekly Trends Section
                SectionTitle(title = "Weekly Trends", icon = Icons.AutoMirrored.Filled.ShowChart)
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyChartOptimized(chartProducer = stepsChartProducer, data = state.weeklySteps, title = "Steps")
                Spacer(modifier = Modifier.height(12.dp))
                WeeklyChartOptimized(chartProducer = caloriesChartProducer, data = state.weeklyCalories, title = "Calories")
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector) {
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
fun ActivityRing(
    progress: Float,
    goal: Float,
    onGoalChange: (Float) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    val progressValue = (progress / goal).coerceIn(0f, 1f)
    val heartColor = when {
        progressValue < 0.33f -> Color.Gray
        progressValue < 0.66f -> Color(0xFFFFA726) // Orange
        else -> ActivityRingRed
    }

    if (showGoalDialog) {
        var input by remember { mutableStateOf(goal.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Daily Calorie Goal") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("KCAL Goal") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newGoal = input.toFloatOrNull()
                    if (newGoal != null && newGoal > 0) {
                        onGoalChange(newGoal)
                        showGoalDialog = false
                    }
                }) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { showGoalDialog = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    // Heart outline path
                    val heartPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width / 2, height * 0.8f)
                        cubicTo(
                            width * 1.1f, height * 0.55f,
                            width * 0.8f, height * 0.1f,
                            width / 2, height * 0.3f
                        )
                        cubicTo(
                            width * 0.2f, height * 0.1f,
                            -width * 0.1f, height * 0.55f,
                            width / 2, height * 0.8f
                        )
                        close()
                    }
                    // Draw background heart
                    drawPath(heartPath, color = Color.LightGray, style = Stroke(width = 8f))

                    // Draw filled heart up to progress height
                    if (progressValue > 0f) {
                        val fillHeight = height * (1f - progressValue)
                        val fillRect = androidx.compose.ui.geometry.Rect(0f, fillHeight, width, height)
                        val fillPath = androidx.compose.ui.graphics.Path().apply {
                            addRect(fillRect)
                        }
                        // Intersect the fill rect and heart path
                        val filledHeartPath = androidx.compose.ui.graphics.Path()
                        filledHeartPath.op(heartPath, fillPath, androidx.compose.ui.graphics.PathOperation.Intersect)
                        drawPath(filledHeartPath, color = heartColor)
                    }
                    // Draw heart outline again for clarity
                    drawPath(heartPath, color = Color.LightGray, style = Stroke(width = 8f))
                }
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Move",
                    tint = heartColor,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Move", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${progress.toInt()}/${goal.toInt()} KCAL",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = heartColor
                )
                Text(
                    text = "Goal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NewHealthSummaryCard(metric: HealthMetric, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                metric.color.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header with icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = metric.icon,
                        contentDescription = null,
                        tint = metric.color,
                        modifier = Modifier.size(28.dp)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Metric name
                Text(
                    metric.type.name.split('_').joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.titlecase() } },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                // Value section
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = metric.value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = metric.color
                    )
                    if (metric.unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = metric.unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = metric.color.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsRun,
                        contentDescription = "Activity",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        activity,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
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

@Composable
fun WeeklyChartOptimized(chartProducer: ChartEntryModelProducer, data: List<Pair<String, Float>>, title: String) {
    val axisValueFormatter = remember(data) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            data.getOrNull(value.toInt())?.first ?: ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (data.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    ProvideChartStyle(rememberChartStyle()) {
                        Chart(
                            chart = columnChart(
                                columns = listOf(
                                    LineComponent(
                                        color = MaterialTheme.colorScheme.primary.toArgb(),
                                        thicknessDp = 16f,
                                        shape = Shapes.roundedCornerShape(topLeftPercent = 50, topRightPercent = 50)
                                    )
                                )
                            ),
                            chartModelProducer = chartProducer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(valueFormatter = axisValueFormatter)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DataUsage,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

