package com.example.veegtrackerpro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.veegtrackerpro.data.local.dao.VeegDao
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.TrackingPoint

@Database(entities = [Route::class, TrackingPoint::class, Poi::class], version = 2, exportSchema = false)
abstract class VeegDatabase : RoomDatabase() {
    abstract fun veegDao(): VeegDao

    companion object {
        const val DATABASE_NAME = "veeg_tracker_db"
    }
}
