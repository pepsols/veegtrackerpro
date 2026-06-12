package com.example.veegtrackerpro.ui.driver

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.gpx.parser.GpxParser
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.service.TrackingService
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.roundToInt

data class DriverTodayState(
    val routeName: String? = null,
    val nextAction: String = "Selecteer een route",
    val blocker: String? = "Kies een route om te beginnen",
    val progress: Int = 0,
    val trackedPoints: Int = 0,
    val totalRoutePoints: Int = 0,
    val isTracking: Boolean = false
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as VeegApplication).database
    private val gpxParser = GpxParser()

    var isTracking by mutableStateOf(false)
        private set

    var isFollowing by mutableStateOf(true)

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints

    val allRoutes = db.veegDao().getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableRoutes = allRoutes
        .map { routes -> routes.filter(::hasRouteGeometry) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _trackingPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackingPoints: StateFlow<List<GeoPoint>> = _trackingPoints

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois: StateFlow<List<Poi>> = _pois

    private val _currentInstruction = MutableStateFlow<String>("")
    val currentInstruction: StateFlow<String> = _currentInstruction

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute

    private val _todayState = MutableStateFlow(DriverTodayState())
    val todayState: StateFlow<DriverTodayState> = _todayState

    private var currentRouteId: Long = -1
    private var trackingObservationJob: Job? = null
    private var poiObservationJob: Job? = null

    init {
        viewModelScope.launch {
            availableRoutes.collectLatest { routes ->
                val currentSelection = _selectedRoute.value
                when {
                    routes.isEmpty() -> {
                        currentRouteId = -1
                        _selectedRoute.value = null
                        _routePoints.value = emptyList()
                        _trackingPoints.value = emptyList()
                        _currentInstruction.value = ""
                        _progress.value = 0
                        trackingObservationJob?.cancel()
                        poiObservationJob?.cancel()
                        updateTodayState()
                    }
                    currentSelection == null -> selectRoute(prioritizedRoute(routes))
                    routes.none { it.id == currentSelection.id } -> selectRoute(prioritizedRoute(routes))
                    else -> {
                        _selectedRoute.value = routes.first { it.id == currentSelection.id }
                        updateTodayState()
                    }
                }
            }
        }
    }

    fun importGpx(inputStream: InputStream) {
        viewModelScope.launch {
            val gpxContent = inputStream.bufferedReader().use { it.readText() }
            val gpx = gpxParser.parse(gpxContent.byteInputStream())
            val points = gpx?.tracks?.flatMap { track ->
                track.segments?.flatMap { segment ->
                    segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
                } ?: emptyList()
            } ?: emptyList()

            if (points.isNotEmpty()) {
                val route = Route(
                    name = gpx?.metadata?.name ?: "Imported Route",
                    gpxData = gpxContent,
                    distanceKm = calculateRouteDistanceKm(points)
                )
                val routeId = db.veegDao().insertRoute(route)
                val savedRoute = route.copy(id = routeId)
                currentRouteId = routeId
                _selectedRoute.value = savedRoute
                _routePoints.value = points
                _trackingPoints.value = emptyList()
                _currentInstruction.value = "Start de ritregistratie"
                _progress.value = 0
                observeTrackingPoints(routeId)
                updateTodayState()
            }
        }
    }

    private fun observeTrackingPoints(routeId: Long) {
        trackingObservationJob?.cancel()
        trackingObservationJob = viewModelScope.launch {
            db.veegDao().getTrackingPointsForRoute(routeId).collectLatest { points ->
                val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                _trackingPoints.value = geoPoints
                updateLiveGeopath(geoPoints)
                updateTodayState()
            }
        }
        poiObservationJob?.cancel()
        poiObservationJob = viewModelScope.launch {
            db.veegDao().getPoisForRoute(routeId).collectLatest { pois ->
                _pois.value = pois
            }
        }
    }

    private fun updateLiveGeopath(trackedPoints: List<GeoPoint>) {
        val route = _routePoints.value
        if (route.isEmpty() || trackedPoints.isEmpty()) {
            if (trackedPoints.isEmpty()) {
                _currentInstruction.value = if (isTracking) "Wachten op GPS-signaal" else "Start de ritregistratie"
            }
            updateTodayState()
            return
        }

        val lastTracked = trackedPoints.last()
        
        // Find nearest point on route
        var minDistance = Double.MAX_VALUE
        var nearestIndex = -1
        
        for (i in route.indices) {
            val dist = lastTracked.distanceToAsDouble(route[i])
            if (dist < minDistance) {
                minDistance = dist
                nearestIndex = i
            }
        }

        if (nearestIndex != -1) {
            // Calculate progress
            val prog = ((nearestIndex.toFloat() / (route.size - 1)) * 100).toInt()
            _progress.value = prog

            // Navigation logic
            if (nearestIndex < route.size - 1) {
                val nextPoint = route[nearestIndex + 1]
                val distanceToNext = lastTracked.distanceToAsDouble(nextPoint)
                
                val instruction = if (nearestIndex < route.size - 5) {
                    val currentBearing = lastTracked.bearingTo(nextPoint)
                    val futurePoint = route[Math.min(nearestIndex + 5, route.size - 1)]
                    val futureBearing = nextPoint.bearingTo(futurePoint)
                    
                    val bearingDiff = normalizeAngle(futureBearing - currentBearing)
                    
                    val turn = when {
                        bearingDiff > 20 -> "Sla rechtsaf over ${distanceToNext.toInt()}m"
                        bearingDiff < -20 -> "Sla linksaf over ${distanceToNext.toInt()}m"
                        else -> "Rechtdoor over ${distanceToNext.toInt()}m"
                    }
                    turn
                } else {
                    "Bestemming bereikt"
                }
                _currentInstruction.value = instruction
            }
        }

        updateTodayState()
    }

    private fun normalizeAngle(angle: Double): Double {
        var a = angle
        while (a <= -180) a += 360
        while (a > 180) a -= 360
        return a
    }

    fun selectRoute(route: Route) {
        viewModelScope.launch {
            currentRouteId = route.id
            _selectedRoute.value = route
            _trackingPoints.value = emptyList()
            _currentInstruction.value = "Route geladen"
            _progress.value = 0
            
            // Parse the stored GPX data to show the route polyline
            if (route.gpxData.isNotEmpty()) {
                _routePoints.value = parseRoutePoints(route.gpxData)
            } else {
                _routePoints.value = emptyList()
            }

            observeTrackingPoints(route.id)
            updateTodayState()
        }
    }

    fun toggleTracking() {
        if (currentRouteId == -1L) return

        isTracking = !isTracking
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = if (isTracking) TrackingService.ACTION_START else TrackingService.ACTION_STOP
            putExtra(TrackingService.EXTRA_ROUTE_ID, currentRouteId)
        }
        
        if (isTracking) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }

        updateTodayState()
    }

    fun updatePoi(
        poi: Poi,
        status: String,
        actionTaken: String,
        followUpAction: String,
        note: String,
        photoUris: List<Uri>
    ) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val normalizedPhotos = photoUris.map(Uri::toString).distinct()
            val logEntry = buildString {
                append(formatTimestamp(timestamp))
                append(" - ")
                append(status)
                if (actionTaken.isNotBlank()) {
                    append(" / ")
                    append(actionTaken)
                }
                if (followUpAction.isNotBlank()) {
                    append(" / ")
                    append(followUpAction)
                }
                if (note.isNotBlank()) {
                    append(": ")
                    append(note.trim())
                }
            }
            val mergedLog = listOfNotNull(logEntry, poi.workLog)
                .joinToString(separator = "\n")
                .trim()
            val updatedPoi = poi.copy(
                status = status,
                actionTaken = actionTaken.ifBlank { poi.actionTaken },
                followUpAction = followUpAction.ifBlank { poi.followUpAction },
                description = if (note.isBlank()) poi.description else note.trim(),
                imageUri = normalizedPhotos.firstOrNull() ?: poi.imageUri,
                photoUris = normalizedPhotos.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n") ?: poi.photoUris,
                workLog = mergedLog,
                updatedAt = timestamp
            )
            db.veegDao().updatePoi(updatedPoi)
        }
    }

    private fun parseRoutePoints(gpxData: String): List<GeoPoint> {
        val gpx = gpxParser.parse(gpxData.byteInputStream())
        return gpx?.tracks?.flatMap { track ->
            track.segments?.flatMap { segment ->
                segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
            } ?: emptyList()
        } ?: emptyList()
    }

    private fun hasRouteGeometry(route: Route): Boolean {
        if (route.gpxData.isBlank()) return false
        return parseRoutePoints(route.gpxData).isNotEmpty()
    }

    private fun prioritizedRoute(routes: List<Route>): Route {
        return routes.sortedWith(
            compareByDescending<Route> { it.gpxData.contains("<wpt", ignoreCase = true) }
                .thenByDescending { it.createdAt }
        ).first()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat("dd-MM HH:mm", java.util.Locale("nl", "NL"))
        return formatter.format(java.util.Date(timestamp))
    }

    private fun calculateRouteDistanceKm(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        val meters = points.zipWithNext().sumOf { (start, end) -> start.distanceToAsDouble(end) }
        return ((meters / 1000.0) * 10).roundToInt() / 10.0
    }

    private fun updateTodayState() {
        val selectedRoute = _selectedRoute.value
        val routePoints = _routePoints.value
        val trackedPoints = _trackingPoints.value
        val blocker = when {
            selectedRoute == null -> "Kies een route om te beginnen"
            routePoints.isEmpty() -> "Routegegevens ontbreken nog"
            isTracking && trackedPoints.isEmpty() -> "Wachten op GPS-signaal"
            !isTracking && trackedPoints.isEmpty() -> "Tracking is nog niet gestart"
            !isTracking && trackedPoints.isNotEmpty() && _progress.value < 100 -> "Route is gepauzeerd"
            else -> null
        }
        val nextAction = when {
            selectedRoute == null -> "Selecteer een route"
            routePoints.isEmpty() -> "Importeer of herlaad de route"
            isTracking && _currentInstruction.value.isNotBlank() -> _currentInstruction.value
            isTracking -> "Volg de route"
            trackedPoints.isEmpty() -> "Start de ritregistratie"
            _progress.value >= 100 -> "Controleer en rond de route af"
            else -> "Hervat de route wanneer je verder gaat"
        }

        _todayState.value = DriverTodayState(
            routeName = selectedRoute?.name,
            nextAction = nextAction,
            blocker = blocker,
            progress = _progress.value,
            trackedPoints = trackedPoints.size,
            totalRoutePoints = routePoints.size,
            isTracking = isTracking
        )
    }
}
