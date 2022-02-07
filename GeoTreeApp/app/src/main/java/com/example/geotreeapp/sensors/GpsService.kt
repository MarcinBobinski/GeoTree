package com.example.geotreeapp.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder

import android.os.IBinder
import androidx.core.app.ActivityCompat

class GpsService: Service(), LocationListener {
    // https://developer.android.com/guide/components/bound-services
    inner class GpsServiceBinder: Binder() { fun getService(): GpsService = this@GpsService }
    private val binder = GpsServiceBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    companion object {
        private const val TIME = 100L
        private const val MIN_DISTANCE = 0.0f

        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val PERMISSIONS_REQUEST_CODE = 2
    }

    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    fun getLocation() = lastLocation

    override fun onCreate() {
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onLocationChanged(location: Location) {
        this.lastLocation = location
    }

    @SuppressLint("MissingPermission")
    fun startGPS(): Boolean {
        return if (checkPermissions()) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                TIME,
                MIN_DISTANCE,
                this as LocationListener
            )
            true
        } else {
            false
        }
    }

    fun stopGPS(){
        locationManager.removeUpdates(this)
    }

    fun requirePermissions(activity: Activity){
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
    }

    fun checkPermissions() = REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onDestroy() {
        stopGPS()
        super.onDestroy()
    }

    // https://stackoverflow.com/questions/18125241/how-to-get-data-from-service-to-activity
    // https://xizzhu.me/post/2020-05-18-android-activity-service-communication/
}

//var gpsService: GpsService? = null
//var gpsBound  = false
//val connection = object : ServiceConnection {
//    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//        gpsBound = true
//        gpsService = (service as GpsService.GpsBinder).getService()
//    }
//
//    override fun onServiceDisconnected(name: ComponentName?) {
//        gpsBound = false
//    }
//
//}
//
//requireActivity().run {
//    Intent(this, GpsService::class.java).also { intent ->
//        this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
//    }
//}
