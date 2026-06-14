package com.example.veegtrackerpro.navigation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.veegtrackerpro.data.local.entities.Route
import org.osmdroid.util.GeoPoint

object OsmAndNavigator {
    private const val SCHEME = "osmand.api"
    private const val CMD_NAVIGATE = "navigate"
    private const val CMD_NAVIGATE_GPX = "navigate_gpx"
    private const val PARAM_DATA = "data"
    private const val PROFILE_CAR = "car"

    fun navigateToMarker(
        context: Context,
        latitude: Double,
        longitude: Double,
        name: String,
        profile: String = PROFILE_CAR
    ) {
        val uri = osmandUri(CMD_NAVIGATE)
            .appendQueryParameter("dest_lat", latitude.toString())
            .appendQueryParameter("dest_lon", longitude.toString())
            .appendQueryParameter("dest_name", name)
            .appendQueryParameter("profile", profile)
            .appendQueryParameter("force", "true")
            .appendQueryParameter("location_permission", "true")
            .build()

        val fallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(name)})")
        )
        startExternal(context, Intent(Intent.ACTION_VIEW, uri), fallback)
    }

    fun navigateRoute(
        context: Context,
        route: Route,
        routePoints: List<GeoPoint>,
        profile: String = PROFILE_CAR
    ) {
        val gpxData = route.gpxData.ifBlank {
            buildGpx(route.name, routePoints)
        }
        if (gpxData.isBlank()) {
            Toast.makeText(context, "Route heeft geen GPX-punten", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = osmandUri(CMD_NAVIGATE_GPX)
            .appendQueryParameter("force", "true")
            .appendQueryParameter("profile", profile)
            .build()

        val fallbackPoint = routePoints.firstOrNull()
        val fallback = fallbackPoint?.let { point ->
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(${Uri.encode(route.name)})")
            )
        }
        startExternal(
            context = context,
            intent = Intent(Intent.ACTION_VIEW, uri).putExtra(PARAM_DATA, gpxData),
            fallback = fallback
        )
    }

    private fun startExternal(context: Context, intent: Intent, fallback: Intent?) {
        val launchIntent = intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallbackIntent = fallback?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
        } catch (_: ActivityNotFoundException) {
            if (fallbackIntent != null) {
                context.startActivity(fallbackIntent)
            } else {
                Toast.makeText(context, "OsmAnd is niet geinstalleerd", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun osmandUri(command: String): Uri.Builder {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(command)
    }

    private fun buildGpx(name: String, routePoints: List<GeoPoint>): String {
        if (routePoints.isEmpty()) return ""
        val safeName = name.escapeXml()
        val points = routePoints.joinToString(separator = "\n") { point ->
            """      <trkpt lat="${point.latitude}" lon="${point.longitude}" />"""
        }
        return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<gpx version="1.1" creator="VeegtrackerPro" xmlns="http://www.topografix.com/GPX/1/1">
            |  <trk>
            |    <name>$safeName</name>
            |    <trkseg>
            |$points
            |    </trkseg>
            |  </trk>
            |</gpx>
        """.trimMargin()
    }

    private fun String.escapeXml(): String {
        return replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
