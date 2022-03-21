package com.example.geotreeapp.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*

import androidx.core.app.ActivityCompat

class GpsService: Service(), LocationListener {
    // https://developer.android.com/guide/components/bound-services
    inner class GpsServiceBinder: Binder() { fun getService(): GpsService = this@GpsService }
    private val binder = GpsServiceBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    companion object {
        private const val TIME = 0L
        private const val MIN_DISTANCE = 0.0f

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val PERMISSIONS_REQUEST_CODE = 2
    }

    var isRunning: Boolean = false
        private set

    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    fun getLocation() = lastLocation

    private lateinit var looper: Looper

    override fun onCreate() {
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        HandlerThread("gpsServiceHandlerThread", Process.THREAD_PRIORITY_DEFAULT).apply {
            start()
            this@GpsService.looper = looper
        }
    }

    override fun onLocationChanged(location: Location) {
        this.lastLocation = location
    }


    override fun onProviderDisabled(provider: String) {
        lastLocation = null
        isRunning = false
    }

    override fun onProviderEnabled(provider: String) {
        lastLocation = null
        isRunning = true
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    @SuppressLint("MissingPermission")
    fun startGPS(): Boolean {
        return if (checkPermissions()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                TIME,
                MIN_DISTANCE,
                this as LocationListener,
                looper
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                TIME,
                MIN_DISTANCE,
                this as LocationListener,
                looper
            )
            isRunning = true
            true
        } else {
            isRunning = false
            false
        }
    }

    fun stopGPS(){
        locationManager.removeUpdates(this)
    }

    fun checkPermissions() = REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onDestroy() {
        stopGPS()
        super.onDestroy()
    }

    fun resetLocation() {
        this.lastLocation = null
    }

}
