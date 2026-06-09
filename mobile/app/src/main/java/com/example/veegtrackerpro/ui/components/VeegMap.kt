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
import com.example.veegtrackerpro.data.local.entities.Poi
import org.osmdroid.util.BoundingBox
import android.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

@Composable
fun VeegMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(52.0907, 5.1214), // Utrecht
    zoomLevel: Double = 15.0,
    routePoints: List<GeoPoint> = emptyList(),
    completedRoutePoints: List<GeoPoint> = emptyList(),
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

                if (completedRoutePoints.size > 1) {
                    val completedLine = Polyline(view)
                    completedLine.setPoints(completedRoutePoints)
                    completedLine.outlinePaint.color = Color.GREEN
                    completedLine.outlinePaint.strokeWidth = 14f
                    view.overlays.add(completedLine)
                }

            }

            if (trackingPoints.isNotEmpty()) {
                val trackingLine = Polyline(view)
                trackingLine.setPoints(trackingPoints)
                trackingLine.outlinePaint.color = Color.MAGENTA
                trackingLine.outlinePaint.strokeWidth = 8f
                view.overlays.add(trackingLine)
            }
            
            view.invalidate()
        }
    )
}
