package com.gramayatri.app.repository

// import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.ktx.toObjects
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import com.gramayatri.app.model.RouteStop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FirebaseRepository(/* private val db: FirebaseFirestore */) {

    private val demoRoutes = listOf(
        BusRoute(
            id = "route_101",
            name = "Town Hall -> East Colony",
            description = "6 stops • Approx 30 mins",
            stops = listOf(
                RouteStop(id = "s1", name = "Town Hall", travelTimeFromPrev = 0),
                RouteStop(id = "s2", name = "Main Market", travelTimeFromPrev = 5),
                RouteStop(id = "s3", name = "School Circle", travelTimeFromPrev = 6),
                RouteStop(id = "s4", name = "Hospital Road", travelTimeFromPrev = 7),
                RouteStop(id = "s5", name = "Canal Bridge", travelTimeFromPrev = 5),
                RouteStop(id = "s6", name = "East Colony", travelTimeFromPrev = 7)
            )
        ),
        BusRoute(
            id = "route_202",
            name = "Railway Station -> Lake View",
            description = "7 stops • Approx 40 mins",
            stops = listOf(
                RouteStop(id = "s1", name = "Railway Station", travelTimeFromPrev = 0),
                RouteStop(id = "s2", name = "Post Office", travelTimeFromPrev = 4),
                RouteStop(id = "s3", name = "Old Bus Stand", travelTimeFromPrev = 6),
                RouteStop(id = "s4", name = "City Park", travelTimeFromPrev = 5),
                RouteStop(id = "s5", name = "Collector Office", travelTimeFromPrev = 7),
                RouteStop(id = "s6", name = "Temple Junction", travelTimeFromPrev = 6),
                RouteStop(id = "s7", name = "Lake View", travelTimeFromPrev = 8)
            )
        )
    )

    fun getRoutes(): Flow<List<BusRoute>> = flowOf(demoRoutes)/*callbackFlow {
        val subscription = db.collection("routes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects<BusRoute>())
            }
        }
        awaitClose { subscription.remove() }
    }*/

    fun getLiveLocation(routeId: String): Flow<BusLocation?> = flowOf(null)/*callbackFlow {
        val subscription = db.collection("locations").document(routeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toObject(BusLocation::class.java))
                } else {
                    trySend(null)
                }
            }
        awaitClose { subscription.remove() }
    }*/

    suspend fun updateLocation(location: BusLocation) {
        // db.collection("locations").document(location.routeId).set(location)
    }
}
