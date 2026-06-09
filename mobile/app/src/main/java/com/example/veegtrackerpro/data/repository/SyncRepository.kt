package com.example.veegtrackerpro.data.repository

import com.example.veegtrackerpro.data.local.dao.VeegDao
import com.example.veegtrackerpro.data.local.entities.Poi
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
            veegDao.getAllRouteRuns().collectLatest { runs ->
                runs.forEach { run ->
                    firestore.collection("routeRuns").document(run.id.toString()).set(run)
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            veegDao.getAllTrackingPoints().collectLatest { points ->
                points.forEach { point ->
                    firestore.collection("trackingPoints").document(point.id.toString()).set(point)
                }
            }
        }

        // Listen for cloud changes (optional for real-time task creation from web)
        firestore.collection("tasks").addSnapshotListener { snapshot, _ ->
            snapshot?.documents?.forEach { doc ->
                // Handle new tasks from cloud
            }
        }
    }
    
    fun uploadPoi(poi: Poi) {
        firestore.collection("pois").document(poi.id.toString()).set(poi)
    }
}
