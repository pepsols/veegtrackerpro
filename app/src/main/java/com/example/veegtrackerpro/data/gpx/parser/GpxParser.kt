package com.example.veegtrackerpro.data.gpx.parser

import android.util.Xml
import com.example.veegtrackerpro.data.gpx.model.*
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

class GpxParser {
    fun parse(inputStream: InputStream): Gpx? {
        return try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var gpx: Gpx? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "gpx") {
                    gpx = readGpx(parser)
                }
                eventType = parser.next()
            }
            
            android.util.Log.d("GpxParser", "Parsed GPX with ${gpx?.tracks?.size ?: 0} tracks and ${gpx?.waypoints?.size ?: 0} waypoints")
            return gpx
        } catch (e: Exception) {
            android.util.Log.e("GpxParser", "Error parsing GPX", e)
            null
        }
    }

    private fun readGpx(parser: XmlPullParser): Gpx {
        var metadata: Metadata? = null
        val tracks = mutableListOf<Track>()
        val waypoints = mutableListOf<Waypoint>()

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "gpx")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "metadata" -> metadata = readMetadata(parser)
                    "trk" -> tracks.add(readTrack(parser))
                    "wpt" -> waypoints.add(readWaypoint(parser))
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }
        return Gpx(metadata, tracks, waypoints)
    }

    private fun readMetadata(parser: XmlPullParser): Metadata {
        var name: String? = null
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "metadata")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> name = readText(parser)
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }
        return Metadata(name)
    }

    private fun readWaypoint(parser: XmlPullParser): Waypoint {
        val lat = parser.getAttributeValue(null, "lat").toDouble()
        val lon = parser.getAttributeValue(null, "lon").toDouble()
        var name: String? = null
        var desc: String? = null

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "wpt")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "name" -> name = readText(parser)
                    "desc" -> desc = readText(parser)
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }
        return Waypoint(lat, lon, name, desc)
    }

    private fun readTrack(parser: XmlPullParser): Track {
        val segments = mutableListOf<TrackSegment>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "trk")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "trkseg" -> segments.add(readTrackSegment(parser))
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }
        return Track(segments)
    }

    private fun readTrackSegment(parser: XmlPullParser): TrackSegment {
        val points = mutableListOf<TrackPoint>()
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "trkseg")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "trkpt" -> points.add(readTrackPoint(parser))
                    else -> skip(parser)
                }
            }
            eventType = parser.next()
        }
        return TrackSegment(points)
    }

    private fun readTrackPoint(parser: XmlPullParser): TrackPoint {
        val lat = parser.getAttributeValue(null, "lat").toDouble()
        val lon = parser.getAttributeValue(null, "lon").toDouble()
        
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "trkpt")) {
            if (eventType == XmlPullParser.START_TAG) {
                skip(parser)
            }
            eventType = parser.next()
        }
        return TrackPoint(lat, lon)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.next()
        }
        return result
    }

    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}
