package com.example.veegtrackerpro.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import com.example.veegtrackerpro.data.local.entities.Poi
import org.osmdroid.util.BoundingBox
import android.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import androidx.core.content.ContextCompat

@Composable
fun VeegMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(52.0907, 5.1214), // Utrecht
    zoomLevel: Double = 15.0,
    routePoints: List<GeoPoint> = emptyList(),
    trackingPoints: List<GeoPoint> = emptyList(),
    pois: List<Poi> = emptyList(),
    isFollowing: Boolean = false,
    onMapLongClick: (GeoPoint) -> Unit = {},
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onDetach()
        }
    }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            val box = BoundingBox.fromGeoPoints(routePoints)
            mapView.zoomToBoundingBox(box, true, 100)
        }
    }

    LaunchedEffect(trackingPoints, isFollowing) {
        if (isFollowing && trackingPoints.isNotEmpty()) {
            mapView.controller.animateTo(trackingPoints.last())
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(zoomLevel)
                controller.setCenter(center)
                
                onMapReady(this)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            view.overlays.clear()
            
            // Map Click Listener for POIs
            val mReceive: MapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    return false
                }

                override fun longPressHelper(p: GeoPoint): Boolean {
                    onMapLongClick(p)
                    return true
                }
            }
            view.overlays.add(MapEventsOverlay(mReceive))

            if (routePoints.isNotEmpty()) {
                val routeLine = Polyline(view)
                routeLine.setPoints(routePoints)
                routeLine.outlinePaint.color = Color.BLUE
                routeLine.outlinePaint.strokeWidth = 10f
                view.overlays.add(routeLine)

                // Add Start Marker
                val startMarker = Marker(view)
                startMarker.position = routePoints.first()
                startMarker.title = "Start"
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                view.overlays.add(startMarker)
            }
            
            // Render POIs
            pois.forEach { poi ->
                val marker = Marker(view)
                marker.position = GeoPoint(poi.latitude, poi.longitude)
                marker.title = poi.type
                marker.snippet = poi.description
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // Use a proper marker icon
                marker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.marker_default)
                view.overlays.add(marker)
            }

            if (trackingPoints.isNotEmpty()) {
                val trackingLine = Polyline(view)
                trackingLine.setPoints(trackingPoints)
                trackingLine.outlinePaint.color = Color.MAGENTA
                trackingLine.outlinePaint.strokeWidth = 8f
                view.overlays.add(trackingLine)
                
                // Add current position marker
                trackingPoints.lastOrNull()?.let { lastPoint ->
                    val marker = Marker(view)
                    marker.position = lastPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    marker.icon = ContextCompat.getDrawable(context, org.osmdroid.library.R.drawable.person)
                    view.overlays.add(marker)
                }
            }
            
            view.invalidate()
        }
    )
}
