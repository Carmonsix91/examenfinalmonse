package com.mobile.location.tracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mobile.location.tracker.MainActivity
import com.mobile.location.tracker.data.AppDatabase
import com.mobile.location.tracker.data.LocationEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackerService : LifecycleService() {
	
	private lateinit var fusedLocationClient: FusedLocationProviderClient
	private lateinit var locationCallback: LocationCallback
	private var intervalMillis: Long = 10000L
	private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
	
	companion object {
		const val ACTION_START = "ACTION_START"
		const val ACTION_STOP = "ACTION_STOP"
		const val EXTRA_INTERVAL = "EXTRA_INTERVAL"
		const val CHANNEL_ID = "location_tracker_channel"
		const val NOTIFICATION_ID = 1
	}
	
	override fun onCreate() {
		super.onCreate()
		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
		createNotificationChannel()
		
		locationCallback = object : LocationCallback() {
			override fun onLocationResult(locationResult: LocationResult) {
				locationResult.lastLocation?.let { location ->
					saveLocation(location)
					updateNotification(location)
				}
			}
		}
	}
	
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		super.onStartCommand(intent, flags, startId)
		when (intent?.action) {
			ACTION_START -> {
				intervalMillis = intent.getLongExtra(EXTRA_INTERVAL, 10000L)
				startForeground(NOTIFICATION_ID, createNotification("Tracking started..."))
				startLocationUpdates()
			}
			
			ACTION_STOP -> {
				stopLocationUpdates()
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
		}
		return START_STICKY
	}
	
	@SuppressLint("MissingPermission")
	private fun startLocationUpdates() {
		val locationRequest =
			LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
				.setMinUpdateIntervalMillis(intervalMillis / 2)
				.build()
		
		fusedLocationClient.requestLocationUpdates(
			locationRequest,
			locationCallback,
			Looper.getMainLooper()
		)
	}
	
	private fun stopLocationUpdates() {
		fusedLocationClient.removeLocationUpdates(locationCallback)
	}
	
	private fun saveLocation(location: Location) {
		val entity = LocationEntity(
			latitude = location.latitude,
			longitude = location.longitude,
			precision = location.accuracy,
			timestamp = System.currentTimeMillis()
		)
		lifecycleScope.launch {
			AppDatabase.getDatabase(applicationContext).locationDao().insert(entity)
		}
	}
	
	private fun updateNotification(location: Location) {
		val contentText = "Lat: ${location.latitude}, Lng: ${location.longitude}\n" +
				"Acc: ${location.accuracy}m | Time: ${dateFormat.format(Date())}"
		
		val notificationManager =
			getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
		notificationManager.notify(NOTIFICATION_ID, createNotification(contentText))
	}
	
	private fun createNotification(contentText: String): Notification {
		val intent = Intent(this, MainActivity::class.java)
		val pendingIntent = PendingIntent.getActivity(
			this, 0, intent, PendingIntent.FLAG_IMMUTABLE
		)
		
		return NotificationCompat.Builder(this, CHANNEL_ID)
			.setContentTitle("Location Tracker Active")
			.setContentText(contentText)
			.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
			.setSmallIcon(android.R.drawable.ic_menu_mylocation)
			.setContentIntent(pendingIntent)
			.setOngoing(true)
			.build()
	}
	
	private fun createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				CHANNEL_ID,
				"Location Tracker Service",
				NotificationManager.IMPORTANCE_LOW
			)
			val manager = getSystemService(NotificationManager::class.java)
			manager.createNotificationChannel(channel)
		}
	}
}
