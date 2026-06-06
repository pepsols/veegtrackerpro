package com.example.veegtrackerpro.ui.admin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.gpx.parser.GpxParser
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.util.PdfGenerator
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as VeegApplication).database
    private val gpxParser = GpxParser()

    val allRoutes = db.veegDao().getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints

    private val _trackingPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackingPoints: StateFlow<List<GeoPoint>> = _trackingPoints

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pois = _selectedRoute.flatMapLatest { route ->
        if (route != null) db.veegDao().getPoisForRoute(route.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectRoute(route: Route) {
        _selectedRoute.value = route
        viewModelScope.launch {
            // Load route points
            if (route.gpxData.isNotEmpty()) {
                val gpx = gpxParser.parse(route.gpxData.byteInputStream())
                val points = gpx?.tracks?.flatMap { track ->
                    track.segments?.flatMap { segment ->
                        segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
                    } ?: emptyList()
                } ?: emptyList()
                _routePoints.value = points
            } else {
                _routePoints.value = emptyList()
            }

            db.veegDao().getTrackingPointsForRoute(route.id).collectLatest { points ->
                _trackingPoints.value = points.map { GeoPoint(it.latitude, it.longitude) }
            }
        }
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch {
            db.veegDao().deleteRoute(route)
            if (_selectedRoute.value?.id == route.id) {
                _selectedRoute.value = null
                _trackingPoints.value = emptyList()
            }
        }
    }

    fun updateRouteImage(route: Route, uri: Uri?) {
        viewModelScope.launch {
            val updatedRoute = route.copy(imageUri = uri?.toString())
            db.veegDao().updateRoute(updatedRoute)
            if (_selectedRoute.value?.id == route.id) {
                _selectedRoute.value = updatedRoute
            }
        }
    }

    fun updateRouteComments(route: Route, comments: String) {
        viewModelScope.launch {
            val updatedRoute = route.copy(comments = comments)
            db.veegDao().updateRoute(updatedRoute)
            if (_selectedRoute.value?.id == route.id) {
                _selectedRoute.value = updatedRoute
            }
        }
    }

    fun addPoi(routeId: Long, type: String, latitude: Double, longitude: Double, description: String?) {
        viewModelScope.launch {
            db.veegDao().insertPoi(Poi(routeId = routeId, type = type, latitude = latitude, longitude = longitude, description = description))
        }
    }

    fun deletePoi(poi: Poi) {
        viewModelScope.launch {
            db.veegDao().deletePoi(poi)
        }
    }

    fun importGpx(inputStream: InputStream) {
        viewModelScope.launch {
            val content = inputStream.bufferedReader().use { it.readText() }
            val gpx = gpxParser.parse(content.byteInputStream())
            if (gpx != null) {
                val routeId = db.veegDao().insertRoute(
                    Route(
                        name = gpx.metadata?.name ?: "Imported Route",
                        gpxData = content
                    )
                )
                
                // Automatically import waypoints as POIs
                gpx.waypoints?.forEach { wpt ->
                    db.veegDao().insertPoi(
                        Poi(
                            routeId = routeId,
                            type = wpt.name ?: "Object",
                            latitude = wpt.lat,
                            longitude = wpt.lon,
                            description = wpt.desc ?: "Geïmporteerd uit GPX"
                        )
                    )
                }
            }
        }
    }

    fun generatePdf(route: Route): File? {
        val points = _trackingPoints.value
        return PdfGenerator.generateRouteReport(getApplication(), route, points.size)
    }
}
