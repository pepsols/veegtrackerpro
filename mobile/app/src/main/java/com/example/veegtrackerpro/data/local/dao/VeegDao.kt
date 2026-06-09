package com.example.veegtrackerpro.data.local.dao

import androidx.room.*
import com.example.veegtrackerpro.data.local.entities.Poi
import com.example.veegtrackerpro.data.local.entities.Route
import com.example.veegtrackerpro.data.local.entities.RouteRun
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

    @Query("SELECT * FROM tracking_points WHERE runId = :runId ORDER BY timestamp ASC")
    fun getTrackingPointsForRun(runId: Long): Flow<List<TrackingPoint>>

    @Query("SELECT * FROM tracking_points ORDER BY timestamp ASC")
    fun getAllTrackingPoints(): Flow<List<TrackingPoint>>

    @Delete
    suspend fun deleteRoute(route: Route)

    @Insert
    suspend fun insertPoi(poi: Poi)

    @Delete
    suspend fun deletePoi(poi: Poi)

    @Query("SELECT * FROM pois WHERE routeId = :routeId ORDER BY timestamp DESC")
    fun getPoisForRoute(routeId: Long): Flow<List<Poi>>

    @Query("SELECT * FROM pois ORDER BY timestamp DESC")
    fun getAllPois(): Flow<List<Poi>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouteRun(routeRun: RouteRun): Long

    @Update
    suspend fun updateRouteRun(routeRun: RouteRun)

    @Query("SELECT * FROM route_runs WHERE routeId = :routeId ORDER BY startedAt DESC")
    fun getRouteRunsForRoute(routeId: Long): Flow<List<RouteRun>>

    @Query("SELECT * FROM route_runs ORDER BY startedAt DESC")
    fun getAllRouteRuns(): Flow<List<RouteRun>>

    @Query("SELECT * FROM route_runs WHERE id = :runId LIMIT 1")
    suspend fun getRouteRunById(runId: Long): RouteRun?

    @Query("SELECT * FROM route_runs WHERE routeId = :routeId ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestRouteRunForRoute(routeId: Long): RouteRun?
}
