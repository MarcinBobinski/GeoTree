package com.example.geotreeapp.screens.camera_detection

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Size
import android.util.SizeF
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.CameraDetectionFragmentBinding
import com.example.geotreeapp.tree_detection.DetectionPayload
import com.example.geotreeapp.tree_detection.TreeImageAnalyzer
import com.example.geotreeapp.tree_detection.TreeDetectionModel
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.Executors

class CameraDetectionFragment : Fragment() {
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val PERMISSIONS_REQUEST_CODE = 123
    }

    private lateinit var binding: CameraDetectionFragmentBinding
    private lateinit var viewModel: CameraDetectionViewModel

    private lateinit var treeImageAnalyzer: TreeImageAnalyzer

    private var distance = 20.0

    // Camera properties
    private var focalLength: Float? = null
    private var sensorSize: SizeF? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraDetectionViewModel::class.java)

        if (!CameraDetectionViewModel.checkPermissions(requireContext())){
            CameraDetectionViewModel.let {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    it.REQUIRED_PERMISSIONS,
                    it.PERMISSIONS_REQUEST_CODE
                )
            }
        } else {
            viewModel.initializeServices()
        }

        viewModel.expectedNTreesPayload.observe(viewLifecycleOwner) {
            it?:return@observe
            binding.expected.text = "t: ${it.amount} \n o: ${it.orientation} \n x: ${it.location.longitude} \n y: ${it.location.latitude}"
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraDetectionFragmentBinding.inflate(inflater, container, false)

        initializeGuiBindings()

        initializeCameraProperties()

        treeImageAnalyzer = TreeImageAnalyzer(TreeDetectionModel(requireContext()))

        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val detectionObserver = Observer<DetectionPayload> { payload ->
            var drawingTime = System.currentTimeMillis()
            binding.treeDetectionDrawingSurface.let {
                if (!it.isTransformInitialized) {
                    it.transformInit(payload.imageHeight, payload.imageWidth)
                }
                it.treeDetections = payload.detections
                it.invalidate()
            }
            drawingTime = System.currentTimeMillis() - drawingTime
            Timber.i("Drawing time: $drawingTime ms.")

            var expectedNumberOfTreesTime = System.currentTimeMillis()
            viewModel.updateExpectedNumberOfTrees(
                UpdateExpectedNumberOfTreesInput(
                    distance,
                    focalLength,
                    sensorSize,
                    payload.imageWidth,
                    payload.imageHeight,
                    isPortrait
                    )
            )
            expectedNumberOfTreesTime = System.currentTimeMillis() - expectedNumberOfTreesTime
            Timber.i("Expecting number of trees time: $expectedNumberOfTreesTime ms.")
        }

        treeImageAnalyzer.detectionPayload.observe(viewLifecycleOwner, detectionObserver)

        initializeCamera()

        return binding.root
    }

    private fun initializeCamera() {
        if (areAllPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (areAllPermissionsGranted()) {
                requireActivity().finish()
                startActivity(requireActivity().intent)
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Failed to grant permission",
                    Toast.LENGTH_LONG
                ).show()
                requireActivity().finish()
            }
        }

        if(requestCode == CameraDetectionViewModel.PERMISSIONS_REQUEST_CODE){
            if (CameraDetectionViewModel.checkPermissions(requireContext())){
//                viewModel.initializeServices()
                requireActivity().finish()
                startActivity(requireActivity().intent)
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Failed to grant permission",
                    Toast.LENGTH_LONG
                ).show()
                requireActivity().finish()
            }
        }
    }

    private fun areAllPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview
                    .Builder()
                    .build()
                    .also { it.setSurfaceProvider(binding.cameraView.surfaceProvider) }

                val resolution =
                    resources.displayMetrics.let { Size(it.widthPixels, it.heightPixels) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetResolution(resolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply { setAnalyzer(Executors.newSingleThreadExecutor(), treeImageAnalyzer) }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis,
                        preview
                    )
                } catch (e: Exception) {
                    Timber.e("Failed to start camera", e)
                }
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    private fun initializeGuiBindings() {
        binding.btnSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_cameraDetectionFragment_to_settingsFragment)
        )
        binding.btnTreeMap.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_cameraDetectionFragment_to_treeMapFragment)
        )
        binding.treeDetectionDrawingSurface.setZOrderOnTop(true)
        binding.expected

        binding.distanceSlider.let {
            distance = it.value.toDouble()
            it.addOnChangeListener { slider, value, fromUser -> distance = value.toDouble() }
        }
    }

    private fun initializeCameraProperties(){
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraCharacteristics = cameraManager.getCameraCharacteristics("0")

        focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
        sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    }
}