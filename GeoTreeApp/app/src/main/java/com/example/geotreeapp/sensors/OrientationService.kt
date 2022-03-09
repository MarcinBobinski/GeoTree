package com.example.geotreeapp.sensors

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import kotlin.math.atan2

class OrientationService() : Service(), SensorEventListener {
    inner class OrientationServiceBinder: Binder() { fun getService(): OrientationService = this@OrientationService }
    private val binder = OrientationServiceBinder()
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    var isRunning = false
        private set

    private lateinit var sensorManager: SensorManager
    private var gravity = FloatArray(3)
    private var magnetic = FloatArray(3)

    private lateinit var handler: Handler

    override fun onCreate() {
        sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        HandlerThread("orientationServiceHandlerThread", Process.THREAD_PRIORITY_DEFAULT).apply {
            start()
            handler = Handler(looper)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

            if(event?.sensor?.type == Sensor.TYPE_ACCELEROMETER){
                synchronized(this) { gravity = event.values }
            } else if(event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                synchronized(this) { magnetic = event.values }
            }

    }

    fun startOrientationUpdates(){
        isRunning = true
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
        sensorManager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
    }

    fun stopOrientationUpdates(){
        isRunning = false
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