package com.example.veegtrackerpro.data.local.init

import android.content.Context
import android.util.Log
import com.example.veegtrackerpro.data.gpx.parser.GpxParser
import com.example.veegtrackerpro.data.local.VeegDatabase
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AssetGpxImporter(private val context: Context, private val database: VeegDatabase) {

    private val gpxParser = GpxParser()

    suspend fun importFromAssets() = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("veeg_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPrefs.getBoolean("first_launch_import", true)

        if (isFirstLaunch || com.example.veegtrackerpro.BuildConfig.DEBUG) {
            try {
                val assetManager = context.assets
                val files = assetManager.list("routes") ?: emptyArray()
                
                Log.d("AssetGpxImporter", "Starting import. Found ${files.size} files in assets/routes")

                for (fileName in files) {
                    if (fileName.endsWith(".gpx")) {
                        Log.d("AssetGpxImporter", "Attempting to parse: $fileName")
                        val gpxContent = assetManager.open("routes/$fileName").use { it.bufferedReader().readText() }
                        assetManager.open("routes/$fileName").use { inputStream ->
                            val gpx = gpxParser.parse(inputStream)
                            if (gpx != null) {
                                val routeName = gpx.metadata?.name ?: fileName.removeSuffix(".gpx")
                                val routeId = database.veegDao().insertRoute(
                                    Route(name = routeName, gpxData = gpxContent)
                                )
                                
                                // Import waypoints from assets as well (only for markers file or if needed)
                                gpx.waypoints?.forEach { wpt ->
                                    database.veegDao().insertPoi(
                                        Poi(
                                            routeId = routeId,
                                            type = wpt.name ?: "Object",
                                            latitude = wpt.lat,
                                            longitude = wpt.lon,
                                            description = wpt.desc ?: "Geïmporteerd uit $fileName"
                                        )
                                    )
                                }
                                Log.d("AssetGpxImporter", "Successfully imported $routeName with ID $routeId")
                            } else {
                                Log.e("AssetGpxImporter", "Failed to parse $fileName")
                            }
                        }
                    }
                }
                
                sharedPrefs.edit().putBoolean("first_launch_import", false).apply()
            } catch (e: IOException) {
                Log.e("AssetGpxImporter", "Error reading GPX assets", e)
            }
        } else {
            Log.d("AssetGpxImporter", "Import skipped (not first launch and not debug)")
        }
    }
}
