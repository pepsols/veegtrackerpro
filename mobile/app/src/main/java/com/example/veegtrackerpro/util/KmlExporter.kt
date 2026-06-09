package com.example.veegtrackerpro.util

import android.content.Context
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
import org.osmdroid.util.GeoPoint
import java.io.File

object KmlExporter {
    fun exportRun(
        context: Context,
        route: Route,
        run: RouteRun,
        routePoints: List<GeoPoint>,
        trackingPoints: List<GeoPoint>,
        pois: List<Poi>
    ): File? {
        return runCatching {
            val directory = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(directory, "route_run_${run.id}.kml")
            file.writeText(buildKml(route, run, routePoints, trackingPoints, pois))
            file
        }.getOrNull()
    }

    private fun buildKml(
        route: Route,
        run: RouteRun,
        routePoints: List<GeoPoint>,
        trackingPoints: List<GeoPoint>,
        pois: List<Poi>
    ): String {
        val plannedCoordinates = routePoints.joinToString(" ") { "${it.longitude},${it.latitude},0" }
        val trackedCoordinates = trackingPoints.joinToString(" ") { "${it.longitude},${it.latitude},0" }
        val poiPlacemarks = pois.joinToString("\n") { poi ->
            """
            <Placemark>
              <name>${escape(poi.type)}</name>
              <description>${escape(poi.description ?: "")}</description>
              <Point><coordinates>${poi.longitude},${poi.latitude},0</coordinates></Point>
            </Placemark>
            """.trimIndent()
        }

        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <kml xmlns="http://www.opengis.net/kml/2.2">
              <Document>
                <name>${escape(route.name)} - run ${run.id}</name>
                <description>Status=${run.status}; progress=${run.progressPercent}; coverage=${run.coveragePercent}</description>
                <Placemark>
                  <name>Planned route</name>
                  <LineString><tessellate>1</tessellate><coordinates>$plannedCoordinates</coordinates></LineString>
                </Placemark>
                <Placemark>
                  <name>Tracked path</name>
                  <LineString><tessellate>1</tessellate><coordinates>$trackedCoordinates</coordinates></LineString>
                </Placemark>
                $poiPlacemarks
              </Document>
            </kml>
        """.trimIndent()
    }

    private fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
