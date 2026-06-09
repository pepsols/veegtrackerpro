package com.example.veegtrackerpro

import com.example.veegtrackerpro.data.local.entities.TrackingPoint
import com.example.veegtrackerpro.util.RouteProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint

class RouteProgressCalculatorTest {

    private val route = listOf(
        GeoPoint(52.0000, 5.0000),
        GeoPoint(52.0005, 5.0005),
        GeoPoint(52.0010, 5.0010),
        GeoPoint(52.0015, 5.0015),
        GeoPoint(52.0020, 5.0020)
    )

    @Test
    fun calculate_increasesProgressAlongRoute() {
        val snapshot = RouteProgressCalculator.calculate(
            routePoints = route,
            trackingPoints = listOf(
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0000, longitude = 5.0000),
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0010, longitude = 5.0010),
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0020, longitude = 5.0020)
            )
        )

        assertTrue(snapshot.progressPercent >= 100)
        assertTrue(snapshot.coveragePercent >= 100)
    }

    @Test
    fun calculate_ignoresPoorAccuracyPoints() {
        val snapshot = RouteProgressCalculator.calculate(
            routePoints = route,
            trackingPoints = listOf(
                TrackingPoint(
                    routeId = 1,
                    runId = 10,
                    latitude = 52.0100,
                    longitude = 5.0100,
                    accuracyMeters = 120f
                )
            )
        )

        assertEquals(0, snapshot.progressPercent)
        assertEquals(0, snapshot.coveragePercent)
        assertEquals(0.0, snapshot.distanceKm, 0.001)
    }

    @Test
    fun calculate_keepsMaxProgressWhenDriverTurnsBack() {
        val snapshot = RouteProgressCalculator.calculate(
            routePoints = route,
            trackingPoints = listOf(
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0000, longitude = 5.0000),
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0015, longitude = 5.0015),
                TrackingPoint(routeId = 1, runId = 10, latitude = 52.0010, longitude = 5.0010)
            )
        )

        assertTrue(snapshot.progressPercent >= 75)
        assertTrue(snapshot.coveragePercent >= 75)
    }
}
