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
import com.example.veegtrackerpro.service.TrackingService
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

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

    private val _trackingPoints = MutableStateFlow<List<GeoPoint>>(emptyList())
    val trackingPoints: StateFlow<List<GeoPoint>> = _trackingPoints

    private val _currentInstruction = MutableStateFlow<String>("")
    val currentInstruction: StateFlow<String> = _currentInstruction

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private var currentRouteId: Long = -1

    init {
        // In a real app, we might load the last active route
    }

    fun importGpx(inputStream: InputStream) {
        viewModelScope.launch {
            val gpx = gpxParser.parse(inputStream)
            val points = gpx?.tracks?.flatMap { track ->
                track.segments?.flatMap { segment ->
                    segment.points?.map { GeoPoint(it.lat, it.lon) } ?: emptyList()
                } ?: emptyList()
            } ?: emptyList()

            if (points.isNotEmpty()) {
                val routeId = db.veegDao().insertRoute(
                    Route(name = gpx?.metadata?.name ?: "Imported Route", gpxData = "")
                )
                currentRouteId = routeId
                _routePoints.value = points
                observeTrackingPoints(routeId)
            }
        }
    }

    private fun observeTrackingPoints(routeId: Long) {
        viewModelScope.launch {
            db.veegDao().getTrackingPointsForRoute(routeId).collectLatest { points ->
                val geoPoints = points.map { GeoPoint(it.latitude, it.longitude) }
                _trackingPoints.value = geoPoints
                updateLiveGeopath(geoPoints)
            }
        }
    }

    private fun updateLiveGeopath(trackedPoints: List<GeoPoint>) {
        val route = _routePoints.value
        if (route.isEmpty() || trackedPoints.isEmpty()) return

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
            _trackingPoints.value = emptyList()
            
            // Parse the stored GPX data to show the route polyline
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

            observeTrackingPoints(route.id)
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
    }
}
