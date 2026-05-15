package com.gramayatri.app.model

// import com.google.firebase.Timestamp

data class RouteStop(
    val id: String = "",
    val name: String = "",
    val travelTimeFromPrev: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class BusRoute(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val stops: List<RouteStop> = emptyList()
)

data class BusLocation(
    val id: String = "",
    val routeId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float? = null,
    val heading: Float? = null,
    val updatedAtMillis: Long? = null, // Replaced Firebase Timestamp
    val driverId: String = "",
    val driverName: String = ""
)
