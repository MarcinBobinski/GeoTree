package com.example.geotreeapp.sensors

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.IBinder
import kotlin.math.atan2

class OrientationService() :Service(), SensorEventListener {
    inner class OrientationServiceBinder: Binder() { fun getService(): OrientationService = this@OrientationService }
    private val binder = OrientationServiceBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private lateinit var sensorManager: SensorManager
    private var gravity = FloatArray(3)
    private var magnetic = FloatArray(3)

    override fun onCreate() {
        sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onSensorChanged(event: SensorEvent?) {
        synchronized(this){
            if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
                gravity = event.values
            } else if(event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetic = event.values
            }
        }
    }

    fun startOrientationUpdates(){
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this,accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this,magneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stopOrientationUpdates(){
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.unregisterListener(this, accelerometerSensor)
        sensorManager.unregisterListener(this, magneticSensor)
    }

    fun getOrientation(): Double {
        val rotationMatrix = FloatArray(9)
        synchronized(this){
            SensorManager.getRotationMatrix(rotationMatrix,null, gravity, magnetic)
        }

        val orientation = atan2(rotationMatrix[2].toDouble(), rotationMatrix[5].toDouble()) + Math.PI

        return Math.toDegrees(orientation)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}