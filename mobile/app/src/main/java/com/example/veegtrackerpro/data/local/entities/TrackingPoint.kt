package com.example.veegtrackerpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracking_points",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteRun::class,
            parentColumns = ["id"],
            childColumns = ["runId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId"), Index("runId")]
)
data class TrackingPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val runId: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)
