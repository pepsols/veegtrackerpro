package com.example.veegtrackerpro.data.repository

import com.example.veegtrackerpro.data.local.dao.VeegDao
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SyncRepository(
    private val veegDao: VeegDao,
    private val scope: CoroutineScope
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var syncedPoiIds: Set<String> = emptySet()

    fun startSync() {
        // Sync local routes to Firestore
        scope.launch(Dispatchers.IO) {
            veegDao.getAllRoutes().collectLatest { routes ->
                routes.forEach { route ->
                    firestore.collection("routes").document(route.id.toString()).set(route)
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            veegDao.getAllPois().collectLatest { pois ->
                val currentIds = pois.map { it.id.toString() }.toSet()
                pois.forEach { poi ->
                    firestore.collection("pois").document(poi.id.toString()).set(poi)
                }

                val removedIds = syncedPoiIds - currentIds
                removedIds.forEach { poiId ->
                    firestore.collection("pois").document(poiId).delete()
                }
                syncedPoiIds = currentIds
            }
        }

        firestore.collection("pois").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                scope.launch(Dispatchers.IO) {
                    val poiId = change.document.id.toLongOrNull() ?: return@launch
                    when (change.type) {
                        DocumentChange.Type.REMOVED -> veegDao.deletePoiById(poiId)
                        DocumentChange.Type.ADDED,
                        DocumentChange.Type.MODIFIED -> {
                            val remotePoi = change.document.toPoiOrNull(poiId) ?: return@launch
                            val localPoi = veegDao.getPoiById(poiId)
                            if (localPoi != remotePoi) {
                                veegDao.upsertPoi(remotePoi)
                            }
                        }
                    }
                }
            }
        }

        // Listen for cloud changes (optional for real-time task creation from web)
        firestore.collection("tasks").addSnapshotListener { _, _ -> }
    }
    
    fun uploadPoi(poi: Poi) {
        firestore.collection("pois").document(poi.id.toString()).set(poi)
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toPoiOrNull(poiId: Long): Poi? {
    val routeId = getLong("routeId") ?: return null
    val latitude = getDouble("latitude") ?: return null
    val longitude = getDouble("longitude") ?: return null

    return Poi(
        id = poiId,
        routeId = routeId,
        type = getString("type").orEmpty().ifBlank { "Punt" },
        latitude = latitude,
        longitude = longitude,
        description = getString("description"),
        status = getString("status").orEmpty().ifBlank { "open" },
        actionTaken = getString("actionTaken"),
        followUpAction = getString("followUpAction"),
        imageUri = getString("imageUri"),
        photoUris = getString("photoUris"),
        workLog = getString("workLog"),
        completedAt = getLong("completedAt"),
        updatedAt = getLong("updatedAt") ?: System.currentTimeMillis(),
        timestamp = getLong("timestamp") ?: System.currentTimeMillis()
    )
}
