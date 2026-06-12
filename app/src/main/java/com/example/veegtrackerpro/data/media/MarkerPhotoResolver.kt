package com.example.veegtrackerpro.data.media

import android.content.Context
import android.net.Uri
import com.example.veegtrackerpro.data.local.entities.Poi
import java.text.Normalizer

object MarkerPhotoResolver {
    private const val MARKER_ASSET_DIR = "web/marker-images"

    @Volatile
    private var cachedMarkerFiles: List<String>? = null

    fun resolvePoiPreviewUri(context: Context, poi: Poi): String? {
        return storedPhotoUris(poi).firstOrNull()
            ?: resolveMarkerAssetUri(context, poi.type, poi.description)
    }

    fun resolveMarkerAssetUri(context: Context, type: String, description: String?): String? {
        val files = markerPhotoFiles(context)
        if (files.isEmpty()) return null

        val signal = normalizeLookupText("$type ${description.orEmpty()}")
        val numericPrefix = extractLeadingNumber(description ?: type)
        val exactNumericMatch = numericPrefix
            ?.takeIf { it.isNotBlank() }
            ?.let { prefix ->
                files.firstOrNull { fileName ->
                    normalizeLookupText(fileName).startsWith("$prefix foto")
                }
            }
        if (exactNumericMatch != null) {
            return assetUri(exactNumericMatch)
        }

        val bestMatch = files
            .map { fileName -> fileName to scorePoiPhotoMatch(signal, normalizeLookupText(fileName)) }
            .maxByOrNull { (_, score) -> score }

        return if (bestMatch != null && bestMatch.second >= 2) assetUri(bestMatch.first) else null
    }

    fun storedPhotoUris(poi: Poi): List<String> {
        val photoUris = poi.photoUris
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.toList()
            .orEmpty()
        val imageUri = poi.imageUri?.trim()?.takeIf { it.isNotBlank() }
        return buildList {
            imageUri?.let(::add)
            addAll(photoUris)
        }.distinct()
    }

    private fun markerPhotoFiles(context: Context): List<String> {
        cachedMarkerFiles?.let { return it }
        return synchronized(this) {
            cachedMarkerFiles ?: context.assets.list(MARKER_ASSET_DIR)
                ?.filter { it.endsWith(".jpg", ignoreCase = true) || it.endsWith(".jpeg", ignoreCase = true) }
                ?.sorted()
                .orEmpty()
                .also { cachedMarkerFiles = it }
        }
    }

    private fun assetUri(fileName: String): String {
        return "file:///android_asset/$MARKER_ASSET_DIR/${Uri.encode(fileName)}"
    }

    private fun normalizeLookupText(value: String): String {
        val normalized = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return normalized
            .replace("[()_,./-]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun extractLeadingNumber(value: String): String? {
        return "\\b(\\d{1,2})\\b".toRegex().find(value)?.groupValues?.getOrNull(1)
    }

    private fun scorePoiPhotoMatch(signal: String, fileSignal: String): Int {
        if (signal.isBlank() || fileSignal.isBlank()) return 0
        var score = 0
        signal.split(" ")
            .filter { it.length >= 4 }
            .forEach { token ->
                if (fileSignal.contains(token)) score += 2
            }
        if (signal.contains("rijsenburgselaan") && fileSignal.contains("rijsenburgselaan")) score += 3
        if (signal.contains("n225") && fileSignal.contains("n225")) score += 3
        if (signal.contains("plateau") && fileSignal.contains("foto")) score += 1
        return score
    }
}
