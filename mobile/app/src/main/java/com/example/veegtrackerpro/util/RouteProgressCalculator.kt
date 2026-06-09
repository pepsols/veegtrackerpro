package com.example.veegtrackerpro.util

import com.example.veegtrackerpro.data.local.entities.TrackingPoint
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

data class ProgressSnapshot(
    val progressPercent: Int,
    val coveragePercent: Int,
    val distanceKm: Double,
    val maxOffRouteMeters: Double,
    val furthestIndex: Int
)

object RouteProgressCalculator {
    private const val MAX_MATCH_DISTANCE_METERS = 35.0
    private const val MAX_ACCURACY_METERS = 40.0

    fun calculate(routePoints: List<GeoPoint>, trackingPoints: List<TrackingPoint>): ProgressSnapshot {
        if (routePoints.size < 2 || trackingPoints.isEmpty()) {
            return ProgressSnapshot(0, 0, 0.0, 0.0, 0)
        }

        val visited = BooleanArray(routePoints.size)
        var furthestIndex = 0
        var maxOffRouteMeters = 0.0
        var distanceMeters = 0.0
        var previousAccepted: GeoPoint? = null

        trackingPoints.sortedBy { it.timestamp }.forEach { point ->
            val accuracy = point.accuracyMeters
            if (accuracy != null && accuracy > MAX_ACCURACY_METERS) {
                return@forEach
            }

            val current = GeoPoint(point.latitude, point.longitude)
            if (previousAccepted != null) {
                distanceMeters += previousAccepted!!.distanceToAsDouble(current)
            }

            val nearest = findNearestIndex(current, routePoints)
            val nearestDistance = current.distanceToAsDouble(routePoints[nearest])
            maxOffRouteMeters = maxOf(maxOffRouteMeters, nearestDistance)

            if (nearestDistance <= MAX_MATCH_DISTANCE_METERS) {
                visited[nearest] = true
                furthestIndex = maxOf(furthestIndex, nearest)
                markGapAsVisited(visited, furthestIndex)
                previousAccepted = current
            }
        }

        val progress = ((furthestIndex.toDouble() / (routePoints.lastIndex.coerceAtLeast(1))) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)
        val coverage = ((visited.count { it }.toDouble() / routePoints.size.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        return ProgressSnapshot(
            progressPercent = progress,
            coveragePercent = coverage,
            distanceKm = distanceMeters / 1000.0,
            maxOffRouteMeters = maxOffRouteMeters,
            furthestIndex = furthestIndex
        )
    }

    private fun findNearestIndex(point: GeoPoint, routePoints: List<GeoPoint>): Int {
        var nearestIndex = 0
        var minDistance = Double.MAX_VALUE
        routePoints.forEachIndexed { index, routePoint ->
            val distance = point.distanceToAsDouble(routePoint)
            if (distance < minDistance) {
                minDistance = distance
                nearestIndex = index
            }
        }
        return nearestIndex
    }

    private fun markGapAsVisited(visited: BooleanArray, furthestIndex: Int) {
        for (index in 0..furthestIndex.coerceAtMost(visited.lastIndex)) {
            visited[index] = true
        }
    }
}
