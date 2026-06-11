package com.example.veegtrackerpro.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.veegtrackerpro.data.local.entities.Poi
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.absoluteValue

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

            pois.forEach { poi ->
                val marker = Marker(view).apply {
                    position = GeoPoint(poi.latitude, poi.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = poi.type
                    subDescription = poi.description ?: "Geen beschrijving"
                    icon = PoiMarkerAssetStore.iconForPoi(context, poi)
                }
                view.overlays.add(marker)
            }
            
            view.invalidate()
        }
    )
}

private object PoiMarkerAssetStore {
    private const val ASSET_DIR = "marker_photos/generated"
    private val assetPaths = mutableListOf<String>()
    private val bitmapCache = mutableMapOf<String, BitmapDrawable>()

    fun iconForPoi(context: Context, poi: Poi): BitmapDrawable? {
        ensureAssetList(context)
        if (assetPaths.isEmpty()) return null

        val key = "${poi.id}-${poi.type}-${poi.latitude}-${poi.longitude}"
        val assetPath = assetPaths[key.hashCode().absoluteValue % assetPaths.size]
        return bitmapCache.getOrPut(assetPath) {
            context.assets.open(assetPath).use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream) ?: fallbackBitmap()
                BitmapDrawable(context.resources, Bitmap.createScaledBitmap(bitmap, 54, 68, true))
            }
        }
    }

    private fun ensureAssetList(context: Context) {
        if (assetPaths.isNotEmpty()) return
        val files = context.assets.list(ASSET_DIR).orEmpty()
            .filter { it.endsWith(".png", ignoreCase = true) }
            .sorted()
        assetPaths += files.map { "$ASSET_DIR/$it" }
    }

    private fun fallbackBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(54, 68, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)
        return bitmap
    }
}
