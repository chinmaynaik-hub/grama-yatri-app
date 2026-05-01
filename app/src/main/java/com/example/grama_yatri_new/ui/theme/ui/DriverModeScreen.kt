package com.gramayatri.app.ui

import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
// import com.google.firebase.Timestamp
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.repository.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverModeScreen(
    route: BusRoute,
    userId: String,
    userName: String,
    repository: FirebaseRepository,
    fusedLocationClient: FusedLocationProviderClient,
    onBack: () -> Unit
) {
    var isLive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val locationCallback = remember(route.id, userId, userName, repository, scope) {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    scope.launch {
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
                }
            }
        }
    }

    DisposableEffect(fusedLocationClient, locationCallback) {
        onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
    }

    DriverModeContent(
        routeName = route.name,
        isLive = isLive,
        onBack = onBack,
        onToggleLive = {
            isLive = !isLive
            if (isLive) {
                val request = LocationRequest.Builder(5000L)
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMinUpdateIntervalMillis(2000L)
                    .build()
                // Permission check should be handled at a higher level
                try {
                    fusedLocationClient.requestLocationUpdates(
                        request,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    isLive = false
                }
            } else {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverModeContent(
    routeName: String,
    isLive: Boolean,
    onBack: () -> Unit,
    onToggleLive: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$routeName - Driver") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            onBack = {},
            onToggleLive = { isLive = !isLive }
        )
    }
}
