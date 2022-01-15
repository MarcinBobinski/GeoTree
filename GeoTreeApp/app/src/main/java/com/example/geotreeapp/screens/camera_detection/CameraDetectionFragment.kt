package com.example.geotreeapp.screens.camera_detection

import android.Manifest
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Size
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
import androidx.navigation.Navigation
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.CameraDetectionFragmentBinding
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = CameraDetectionFragmentBinding.inflate(inflater, container, false)

        binding.btnSettings.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_cameraDetectionFragment_to_settingsFragment)
        )
        binding.btnTreeMap.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_cameraDetectionFragment_to_treeMapFragment)
        )
        binding.treeDetectionDrawingSurface.setZOrderOnTop(true)


        val treeDetectionModel = TreeDetectionModel(requireContext())
        treeImageAnalyzer =
            TreeImageAnalyzer(treeDetectionModel, binding.treeDetectionDrawingSurface)

        if (areAllPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                REQUIRED_PERMISSIONS,
                PERMISSIONS_REQUEST_CODE
            )
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CameraDetectionViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (areAllPermissionsGranted()) {
                startCamera()
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

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview
                .Builder()
                .build()
                .also { it.setSurfaceProvider(binding.cameraView.surfaceProvider) }

            val resolution = resources.displayMetrics.let { Size(it.widthPixels, it.heightPixels) }

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
        }, ContextCompat.getMainExecutor(requireContext()))
    }

}