package com.gramayatri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
// import com.google.firebase.firestore.FirebaseFirestore
import com.gramayatri.app.repository.FirebaseRepository
import com.gramayatri.app.ui.BusTrackerScreen
import com.gramayatri.app.ui.DriverModeScreen
import com.gramayatri.app.ui.RouteListScreen
import com.gramayatri.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
    // Initialize Firebase Repository and ViewModel
        // val db = FirebaseFirestore.getInstance()
        val repository = FirebaseRepository(/* db */)
        viewModel = MainViewModel(repository)

        setContent {
            val routes by viewModel.routes.collectAsStateWithLifecycle()
            val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
            val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
            var isDriverMode by rememberSaveable { mutableStateOf(false) }
            val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(this@MainActivity)
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        selectedRoute == null -> {
                            RouteListScreen(
                                routes = routes,
                                onRouteSelected = { route ->
                                    isDriverMode = false
                                    viewModel.selectRoute(route)
                                }
                            )
                        }

                        isDriverMode -> {
                            DriverModeScreen(
                                route = selectedRoute!!,
                                userId = "driver_demo_001",
                                userName = "Demo Driver",
                                repository = repository,
                                fusedLocationClient = fusedLocationClient,
                                onBack = { isDriverMode = false }
                            )
                        }

                        else -> {
                            BusTrackerScreen(
                                route = selectedRoute!!,
                                busLocation = liveLocation,
                                onBack = {
                                    isDriverMode = false
                                    viewModel.clearSelection()
                                },
                                onDriverMode = { isDriverMode = true }
                            )
                        }
                    }
                }
            }
        }
    }
}
