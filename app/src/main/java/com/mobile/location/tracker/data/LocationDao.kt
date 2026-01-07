package com.mobile.location.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Insert
    suspend fun insert(location: LocationEntity)

    @Query("SELECT * FROM locations ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLocation(): Flow<LocationEntity?>
}
