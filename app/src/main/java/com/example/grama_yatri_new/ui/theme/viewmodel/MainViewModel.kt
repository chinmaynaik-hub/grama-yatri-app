package com.gramayatri.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val _routes = MutableStateFlow<List<BusRoute>>(emptyList())
    val routes: StateFlow<List<BusRoute>> = _routes

    private val _selectedRoute = MutableStateFlow<BusRoute?>(null)
    val selectedRoute: StateFlow<BusRoute?> = _selectedRoute

    private val _liveLocation = MutableStateFlow<BusLocation?>(null)
    val liveLocation: StateFlow<BusLocation?> = _liveLocation

    init {
        viewModelScope.launch {
            repository.getRoutes().collect {
                _routes.value = it
            }
        }
    }

    fun selectRoute(route: BusRoute) {
        _selectedRoute.value = route
        viewModelScope.launch {
            repository.getLiveLocation(route.id).collectLatest {
                _liveLocation.value = it
            }
        }
    }

    fun clearSelection() {
        _selectedRoute.value = null
        _liveLocation.value = null
    }
}
