package com.example.geotreeapp.screens.camera_detection

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.util.SizeF
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.sensors.GpsService
import com.example.geotreeapp.sensors.OrientationService
import com.example.geotreeapp.tree.TreeService
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Math.*
import org.gavaghan.geodesy.Ellipsoid
import org.gavaghan.geodesy.GeodeticCalculator
import org.gavaghan.geodesy.GlobalCoordinates


class CameraDetectionViewModel(
    application: Application
    ): AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private var gpsService: GpsService? = null
    @SuppressLint("StaticFieldLeak")
    private var orientationService: OrientationService? = null
    @SuppressLint("StaticFieldLeak")
    private var treeService: TreeService? = null

    var autoVerifier = false
        private set




    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            GpsService.REQUIRED_PERMISSIONS
        ).flatten().distinct().toTypedArray()
        const val PERMISSIONS_REQUEST_CODE = 10
        fun checkPermissions(context: Context) = REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        val REFERENCE: Ellipsoid = Ellipsoid.WGS84
    }

    var trees: List<Tree>? = null

    private var areServicesRunning = false

    private val _expectedTreesPayload: MutableLiveData<ExpectedTreesPayload> = MutableLiveData(null)
    val expectedNTreesPayload: LiveData<ExpectedTreesPayload>
        get() = _expectedTreesPayload

    private val geoCalculator = GeodeticCalculator()

    init {
        application.let {
            bindGpsService(it)
            bindTreeService(it)
            bindOrientationService(it)
        }
    }

    fun autoVerifierSwitch(): Boolean {
        autoVerifier = !autoVerifier
        return autoVerifier
    }

    fun updateTrees(trees: List<Tree>) {
        treeService?.updateTrees(trees)
    }

    fun initializeServices(): Boolean {
        if(!servicesLoaded()) return false

        if(!gpsService!!.startGPS()) return false
        orientationService!!.startOrientationUpdates()
        areServicesRunning = true
        return true
    }

    fun deactivateServices(){
        areServicesRunning = false
        gpsService?.stopGPS()
        orientationService?.stopOrientationUpdates()
    }

    fun updateExpectedNumberOfTrees(input: UpdateExpectedNumberOfTreesInput){
        CoroutineScope(Dispatchers.Default).launch {
            update(input)
        }
    }

    private suspend fun update(input: UpdateExpectedNumberOfTreesInput){
        if(!areServicesRunning){ if (!initializeServices()) return }
        if (!input.isValid()) return

        val location = gpsService?.getLocation()?: return
        val locationGC = GlobalCoordinates(location.latitude, location.longitude)

        val userAzimuth = orientationService?.getOrientation() ?: return
        val fov = calculateFov(
            focalLength = input.focalLength!!,
            sensorSize = input.sensorSize!!,
            aspectRatio = ( input.imageWidth / input.imageHeight.toDouble() ),
            portraitOrientation = input.portraitOrientation
        )

        listOf(1,2,3)

        val expectedTrees = trees
            ?.associate { it to geoCalculator.calculateGeodeticCurve(REFERENCE, locationGC, GlobalCoordinates(it.y, it.x)) }
            ?.filter { it.value.ellipsoidalDistance < input.distance }
            ?.filter { isInFov(userAzimuth, it.value.azimuth, fov) }
            ?.keys
            ?.toList() ?: return

        Timber.i("Expected number of trees: ${expectedTrees.size}")

        withContext(Dispatchers.Main){
            _expectedTreesPayload.value = ExpectedTreesPayload(
                input.detectedTrees,
                expectedTrees,
                userAzimuth,
                location
            )
        }
    }

    private fun calculateFov(
        focalLength: Float,
        sensorSize: SizeF,
        aspectRatio: Double,
        portraitOrientation: Boolean
    ): Double {
        // https://stackoverflow.com/questions/67375781/without-additional-calculation-camera-or-camera2-apis-return-fov-angle-values-fo
        return if (portraitOrientation) {
            toDegrees(2.0 * atan((sensorSize.height / (focalLength * 2f)).toDouble())) // vertical angle
        } else {
            toDegrees(2.0 * atan(aspectRatio * (sensorSize.height / (focalLength * 2f)).toDouble())) // horizontal angle
        }
    }


    private fun isInFov(userAzimuth: Double, objectAzimuth: Double, fov: Double): Boolean {
        val angle = normalizeAngle(objectAzimuth - userAzimuth)
        return angle < fov/2.0 || angle > 360.0 - (fov/2.0)
    }

    private fun normalizeAngle(angle: Double): Double {
        var result = angle
        while (result < 0) {
            result += 360.0
        }
        while (result > 360) {
            result -= 360.0
        }
        return result
    }

    private fun servicesLoaded() = !listOf(gpsService, treeService, orientationService).any {it == null}

    private fun bindGpsService(application: Application) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                gpsService = (service as GpsService.GpsServiceBinder).getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                gpsService = null
            }
        }

        application.run {
            Intent(this,GpsService::class.java).also { intent ->
                this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun bindOrientationService(application: Application) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                orientationService = (service as OrientationService.OrientationServiceBinder).getService()
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                orientationService = null
            }
        }

        application.run {
            Intent(this,OrientationService::class.java).also { intent ->
                this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun bindTreeService(application: Application) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                treeService = (service as TreeService.TreeServiceBinder).getService()
                treeService!!.allTrees.observeForever {
                    trees = it
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                treeService = null
            }
        }

        application.run {
            Intent(this,TreeService::class.java).also { intent ->
                this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

data class UpdateExpectedNumberOfTreesInput(
    val detectedTrees: Int,
    val distance: Double,
    val focalLength: Float?,
    val sensorSize: SizeF?,
    val imageWidth: Int,
    val imageHeight: Int,
    val portraitOrientation: Boolean
) {
    fun isValid(): Boolean {
        if (focalLength == null) return false
        if (sensorSize == null) return false
        return true
    }
}

data class ExpectedTreesPayload(
    val detectedTrees: Int,
    val expectedTrees: List<Tree>,
    val orientation: Double,
    val location: Location
)

