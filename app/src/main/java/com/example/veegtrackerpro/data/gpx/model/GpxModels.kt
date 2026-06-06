package com.example.veegtrackerpro.data.gpx.model

data class Gpx(
    val metadata: Metadata? = null,
    val tracks: List<Track>? = null,
    val waypoints: List<Waypoint>? = null
)

data class Metadata(
    val name: String? = null
)

data class Waypoint(
    val lat: Double,
    val lon: Double,
    val name: String? = null,
    val desc: String? = null
)

data class Track(
    val segments: List<TrackSegment>? = null
)

data class TrackSegment(
    val points: List<TrackPoint>? = null
)

data class TrackPoint(
    val lat: Double,
    val lon: Double
)
