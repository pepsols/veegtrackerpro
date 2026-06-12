package com.example.veegtrackerpro.ui.driver

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.storage.FirebaseStorage
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.Manifest
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

data class NearbyRouteSuggestion(
    val route: Route,
    val distanceToRouteMeters: Int,
    val distanceToStartMeters: Int
)

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as VeegApplication).database
    private val gpxParser = GpxParser()
    private val storage = FirebaseStorage.getInstance()
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val routeGeometryCache = mutableMapOf<Long, List<GeoPoint>>()

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

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    val currentLocation: StateFlow<GeoPoint?> = _currentLocation

    private val _nearbyRouteSuggestion = MutableStateFlow<NearbyRouteSuggestion?>(null)
    val nearbyRouteSuggestion: StateFlow<NearbyRouteSuggestion?> = _nearbyRouteSuggestion

    private var currentRouteId: Long = -1
    private var trackingObservationJob: Job? = null
    private var poiObservationJob: Job? = null
    private var isLocationAwarenessStarted = false

    private val passiveLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                _currentLocation.value = GeoPoint(location.latitude, location.longitude)
                updateNearbyRouteSuggestion()
                updateTodayState()
            }
        }
    }

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
                    currentSelection == null -> updateTodayState()
                    routes.none { it.id == currentSelection.id } -> {
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
                    else -> {
                        _selectedRoute.value = routes.first { it.id == currentSelection.id }
                        updateTodayState()
                    }
                }
                updateNearbyRouteSuggestion()
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
                routeGeometryCache[routeId] = points
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

    fun selectRoute(route: Route, navigateToStart: Boolean = false) {
        viewModelScope.launch {
            applyRouteSelection(route, navigateToStart)
        }
    }

    fun startSuggestedRoute(route: Route) {
        viewModelScope.launch {
            applyRouteSelection(route, navigateToStart = false)
            if (!isTracking) {
                toggleTracking()
            }
            _nearbyRouteSuggestion.value = null
        }
    }

    fun dismissRouteSuggestion() {
        _nearbyRouteSuggestion.value = null
    }

    fun startLocationAwareness() {
        if (isLocationAwarenessStarted) return
        val app = getApplication<Application>()
        val fineGranted = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) return
        isLocationAwarenessStarted = true

        runCatching {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    _currentLocation.value = GeoPoint(it.latitude, it.longitude)
                    updateNearbyRouteSuggestion()
                    updateTodayState()
                }
            }
            fusedLocationClient.requestLocationUpdates(
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L)
                    .setMinUpdateIntervalMillis(4_000L)
                    .build(),
                passiveLocationCallback,
                getApplication<Application>().mainLooper
            )
        }.onFailure {
            isLocationAwarenessStarted = false
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
            val normalizedPhotos = uploadOrReusePhotoUris(poi, photoUris, timestamp)
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
                completedAt = if (status.equals("afgerond", ignoreCase = true)
                    || status.equals("done", ignoreCase = true)
                    || status.equals("closed", ignoreCase = true)
                ) timestamp else poi.completedAt,
                updatedAt = timestamp
            )
            db.veegDao().updatePoi(updatedPoi)
        }
    }

    private suspend fun uploadOrReusePhotoUris(
        poi: Poi,
        photoUris: List<Uri>,
        timestamp: Long
    ): List<String> {
        return photoUris.mapIndexedNotNull { index, uri ->
            val rawValue = uri.toString()
            when {
                rawValue.startsWith("http://", ignoreCase = true) || rawValue.startsWith("https://", ignoreCase = true) -> rawValue
                else -> runCatching {
                    val ref = storage.reference
                        .child("poi-photos/${poi.routeId}/${poi.id}/$timestamp-$index.jpg")
                    ref.putFile(uri).await()
                    ref.downloadUrl.await().toString()
                }.getOrElse { rawValue }
            }
        }.distinct()
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
        return routeGeometry(route).isNotEmpty()
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
        val currentLocation = _currentLocation.value
        val startDistance = if (selectedRoute != null && routePoints.isNotEmpty() && currentLocation != null) {
            currentLocation.distanceToAsDouble(routePoints.first()).roundToInt()
        } else null
        val blocker = when {
            selectedRoute == null -> "Kies een route om te beginnen"
            routePoints.isEmpty() -> "Routegegevens ontbreken nog"
            isTracking && trackedPoints.isEmpty() -> "Wachten op GPS-signaal"
            !isTracking && trackedPoints.isEmpty() && startDistance != null -> "Startpunt ligt op ${startDistance} m"
            !isTracking && trackedPoints.isEmpty() -> "Tracking is nog niet gestart"
            !isTracking && trackedPoints.isNotEmpty() && _progress.value < 100 -> "Route is gepauzeerd"
            else -> null
        }
        val nextAction = when {
            selectedRoute == null -> "Selecteer een route"
            routePoints.isEmpty() -> "Importeer of herlaad de route"
            isTracking && _currentInstruction.value.isNotBlank() -> _currentInstruction.value
            isTracking -> "Volg de route"
            trackedPoints.isEmpty() && startDistance != null -> "Navigeer naar startpunt of start direct vanaf je positie"
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

    private fun updateNearbyRouteSuggestion() {
        if (isTracking) {
            _nearbyRouteSuggestion.value = null
            return
        }
        val location = _currentLocation.value ?: return
        val routes = availableRoutes.value
        val suggestion = routes
            .mapNotNull { route ->
                val points = routeGeometry(route)
                val startPoint = points.firstOrNull() ?: return@mapNotNull null
                val nearestMeters = points.minOfOrNull { point -> location.distanceToAsDouble(point) }?.roundToInt() ?: return@mapNotNull null
                val startMeters = location.distanceToAsDouble(startPoint).roundToInt()
                if (nearestMeters > 120 && startMeters > 250) return@mapNotNull null
                NearbyRouteSuggestion(route, nearestMeters, startMeters)
            }
            .sortedWith(compareBy<NearbyRouteSuggestion> { it.distanceToRouteMeters }.thenBy { it.distanceToStartMeters })
            .firstOrNull()

        val selected = _selectedRoute.value
        _nearbyRouteSuggestion.value = when {
            suggestion == null -> null
            selected?.id == suggestion.route.id -> null
            else -> suggestion
        }
    }

    private fun routeGeometry(route: Route): List<GeoPoint> {
        return routeGeometryCache.getOrPut(route.id) {
            if (route.gpxData.isBlank()) emptyList() else parseRoutePoints(route.gpxData)
        }
    }

    private suspend fun applyRouteSelection(route: Route, navigateToStart: Boolean) {
        currentRouteId = route.id
        _selectedRoute.value = route
        _trackingPoints.value = emptyList()
        _progress.value = 0

        if (route.gpxData.isNotEmpty()) {
            val points = parseRoutePoints(route.gpxData)
            routeGeometryCache[route.id] = points
            _routePoints.value = points
        } else {
            _routePoints.value = emptyList()
        }

        val startPoint = _routePoints.value.firstOrNull()
        _currentInstruction.value = when {
            navigateToStart && startPoint != null -> "Navigeer naar startpunt"
            else -> "Route geladen"
        }
        if (navigateToStart && startPoint != null) {
            launchNavigationTo(startPoint)
        }

        observeTrackingPoints(route.id)
        updateNearbyRouteSuggestion()
        updateTodayState()
    }

    private fun launchNavigationTo(point: GeoPoint) {
        val app = getApplication<Application>()
        val uri = Uri.parse("google.navigation:q=${point.latitude},${point.longitude}&mode=b")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(Startpunt route)")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
            .recoverCatching { app.startActivity(fallbackIntent) }
    }

    override fun onCleared() {
        super.onCleared()
        if (isLocationAwarenessStarted) {
            fusedLocationClient.removeLocationUpdates(passiveLocationCallback)
        }
    }
}
