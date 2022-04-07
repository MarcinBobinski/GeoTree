package com.example.geotreeapp.screens.tree_map

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
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
import androidx.navigation.Navigation
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.TreeMapFragmentBinding
import com.example.geotreeapp.tree.TreeService
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import android.widget.TextView

import android.view.Gravity

import android.widget.LinearLayout
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeStatus
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter

import com.google.android.gms.maps.model.Marker


class TreeMapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var binding: TreeMapFragmentBinding
    private lateinit var viewModel: TreeMapViewModel

    private var treeService: TreeService? = null

    private val trees: MutableLiveData<List<Tree>> = MutableLiveData(emptyList())
    private var displayedMarkers: Map<Marker, Tree> =  emptyMap()

    private var selectedMarker: Marker? = null

    private var lastCameraPosition: CameraPosition? = null

    private lateinit var mapView: MapView

    companion object {
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val MAP_POSITION = "MapPosition"
        private val DEFAULT_LOCATION = LatLng(52.237049, 21.017532)
        private const val DEFAULT_ZOOM = 11f
        private const val MAX_NUMBER_OF_DISPLAYED_TREES = 150

        private const val LAT = "Lat"
        private const val LNG = "Lng"
        private const val ZOOM = "Zoom"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TreeMapFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(TreeMapViewModel::class.java)

        initMap(savedInstanceState)
        initGuiBindings()
        connectServices()

        return binding.root
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
        getCameraPosition().let {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it[LAT] as Double, it[LNG] as Double),
                    it[ZOOM] as Float
                )
            )
        }

        val bitmapDescriptors = mapOf(
            TreeStatus.VERIFIED to bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.GREEN),
            TreeStatus.NOT_VERIFIED to bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.GRAY),
            TreeStatus.MISSING to bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.RED)
        )

        trees.observe(viewLifecycleOwner){
            val markersToDelete:MutableList<Marker> = mutableListOf()
            displayedMarkers.forEach { markerMap ->
                val updatedTree = it.find { it.id == markerMap.value.id }
                if(updatedTree == null){
                    markersToDelete.add(markerMap.key)
                } else if (markerMap.value.treeStatus != updatedTree.treeStatus){
                    markerMap.key.setIcon(bitmapDescriptors[updatedTree.treeStatus])
                }
            }
            markersToDelete.forEach { it.remove() }
            displayedMarkers = displayedMarkers - markersToDelete.toSet()
        }

        map.setOnInfoWindowClickListener {
            selectedMarker = it
            showTreeStatusActionPanel(true)
        }
        map.setOnInfoWindowCloseListener {
            selectedMarker = null
            showTreeStatusActionPanel(false)
        }

        map.setInfoWindowAdapter(markerWindowAdapter)



        map.setOnCameraIdleListener {
            val bounds = map.projection.visibleRegion.latLngBounds

            trees.value?.filter { bounds.contains(LatLng(it.y, it.x)) }
                ?.let {
                    // Delete markers out of view
                    displayedMarkers.filter { !bounds.contains(it.key.position) }
                        .forEach { it.key.remove() }

                    val markersInView =
                        displayedMarkers.filter { bounds.contains(it.key.position) }

                    val newMarkersToAdd =
                        it.filter {
                            !markersInView.map { it.key.position }.contains(LatLng(it.y, it.x))
                        }.associate {
                            prepareMarkerOptions(it, bitmapDescriptors[it.treeStatus]!!) to it
                        }

                    displayedMarkers = if (it.size < MAX_NUMBER_OF_DISPLAYED_TREES) {
                        val newAddedMarkers = newMarkersToAdd.keys.associate {
                            map.addMarker(it)!! to newMarkersToAdd[it]!!
                        }

                        markersInView + newAddedMarkers
                    } else {
                        map.clear()
                        emptyMap()
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

    private fun prepareMarkerOptions(tree: Tree, bitmapDescriptor: BitmapDescriptor) = MarkerOptions()
        .position(LatLng(tree.y, tree.x))
        .icon(bitmapDescriptor)
        .title(tree.inv_number)
        .snippet("Gatunek: ${tree.type}\nLat: ${tree.y}\nLng: ${tree.x}")

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

    private fun bitmapDescriptorFromVector(context: Context, resId: Int, color: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, resId)!!
        vectorDrawable.setBounds(0,0,vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        vectorDrawable.setTint(color)
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
            override fun onServiceDisconnected(name: ComponentName?) { }
        }
        requireActivity().run {
            Intent(this, TreeService::class.java).also {
                this.bindService(it, treeServiceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    fun getCameraPosition(): Map<String, Any> {
        val sharedPreferences = requireActivity().getSharedPreferences(MAP_POSITION, Context.MODE_PRIVATE)
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

    private fun initGuiBindings(){
        binding.returnButton.setOnClickListener{ Navigation.findNavController(binding.root).navigateUp() }

        showTreeStatusActionPanel(false)

        binding.btnMissing.setOnClickListener {
            treeService?.let { service ->
                selectedMarker?.let { marker ->
                    displayedMarkers[marker]?.let { tree ->
                        service.updateTree(tree.copy(treeStatus = TreeStatus.MISSING))
                        marker.setIcon(bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.RED))
                    }
                }
            }
        }
        binding.btnVerified.setOnClickListener {
            treeService?.let { service ->
                selectedMarker?.let { marker ->
                    displayedMarkers[marker]?.let { tree ->
                        service.updateTree(tree.copy(treeStatus = TreeStatus.VERIFIED))
                        marker.setIcon(bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.GREEN))
                    }
                }
            }
        }
        binding.btnNotVerified.setOnClickListener{
            treeService?.let { service ->
                selectedMarker?.let { marker ->
                    displayedMarkers[marker]?.let { tree ->
                        service.updateTree(tree.copy(treeStatus = TreeStatus.NOT_VERIFIED))
                        marker.setIcon(bitmapDescriptorFromVector(requireContext(), R.drawable.green_circle, Color.GRAY))
                    }
                }
            }
        }
    }

    private fun showTreeStatusActionPanel(show: Boolean) {
        listOf(binding.btnMissing, binding.btnVerified, binding.btnNotVerified).forEach {
            it.visibility = if (show) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private val markerWindowAdapter = object : InfoWindowAdapter {
        override fun getInfoWindow(arg0: Marker) = null

        override fun getInfoContents(marker: Marker): View {
            val context = requireContext()

            val title = TextView(context).apply {
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                text = marker.title
            }

            val snippet = TextView(context).apply {
                    setTextColor(Color.GRAY)
                    text = marker.snippet
                }

            return LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(title)
                addView(snippet)
            }
        }
    }
}