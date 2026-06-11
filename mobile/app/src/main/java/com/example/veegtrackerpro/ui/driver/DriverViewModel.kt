package com.example.veegtrackerpro.ui.driver

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.veegtrackerpro.VeegApplication
import com.example.veegtrackerpro.data.gpx.parser.GpxParser
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
import com.example.veegtrackerpro.data.local.entities.TrackingPoint
import com.example.veegtrackerpro.service.TrackingService
import com.example.veegtrackerpro.util.ProgressSnapshot
import com.example.veegtrackerpro.util.RouteProgressCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.InputStream
import java.util.Locale

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    sealed interface UiEvent {
        data class Message(val text: String) : UiEvent
    }

    private val db = (application as VeegApplication).database
    private val gpxParser = GpxParser()
    private var trackingObserverJob: Job? = null
    private var currentRouteId: Long = -1
    private var currentRunId: Long = -1

    var isTracking by mutableStateOf(false)
        private set

    var isFollowing by mutableStateOf(true)

    private val _selectedRoute = MutableStateFlow<Route?>(null)
    val selectedRoute: StateFlow<Route?> = _selectedRoute

    private val _routePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val routePoints: StateFlow<List<GeoPoint>> = _routePoints

    private val _trackingPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackingPoints: StateFlow<List<GeoPoint>> = _trackingPoints

    private val _trackingSamples = MutableStateFlow<List<TrackingPoint>>(emptyList())
    val trackingSamples: StateFlow<List<TrackingPoint>> = _trackingSamples

    private val _currentInstruction = MutableStateFlow("")
    val currentInstruction: StateFlow<String> = _currentInstruction

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _coverage = MutableStateFlow(0)
    val coverage: StateFlow<Int> = _coverage

    private val _completedRoutePoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val completedRoutePoints: StateFlow<List<GeoPoint>> = _completedRoutePoints

    private val _distanceKm = MutableStateFlow(0.0)
    val distanceKm: StateFlow<Double> = _distanceKm

    private val _runStatus = MutableStateFlow(RouteRun.STATUS_NOT_STARTED)
    val runStatus: StateFlow<String> = _runStatus

    private val _currentRun = MutableStateFlow<RouteRun?>(null)
    val currentRun: StateFlow<RouteRun?> = _currentRun

    private val _events = MutableSharedFlow<UiEvent>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events

    val allRoutes = db.veegDao().getAllRoutes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun importGpx(inputStream: InputStream) {
        viewModelScope.launch {
            val content = inputStream.bufferedReader().use { it.readText() }
            val gpx = gpxParser.parse(content.byteInputStream())
            val points = gpx?.tracks?.flatMap { track ->
                track.segments?.flatMap { segment ->
                    segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
                } ?: emptyList()
            } ?: emptyList()

            if (points.isNotEmpty()) {
                val route = Route(
                    name = gpx?.metadata?.name ?: "Imported Route",
                    gpxData = content
                )
                val routeId = db.veegDao().insertRoute(route)
                selectRoute(route.copy(id = routeId))
                emitMessage("Route geimporteerd en direct geselecteerd.")
            } else {
                emitMessage("Geen bruikbare routepunten gevonden in dit bestand.")
            }
        }
    }

    fun selectRoute(route: Route) {
        _selectedRoute.value = route
        currentRouteId = route.id
        _trackingPoints.value = emptyList()
        _trackingSamples.value = emptyList()
        _runStatus.value = RouteRun.STATUS_NOT_STARTED
        _currentRun.value = null
        _progress.value = 0
        _coverage.value = 0
        _distanceKm.value = 0.0
        _completedRoutePoints.value = emptyList()

        viewModelScope.launch {
            _routePoints.value = parseRoutePoints(route)
            db.veegDao().getLatestRouteRunForRoute(route.id)?.let { latestRun ->
                currentRunId = latestRun.id
                _currentRun.value = latestRun
                _runStatus.value = latestRun.status
                _progress.value = latestRun.progressPercent
                _coverage.value = latestRun.coveragePercent
                _distanceKm.value = latestRun.distanceKm
                observeTrackingPoints(latestRun.id)
            }
        }

        emitMessage("Route ${route.name} staat klaar.")
    }

    fun ensureRouteSelected(routes: List<Route>) {
        if (_selectedRoute.value == null && routes.isNotEmpty()) {
            selectRoute(routes.first())
        }
    }

    fun toggleTracking() {
        if (currentRouteId == -1L) return
        if (isTracking) {
            stopRun()
        } else {
            startRun()
        }
    }

    private fun startRun() {
        viewModelScope.launch {
            val routeId = currentRouteId
            if (routeId == -1L) return@launch

            val runId = db.veegDao().insertRouteRun(
                RouteRun(
                    routeId = routeId,
                    status = RouteRun.STATUS_IN_PROGRESS
                )
            )
            currentRunId = runId
            val run = db.veegDao().getRouteRunById(runId) ?: return@launch
            _currentRun.value = run
            _runStatus.value = run.status
            isTracking = true
            observeTrackingPoints(runId)
            emitMessage("Route gestart. Tracking loopt.")

            val intent = Intent(getApplication(), TrackingService::class.java).apply {
                action = TrackingService.ACTION_START
                putExtra(TrackingService.EXTRA_ROUTE_ID, routeId)
                putExtra(TrackingService.EXTRA_RUN_ID, runId)
            }
            getApplication<Application>().startForegroundService(intent)
        }
    }

    private fun stopRun() {
        val runId = currentRunId
        if (runId == -1L) return

        viewModelScope.launch {
            val existingRun = db.veegDao().getRouteRunById(runId) ?: return@launch
            val completed = _coverage.value >= 90 && _progress.value >= 95
            val updatedRun = existingRun.copy(
                status = if (completed) RouteRun.STATUS_COMPLETED else RouteRun.STATUS_INCOMPLETE,
                progressPercent = _progress.value,
                coveragePercent = _coverage.value,
                distanceKm = _distanceKm.value,
                offRouteMeters = existingRun.offRouteMeters,
                finishedAt = System.currentTimeMillis()
            )
            db.veegDao().updateRouteRun(updatedRun)
            _currentRun.value = updatedRun
            _runStatus.value = updatedRun.status
            emitMessage(
                if (completed) {
                    "Route afgerond en opgeslagen."
                } else {
                    "Route gestopt. Controleer later de incomplete rit."
                }
            )
        }

        isTracking = false
        val intent = Intent(getApplication(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    private fun observeTrackingPoints(runId: Long) {
        trackingObserverJob?.cancel()
        trackingObserverJob = viewModelScope.launch {
            db.veegDao().getTrackingPointsForRun(runId).collectLatest { points ->
                _trackingSamples.value = points
                _trackingPoints.value = points.map { GeoPoint(it.latitude, it.longitude) }
                val snapshot = RouteProgressCalculator.calculate(_routePoints.value, points)
                applySnapshot(snapshot)
                persistRunProgress(snapshot)
            }
        }
    }

    private suspend fun persistRunProgress(snapshot: ProgressSnapshot) {
        val run = db.veegDao().getRouteRunById(currentRunId) ?: return
        val updatedRun = run.copy(
            status = if (isTracking) RouteRun.STATUS_IN_PROGRESS else run.status,
            progressPercent = snapshot.progressPercent,
            coveragePercent = snapshot.coveragePercent,
            distanceKm = snapshot.distanceKm,
            offRouteMeters = snapshot.maxOffRouteMeters
        )
        db.veegDao().updateRouteRun(updatedRun)
        _currentRun.value = updatedRun
        _runStatus.value = updatedRun.status
    }

    private fun applySnapshot(snapshot: ProgressSnapshot) {
        _progress.value = snapshot.progressPercent
        _coverage.value = snapshot.coveragePercent
        _distanceKm.value = snapshot.distanceKm
        _completedRoutePoints.value = _routePoints.value.take((snapshot.furthestIndex + 1).coerceAtLeast(0))
        _currentInstruction.value = buildInstruction(snapshot.furthestIndex)
    }

    private fun buildInstruction(furthestIndex: Int): String {
        val route = _routePoints.value
        if (route.size < 2 || furthestIndex >= route.lastIndex) {
            return "Bestemming bereikt"
        }

        val currentPoint = route[furthestIndex]
        val nextPoint = route[(furthestIndex + 1).coerceAtMost(route.lastIndex)]
        val distanceToNext = currentPoint.distanceToAsDouble(nextPoint).toInt()

        return if (furthestIndex < route.lastIndex - 5) {
            val futurePoint = route[(furthestIndex + 5).coerceAtMost(route.lastIndex)]
            val currentBearing = currentPoint.bearingTo(nextPoint)
            val futureBearing = nextPoint.bearingTo(futurePoint)
            val bearingDiff = normalizeAngle(futureBearing - currentBearing)
            when {
                bearingDiff > 20 -> "Sla rechtsaf over ${distanceToNext}m"
                bearingDiff < -20 -> "Sla linksaf over ${distanceToNext}m"
                else -> "Rechtdoor over ${distanceToNext}m"
            }
        } else {
            "Nog ${distanceToNext}m tot het einde"
        }
    }

    private fun normalizeAngle(angle: Double): Double {
        var adjusted = angle
        while (adjusted <= -180) adjusted += 360.0
        while (adjusted > 180) adjusted -= 360.0
        return adjusted
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

    fun formattedDistance(): String = String.format(Locale.getDefault(), "%.1f km", _distanceKm.value)

    private fun emitMessage(text: String) {
        _events.tryEmit(UiEvent.Message(text))
    }
}
