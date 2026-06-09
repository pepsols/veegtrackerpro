package com.example.veegtrackerpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_runs",
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routeId")]
)
data class RouteRun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val status: String = STATUS_NOT_STARTED,
    val progressPercent: Int = 0,
    val coveragePercent: Int = 0,
    val distanceKm: Double = 0.0,
    val offRouteMeters: Double = 0.0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
) {
    companion object {
        const val STATUS_NOT_STARTED = "not_started"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_INCOMPLETE = "incomplete"
        const val STATUS_PAUSED = "paused"
    }
}
