package com.gramayatri.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.RouteStop
import com.gramayatri.app.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FirebaseRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes

    private val _selectedRoute = MutableStateFlow<BusRoute?>(null)
    val selectedRoute: StateFlow<BusRoute?> = _selectedRoute

    private val _liveLocation = MutableStateFlow<BusLocation?>(null)
    val liveLocation: StateFlow<BusLocation?> = _liveLocation
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    private val _isAddingRoute = MutableStateFlow(false)
    val isAddingRoute: StateFlow<Boolean> = _isAddingRoute
    private var liveLocationJob: Job? = null
    private var routeRetryAttempts = 0

    init {
        observeAuthAndRoutes()
    }

    private fun observeAuthAndRoutes() {
        viewModelScope.launch {
            authStateChanges()
                .collectLatest { user ->
                    if (user == null) {
                        routeRetryAttempts = 0
                        _routes.value = emptyList()
                        clearSelection()
                        _errorMessage.value = null
                        return@collectLatest
                    }

                    repository.getRoutes()
                        .retryWhen { error, _ ->
                            val shouldRetry = shouldRetryRouteStream(error)
                            if (shouldRetry) {
                                routeRetryAttempts += 1
                                _routes.value = emptyList()
                                _errorMessage.value = mapDataError(
                                    error = error,
                                    action = "load routes",
                                    firestorePermissionHint =
                                        "Firestore denied access to routes for your signed-in account. " +
                                            "Update Firestore rules once to allow authenticated users to read routes and locations."
                                )
                                delay(calculateRetryDelay(routeRetryAttempts))
                            }
                            shouldRetry
                        }
                        .catch { error ->
                            routeRetryAttempts = 0
                            _routes.value = emptyList()
                            _errorMessage.value = mapDataError(
                                error = error,
                                action = "load routes",
                                firestorePermissionHint =
                                    "Firestore denied access to routes for your signed-in account. " +
                                        "Update Firestore rules once to allow authenticated users to read routes and locations."
                            )
                        }
                        .collect { routes ->
                            routeRetryAttempts = 0
                            _routes.value = routes
                            _errorMessage.value = null
                        }
                }
        }
    }

    fun selectRoute(route: BusRoute) {
        _selectedRoute.value = route
        liveLocationJob?.cancel()
        liveLocationJob = viewModelScope.launch {
            repository.getLiveLocation(route.id)
                .catch { error ->
                    _liveLocation.value = null
                    _errorMessage.value = mapDataError(
                        error = error,
                        action = "load live location",
                        firestorePermissionHint =
                            "Firestore denied access to live location. In Firebase Console > Firestore Database > Rules, " +
                                "allow authenticated users to read locations."
                    )
                }
                .collectLatest { location ->
                    _liveLocation.value = location
                    _errorMessage.value = null
                }
        }
    }

    fun clearSelection() {
        liveLocationJob?.cancel()
        liveLocationJob = null
        _selectedRoute.value = null
        _liveLocation.value = null
    }

    fun addRoute(
        routeName: String,
        routeDescription: String,
        stopsInput: String,
        onResult: (String?, String?) -> Unit
    ) {
        val name = routeName.trim()
        if (name.isBlank()) {
            val message = "Route name is required."
            _errorMessage.value = message
            onResult(null, message)
            return
        }

        val parsedStops = parseStops(stopsInput)
        if (parsedStops is ParsedStops.Invalid) {
            _errorMessage.value = parsedStops.message
            onResult(null, parsedStops.message)
            return
        }
        parsedStops as ParsedStops.Valid

        _isAddingRoute.value = true
        viewModelScope.launch {
            try {
                repository.addRoute(
                    BusRoute(
                        name = name,
                        description = routeDescription.trim(),
                        stops = parsedStops.stops
                    )
                )
                _errorMessage.value = null
                onResult("Route added successfully.", null)
            } catch (error: Throwable) {
                val message = mapDataError(
                    error = error,
                    action = "add route",
                    firestorePermissionHint =
                        "Firestore denied route creation. In Firebase Console > Firestore Database > Rules, " +
                            "allow driver/admin users to create routes."
                )
                _errorMessage.value = message
                onResult(null, message)
            } finally {
                _isAddingRoute.value = false
            }
        }
    }

    private fun mapDataError(
        error: Throwable,
        action: String,
        firestorePermissionHint: String
    ): String {
        if (error is FirebaseNetworkException) {
            return "Network error. Check your internet connection and try again."
        }

        if (error is FirebaseFirestoreException) {
            return when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    if (auth.currentUser == null) {
                        "Please login to continue."
                    } else {
                        firestorePermissionHint
                    }
                }
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "Firestore database is not initialized for this project. Create it in Firebase Console and try again."
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "Firestore is temporarily unavailable. Please try again."
                else -> error.localizedMessage ?: "Unable to $action."
            }
        }

        return error.message ?: "Unable to $action."
    }

    private fun shouldRetryRouteStream(error: Throwable): Boolean {
        if (auth.currentUser == null) return false
        if (error is FirebaseNetworkException) return true
        if (error is FirebaseFirestoreException) {
            return error.code == FirebaseFirestoreException.Code.UNAVAILABLE
        }
        return false
    }

    private fun calculateRetryDelay(attempt: Int): Long {
        val maxDelayMillis = 15_000L
        return (attempt.coerceAtLeast(1) * 2_000L).coerceAtMost(maxDelayMillis)
    }

    private fun authStateChanges() = callbackFlow<FirebaseUser?> {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged { old, new -> old?.uid == new?.uid }

    private fun parseStops(stopsInput: String): ParsedStops {
        val lines = stopsInput
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.size < 2) {
            return ParsedStops.Invalid(
                "Add at least 2 stops. Use one stop per line: Stop Name|Minutes from previous|Latitude(optional)|Longitude(optional)."
            )
        }

        val stops = mutableListOf<RouteStop>()
        lines.forEachIndexed { index, line ->
            val parts = line.split("|").map { it.trim() }
            val stopName = parts.firstOrNull().orEmpty()
            if (stopName.isBlank()) {
                return ParsedStops.Invalid("Stop ${index + 1} name is missing.")
            }

            val minutesValue = if (index == 0) {
                0
            } else {
                val value = parts.getOrNull(1)?.toIntOrNull()
                    ?: return ParsedStops.Invalid(
                        "Stop ${index + 1} must include minutes from previous stop as an integer."
                    )
                value.coerceAtLeast(0)
            }

            val latitude = when {
                parts.size >= 3 && parts[2].isNotBlank() -> {
                    parts[2].toDoubleOrNull()
                        ?: return ParsedStops.Invalid("Stop ${index + 1} has invalid latitude.")
                }
                else -> 0.0
            }

            val longitude = when {
                parts.size >= 4 && parts[3].isNotBlank() -> {
                    parts[3].toDoubleOrNull()
                        ?: return ParsedStops.Invalid("Stop ${index + 1} has invalid longitude.")
                }
                else -> 0.0
            }

            stops += RouteStop(
                id = "stop_${index + 1}",
                name = stopName,
                travelTimeFromPrev = minutesValue,
                latitude = latitude,
                longitude = longitude
            )
        }

        return ParsedStops.Valid(stops)
    }

    private sealed interface ParsedStops {
        data class Valid(val stops: List<RouteStop>) : ParsedStops
        data class Invalid(val message: String) : ParsedStops
    }
}
