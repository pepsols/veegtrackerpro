package com.example.veegtrackerpro.ui.waypoints

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class WaypointUiModel(
    val id: Long,
    val title: String,
    val description: String,
    val routeName: String,
    val latitude: Double,
    val longitude: Double
)

class ObjectWaypointsViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as VeegApplication).database.veegDao()

    val waypointItems: StateFlow<List<WaypointUiModel>> = combine(
        dao.getAllPois(),
        dao.getAllRoutes()
    ) { pois, routes ->
        buildWaypointItems(pois, routes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildWaypointItems(pois: List<Poi>, routes: List<Route>): List<WaypointUiModel> {
        val routesById = routes.associateBy { it.id }

        return pois.map { poi ->
            WaypointUiModel(
                id = poi.id,
                title = poi.type,
                description = poi.description?.takeIf { it.isNotBlank() } ?: "Geen extra beschrijving",
                routeName = routesById[poi.routeId]?.name ?: "Onbekende route",
                latitude = poi.latitude,
                longitude = poi.longitude
            )
        }
    }
}
