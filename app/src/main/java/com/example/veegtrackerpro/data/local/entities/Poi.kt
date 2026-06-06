package com.example.veegtrackerpro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pois",
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
data class Poi(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val type: String, // e.g., "Onkruid", "Obstakel", "Schade"
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
