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
import timber.log.Timber
import java.lang.Math.*

class CameraDetectionViewModel(
    application: Application
    ): AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private var gpsService: GpsService? = null
    @SuppressLint("StaticFieldLeak")
    private var orientationService: OrientationService? = null
    @SuppressLint("StaticFieldLeak")
    private var treeService: TreeService? = null

    var trees: List<NormalizedLocation>? = null

    companion object {

        val REQUIRED_PERMISSIONS = arrayOf(
            GpsService.REQUIRED_PERMISSIONS
        ).flatten().distinct().toTypedArray()
        const val PERMISSIONS_REQUEST_CODE = 10
        fun checkPermissions(context: Context) = REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    private var areServicesRunning = false

    private val _expectedTreesPayload: MutableLiveData<ExpectedTreesPayload> = MutableLiveData(null)
    val expectedNTreesPayload: LiveData<ExpectedTreesPayload>
        get() = _expectedTreesPayload


    init {
        application.let {
            bindGpsService(it)
            bindTreeService(it)
            bindOrientationService(it)
        }
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
        if(!areServicesRunning){ if (!initializeServices()) return }
        if (!input.isValid()) return

        val location = gpsService?.getLocation()?: return
        val locationNormalized = location.let {
            NormalizedLocation.fromLocation(it)
            //NormalizedLocation.fromLocation(52.229934, 21.066967)
        }

        val orientation = orientationService?.getOrientation() ?: return
        val fov = calculateFov(
            focalLength = input.focalLength!!,
            sensorSize = input.sensorSize!!,
            aspectRatio = ( input.imageWidth / input.imageHeight.toDouble() ),
            portraitOrientation = input.portraitOrientation
        )
        Timber.i("LEGIA 6")
        val treesInFOV = trees?.filter {
            pow((it.x - locationNormalized.x), 2.0) + pow((it.y - locationNormalized.y), 2.0) < pow(input.distance, 2.0)
        }?.filter {
            isInFov(
                orientation,
                fov,
                calculateAngleFromNorthAxis(locationNormalized, it)
            )
        }?: return
        Timber.i("Expected number of trees: ${treesInFOV.size}")

        _expectedTreesPayload.value = ExpectedTreesPayload(
            treesInFOV.size,
            orientation,
            location
        )
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

    private fun calculateAngleFromNorthAxis(p1: NormalizedLocation, p2: NormalizedLocation): Double {
        // https://math.stackexchange.com/questions/714378/find-the-angle-that-creating-with-y-axis-in-degrees
        val ymax = max(p1.y, p2.y)
        val ymin = min(p1.y, p2.y)

        val angle =  acos(
            ((ymax - ymin)) / (sqrt(pow(p1.x - p2.x, 2.0) + pow(ymax - ymin, 2.0)))
        )

        return when {
            p1.x < p2.x -> { toDegrees(angle) }
            p1.x > p2.x -> { toDegrees(angle) + 180.0 }
            else -> {
                if(p1.y < p2.y){
                    0.0
                } else {
                    180.0
                }
            }
        }
    }

    private fun isInFov(orientation: Double, fov: Double, orientationOfObject: Double): Boolean {
        val angle = normalizeAngle(orientationOfObject - orientation)
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
                    trees = it.map { NormalizedLocation.fromTree(it) }
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

data class NormalizedLocation(
    val x: Double,
    val y: Double,
    val id: Int? = null
) {
    companion object {
        private val EARTH_RADIUS_IN_METERS = 6_371_008.7714
        fun fromTree(tree: Tree): NormalizedLocation{
            //https://stackoverflow.com/questions/1185408/converting-from-longitude-latitude-to-cartesian-coordinates
            return NormalizedLocation(
                EARTH_RADIUS_IN_METERS * cos(toRadians(tree.y)) * cos(toRadians(tree.x)),
                EARTH_RADIUS_IN_METERS * cos(toRadians(tree.y)) * sin(toRadians(tree.x)),
                tree.id
            )
        }
        fun fromLocation(location: Location): NormalizedLocation {
            return NormalizedLocation(
                EARTH_RADIUS_IN_METERS * cos(toRadians(location.latitude)) * cos(toRadians(location.longitude)),
                EARTH_RADIUS_IN_METERS * cos(toRadians(location.latitude)) * sin(toRadians(location.longitude))
            )
        }
        fun fromLocation(x: Double, y: Double): NormalizedLocation {
            return NormalizedLocation(
                EARTH_RADIUS_IN_METERS * cos(toRadians(y)) * cos(toRadians(x)),
                EARTH_RADIUS_IN_METERS * cos(toRadians(y)) * sin(toRadians(x))
            )
        }
    }
}

data class ExpectedTreesPayload(
    val amount: Int,
    val orientation: Double,
    val location: Location
)

