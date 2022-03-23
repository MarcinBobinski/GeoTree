package com.example.geotreeapp.screens.tree_map

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.TreeMapFragmentBinding
import com.example.geotreeapp.tree.TreeService
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import kotlin.math.ln

class TreeMapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: TreeMapFragmentBinding
    private lateinit var viewModel: TreeMapViewModel

    private var treeService: TreeService? = null

    private val trees: MutableLiveData<List<Tree>> = MutableLiveData(emptyList())
    private var displayedMarkers: List<Marker> = emptyList()

    private var lastCameraPosition: CameraPosition? = null

    private lateinit var mapView: MapView

    companion object {
        private val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private val MAP_POSITION = "MapPostion"
        private val DEFAULT_LOCATION = LatLng(52.237049, 21.017532)
        private val DEFAULT_ZOOM = 11f
        private const val MAX_NUMBER_OF_DISPLAYED_TREES = 100

        private val LAT = "Lat"
        private val LNG = "Lng"
        private val ZOOM = "Zoom"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TreeMapFragmentBinding.inflate(inflater, container, false)

        connectServices()

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TreeMapViewModel::class.java)

        initMap(savedInstanceState)
    }

    private fun initMap(savedInstanceState: Bundle?){
        val mapViewBundle = savedInstanceState?.getBundle(MAPVIEW_BUNDLE_KEY)

        mapView = binding.mapView
        mapView.onCreate(mapViewBundle)

        mapView.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }

        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onMapReady(map: GoogleMap) {

        val cameraPosition = getCameraPosition()

        cameraPosition.let {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it[LAT] as Double, it[LNG] as Double),
                    it[ZOOM] as Float
                )
            )
        }

        val circleBitmapDescriptor =
            bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle_24)

        map.setOnCameraIdleListener {
            val bounds = map.projection.visibleRegion.latLngBounds

            trees.value?.filter { bounds.contains(LatLng(it.y, it.x)) }
                ?.map { tree ->
                    MarkerOptions()
                        .position(LatLng(tree.y, tree.x))
                        .icon(circleBitmapDescriptor)
                        .title(tree.inv_number)
                }?.let {
                    displayedMarkers.filter { !bounds.contains(it.position) }
                        .forEach { it.remove() }
                    val markersInView =
                        displayedMarkers.filter { bounds.contains(it.position) }.toMutableList()
                    val newMarkersToAdd =
                        it.filter { !markersInView.map { it.position }.contains(it.position) }

                    displayedMarkers = if (it.size < MAX_NUMBER_OF_DISPLAYED_TREES) {
                        newMarkersToAdd.forEach { tree ->
                            markersInView.add(
                                map.addMarker(tree)!!
                            )
                        }
                        markersInView
                    } else {
                        map.clear()
                        emptyList()
                    }
                }

            lastCameraPosition = map.cameraPosition
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        } else {
            map.isMyLocationEnabled = true
        }
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        lastCameraPosition?.let { saveCameraPosition(it) }
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun bitmapDescriptorFromVector(context: Context, resId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, resId)!!
        vectorDrawable.setBounds(0,0,vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun connectServices(){
        val treeServiceConnection = object: ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                treeService = (service as TreeService.TreeServiceBinder).getService()

                treeService!!.allTrees.observe(viewLifecycleOwner) {
                    this@TreeMapFragment.trees.value = it
                }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
            }
        }
        requireActivity().run {
            Intent(this, TreeService::class.java).also {
                this.bindService(it, treeServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun getCameraPosition(): Map<String, Any> {
        val sharedPreferences = requireActivity().getSharedPreferences(MAP_POSITION, Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        val lat = sharedPreferences.getFloat(LAT, DEFAULT_LOCATION.latitude.toFloat())
        val lng = sharedPreferences.getFloat(LNG, DEFAULT_LOCATION.longitude.toFloat())
        val zoom = sharedPreferences.getFloat(ZOOM, DEFAULT_ZOOM)

        return mapOf(
            LAT to lat.toDouble(),
            LNG to lng.toDouble(),
            ZOOM to zoom
        )
    }

    fun saveCameraPosition(position: CameraPosition) {
        val sharedPreferences = requireActivity().getSharedPreferences(MAP_POSITION, Context.MODE_PRIVATE)
        sharedPreferences.edit().apply{
            putFloat(LAT, position.target.latitude.toFloat())
            putFloat(LNG, position.target.longitude.toFloat())
            putFloat(ZOOM, position.zoom)
        }.apply()
    }
}