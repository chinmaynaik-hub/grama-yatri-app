package com.gramayatri.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gramayatri.app.location.FusedLocationTracker
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverModeScreen(
    route: BusRoute,
    userId: String,
    userName: String,
    repository: FirebaseRepository,
    locationTracker: FusedLocationTracker,
    onBack: () -> Unit
) {
    var isLive by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var trackingJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val stopTracking: () -> Unit = {
        trackingJob?.cancel()
        trackingJob = null
        isLive = false
    }

    val startTracking: () -> Unit = {
        errorMessage = null
        trackingJob?.cancel()
        trackingJob = scope.launch {
            locationTracker.locationUpdates()
                .onEach { location ->
                    repository.updateLocation(
                        BusLocation(
                            routeId = route.id,
                            latitude = location.latitude,
                            longitude = location.longitude,
                            speed = location.speed,
                            heading = location.bearing,
                            updatedAtMillis = System.currentTimeMillis(),
                            driverId = userId,
                            driverName = userName
                        )
                    )
                }
                .catch { error ->
                    errorMessage = error.message ?: "Failed to start location updates."
                    isLive = false
                }
                .collect()
        }
        isLive = true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            startTracking()
        } else {
            errorMessage = "Location permission is required to go live."
            stopTracking()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            trackingJob?.cancel()
            trackingJob = null
        }
    }

    DriverModeContent(
        routeName = route.name,
        isLive = isLive,
        errorMessage = errorMessage,
        onBack = onBack,
        onToggleLive = {
            if (isLive) {
                stopTracking()
            } else if (locationTracker.hasLocationPermission()) {
                startTracking()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverModeContent(
    routeName: String,
    isLive: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleLive: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$routeName - Driver") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = Color(0xFF141414)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF141414))
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Driver Control", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onToggleLive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLive) Color.White else Color.Red
                )
            ) {
                Text(
                    if (isLive) "STOP GPS" else "GO LIVE",
                    color = if (isLive) Color.Black else Color.White
                )
            }

            if (isLive) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Broadcasting live location...", color = Color.Green)
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = Color(0xFFFF7777))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DriverModeScreenPreview() {
    var isLive by remember { mutableStateOf(false) }

    MaterialTheme {
        DriverModeContent(
            routeName = "Town Hall -> East Colony",
            isLive = isLive,
            errorMessage = null,
            onBack = {},
            onToggleLive = { isLive = !isLive }
        )
    }
}
