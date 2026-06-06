package com.example.veegtrackerpro.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class Route(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gpxData: String, // Storing raw GPX or a simplified version
    val imageUri: String? = null,
    val comments: String? = null,
    val distanceKm: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
