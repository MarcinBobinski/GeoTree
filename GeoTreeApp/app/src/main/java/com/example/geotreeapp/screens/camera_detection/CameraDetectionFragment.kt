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
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.CameraDetectionFragmentBinding
import com.example.geotreeapp.tree.tree_db.infrastructure.TreeStatus
import com.example.geotreeapp.tree_detection.TreeImageAnalyzer
import com.example.geotreeapp.tree_detection.TreeDetectionModel
import timber.log.Timber
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.math.roundToInt

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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(CameraDetectionViewModel::class.java)
        binding = CameraDetectionFragmentBinding.inflate(inflater, container, false)

        initializeGuiBindings()

        bindAllObservers()

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


        initializeCamera()

        return binding.root
    }

    private fun bindAllObservers(){
        viewModel.expectedNTreesPayload.observe(viewLifecycleOwner) {
            it?:return@observe
            binding.compassText.text = "${it.orientation.roundToInt()}"
            binding.expectedText.text = "${it.expectedTrees.size}"
            binding.detectedText.text = "${it.detectedTrees}"

            if (viewModel.autoVerifier) {
                if (it.expectedTrees.size <= it.detectedTrees && it.expectedTrees.isNotEmpty()) {
                    it.expectedTrees.filter { it.treeStatus != TreeStatus.VERIFIED }
                        .map { it.copy(treeStatus = TreeStatus.VERIFIED) }
                        .let { viewModel.updateTrees(it) }
                } else if (it.expectedTrees.size == 1) {
                    it.expectedTrees.filter { it.treeStatus == TreeStatus.NOT_VERIFIED }
                        .map { it.copy(treeStatus = TreeStatus.MISSING) }
                        .let {
                            if (it.isNotEmpty()) {
                                viewModel.updateTrees(it)
                            }
                        }
                }
            }
        }

        treeImageAnalyzer = TreeImageAnalyzer(TreeDetectionModel(requireContext()))
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        treeImageAnalyzer.detectionPayload.observe(viewLifecycleOwner) { payload ->
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
                    payload.detections.size,
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

    }

    private fun initializeCamera() {
        if (areAllPermissionsGranted()) {
            initializeCameraProperties()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
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

        binding.distanceSlider.let {
            distance = it.value.toDouble()
            it.addOnChangeListener { slider, value, fromUser -> distance = value.toDouble() }
        }

        if(viewModel.autoVerifier) {
            binding.btnAutoVerification.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
        } else {
            binding.btnAutoVerification.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        }

        binding.btnAutoVerification.setOnClickListener {
            if( viewModel.autoVerifierSwitch()) {
                binding.btnAutoVerification.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            } else {
                binding.btnAutoVerification.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
            }
        }
    }

    private fun initializeCameraProperties(){
        val cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraCharacteristics = cameraManager.getCameraCharacteristics("0")

        focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
        sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
    }
}