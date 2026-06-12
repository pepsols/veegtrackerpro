package com.example.veegtrackerpro.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.veegtrackerpro.data.local.entities.Poi
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun VeegMap(
    modifier: Modifier = Modifier,
    center: GeoPoint = GeoPoint(52.0907, 5.1214),
    zoomLevel: Double = 15.0,
    routePoints: List<GeoPoint> = emptyList(),
    trackingPoints: List<GeoPoint> = emptyList(),
    pois: List<Poi> = emptyList(),
    isFollowing: Boolean = false,
    onMapLongClick: (GeoPoint) -> Unit = {},
    onPoiClick: (Poi) -> Unit = {},
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

            val mapReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean = false

                override fun longPressHelper(p: GeoPoint): Boolean {
                    onMapLongClick(p)
                    return true
                }
            }
            view.overlays.add(MapEventsOverlay(mapReceiver))

            if (routePoints.isNotEmpty()) {
                val routeLine = Polyline(view)
                routeLine.setPoints(routePoints)
                routeLine.outlinePaint.color = Color.rgb(35, 88, 214)
                routeLine.outlinePaint.strokeWidth = 12f
                view.overlays.add(routeLine)

                val startMarker = Marker(view)
                startMarker.position = routePoints.first()
                startMarker.title = "Start"
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.icon = createPinBitmap(
                    label = "S",
                    fillColor = Color.rgb(34, 139, 34),
                    strokeColor = Color.WHITE
                ).let { android.graphics.drawable.BitmapDrawable(context.resources, it) }
                view.overlays.add(startMarker)
            }

            pois.forEach { poi ->
                val marker = Marker(view)
                marker.position = GeoPoint(poi.latitude, poi.longitude)
                marker.title = poi.type
                marker.snippet = buildString {
                    append(poi.status.replaceFirstChar { it.uppercase() })
                    if (!poi.description.isNullOrBlank()) {
                        append("\n")
                        append(poi.description)
                    }
                }
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.icon = android.graphics.drawable.BitmapDrawable(
                    context.resources,
                    createPinBitmap(
                        label = markerLabelFor(poi),
                        fillColor = markerColorFor(poi),
                        strokeColor = Color.WHITE
                    )
                )
                marker.setOnMarkerClickListener { _, _ ->
                    onPoiClick(poi)
                    true
                }
                view.overlays.add(marker)
            }

            if (trackingPoints.isNotEmpty()) {
                val trackingLine = Polyline(view)
                trackingLine.setPoints(trackingPoints)
                trackingLine.outlinePaint.color = Color.rgb(209, 77, 92)
                trackingLine.outlinePaint.strokeWidth = 8f
                view.overlays.add(trackingLine)

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

private fun markerLabelFor(poi: Poi): String {
    return when (poi.status.lowercase()) {
        "afgerond" -> "OK"
        "overgeslagen" -> "!"
        else -> when (poi.type.lowercase()) {
            "onkruid" -> "W"
            "obstakel" -> "O"
            "schade" -> "S"
            else -> "P"
        }
    }
}

private fun markerColorFor(poi: Poi): Int {
    return when (poi.status.lowercase()) {
        "afgerond" -> Color.rgb(41, 145, 69)
        "overgeslagen" -> Color.rgb(201, 94, 32)
        else -> Color.rgb(119, 76, 189)
    }
}

private fun createPinBitmap(
    label: String,
    fillColor: Int,
    strokeColor: Int
): Bitmap {
    val width = 84
    val height = 104
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 4f, Color.argb(80, 0, 0, 0))
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
    }

    val headBounds = RectF(12f, 6f, 72f, 66f)
    canvas.drawOval(headBounds, fillPaint)
    canvas.drawOval(headBounds, strokePaint)

    val tailPath = Path().apply {
        moveTo(42f, 98f)
        lineTo(24f, 56f)
        lineTo(60f, 56f)
        close()
    }
    canvas.drawPath(tailPath, fillPaint)
    canvas.drawPath(tailPath, strokePaint)

    canvas.drawText(label, 42f, 45f, textPaint)
    return bitmap
}
