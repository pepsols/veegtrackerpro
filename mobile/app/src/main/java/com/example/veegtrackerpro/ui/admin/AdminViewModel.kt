package com.example.veegtrackerpro.ui.admin

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.gpx.parser.GpxParser
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
import com.example.veegtrackerpro.util.KmlExporter
import com.example.veegtrackerpro.util.PdfGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.InputStream

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as VeegApplication).database
    private val gpxParser = GpxParser()
    private var routeRunsObserverJob: Job? = null
    private var runObserverJob: Job? = null

    val allRoutes = db.veegDao().getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute

    private val _selectedRun = MutableStateFlow<RouteRun?>(null)
    val selectedRun: StateFlow<RouteRun?> = _selectedRun

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints

    private val _trackingPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackingPoints: StateFlow<List<GeoPoint>> = _trackingPoints

    private val _routeRuns = MutableStateFlow<List<RouteRun>>(emptyList())
    val routeRuns: StateFlow<List<RouteRun>> = _routeRuns

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val pois = _selectedRoute.flatMapLatest { route ->
        if (route != null) db.veegDao().getPoisForRoute(route.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPoisByRouteId: StateFlow<Map<Long, List<Poi>>> = db.veegDao().getAllPois()
        .map { allPois -> allPois.groupBy { it.routeId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun selectRoute(route: Route) {
        _selectedRoute.value = route
        routeRunsObserverJob?.cancel()
        viewModelScope.launch {
            _routePoints.value = parseRoutePoints(route)
        }
        routeRunsObserverJob = viewModelScope.launch {
            db.veegDao().getRouteRunsForRoute(route.id).collectLatest { runs ->
                _routeRuns.value = runs
                val nextRun = _selectedRun.value?.let { selected ->
                    runs.firstOrNull { it.id == selected.id }
                } ?: runs.firstOrNull()
                selectRun(nextRun)
            }
        }
    }

    fun selectRun(run: RouteRun?) {
        _selectedRun.value = run
        runObserverJob?.cancel()
        if (run == null) {
            _trackingPoints.value = emptyList()
            return
        }

        runObserverJob = viewModelScope.launch {
            db.veegDao().getTrackingPointsForRun(run.id).collectLatest { points ->
                _trackingPoints.value = points.map { GeoPoint(it.latitude, it.longitude) }
            }
        }
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch {
            db.veegDao().deleteRoute(route)
            if (_selectedRoute.value?.id == route.id) {
                _selectedRoute.value = null
                _selectedRun.value = null
                _routeRuns.value = emptyList()
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
            val poi = Poi(
                routeId = routeId,
                type = type,
                latitude = latitude,
                longitude = longitude,
                description = description
            )
            db.veegDao().insertPoi(poi)
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

                gpx.waypoints?.forEach { wpt ->
                    db.veegDao().insertPoi(
                        Poi(
                            routeId = routeId,
                            type = wpt.name ?: "Object",
                            latitude = wpt.lat,
                            longitude = wpt.lon,
                            description = wpt.desc ?: "Geimporteerd uit GPX"
                        )
                    )
                }
            }
        }
    }

    fun generatePdf(route: Route): File? {
        return PdfGenerator.generateRouteReport(getApplication(), route, _trackingPoints.value.size)
    }

    fun exportSelectedRunKml(): File? {
        val route = _selectedRoute.value ?: return null
        val run = _selectedRun.value ?: return null
        return KmlExporter.exportRun(
            context = getApplication(),
            route = route,
            run = run,
            routePoints = _routePoints.value,
            trackingPoints = _trackingPoints.value,
            pois = pois.value
        )
    }

    private fun parseRoutePoints(route: Route): List<GeoPoint> {
        if (route.gpxData.isEmpty()) return emptyList()
        val gpx = gpxParser.parse(route.gpxData.byteInputStream())
        return gpx?.tracks?.flatMap { track ->
            track.segments?.flatMap { segment ->
                segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }
}
