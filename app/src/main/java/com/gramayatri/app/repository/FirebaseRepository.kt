package com.gramayatri.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.gramayatri.app.model.BusLocation
import com.gramayatri.app.model.BusRoute
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val db: FirebaseFirestore) {
    fun getRoutes(): Flow<List<BusRoute>> = callbackFlow {
        val subscription = db.collection("routes").addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(
                    snapshot.documents.mapNotNull { document ->
                        document.toObject(BusRoute::class.java)?.copy(
                            id = document.id
                        )
                    }
                )
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addRoute(route: BusRoute) {
        val routeDocument = db.collection("routes").document()
        routeDocument
            .set(route.copy(id = routeDocument.id))
            .await()
    }

    fun getLiveLocation(routeId: String): Flow<BusLocation?> = callbackFlow {
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
    }

    suspend fun updateLocation(location: BusLocation) {
        db.collection("locations")
            .document(location.routeId)
            .set(location.copy(id = location.routeId))
            .await()
    }
}
