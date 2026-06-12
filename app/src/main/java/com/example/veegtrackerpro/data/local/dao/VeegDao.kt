package com.example.veegtrackerpro.data.local.dao

import androidx.room.*
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.TrackingPoint
import kotlinx.coroutines.flow.Flow

@Dao
interface VeegDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: Route): Long

    @Update
    suspend fun updateRoute(route: Route)

    @Query("SELECT * FROM routes ORDER BY createdAt DESC")
    fun getAllRoutes(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): Route?

    @Insert
    suspend fun insertTrackingPoint(point: TrackingPoint)

    @Query("SELECT * FROM tracking_points WHERE routeId = :routeId ORDER BY timestamp ASC")
    fun getTrackingPointsForRoute(routeId: Long): Flow<List<TrackingPoint>>

    @Delete
    suspend fun deleteRoute(route: Route)

    @Insert
    suspend fun insertPoi(poi: Poi)

    @Update
    suspend fun updatePoi(poi: Poi)

    @Delete
    suspend fun deletePoi(poi: Poi)

    @Query("SELECT * FROM pois WHERE routeId = :routeId ORDER BY timestamp DESC")
    fun getPoisForRoute(routeId: Long): Flow<List<Poi>>
}
