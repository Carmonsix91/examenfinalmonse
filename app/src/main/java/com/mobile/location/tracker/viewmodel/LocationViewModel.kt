package com.mobile.location.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mobile.location.tracker.data.AppDatabase
import com.mobile.location.tracker.data.LocationEntity
import kotlinx.coroutines.flow.Flow

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).locationDao()
    
    val allLocations: Flow<List<LocationEntity>> = dao.getAllLocations()
    val latestLocation: Flow<LocationEntity?> = dao.getLatestLocation()
}
