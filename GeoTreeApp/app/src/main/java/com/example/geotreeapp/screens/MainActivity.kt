package com.example.geotreeapp.screens

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.geotreeapp.databinding.ActivityMainBinding
import com.example.geotreeapp.screens.camera_detection.CameraDetectionFragment
import com.example.geotreeapp.screens.camera_detection.CameraDetectionViewModel
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults.all { it == PackageManager.PERMISSION_GRANTED }){
            finish()
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Failed to grant permission",
                Toast.LENGTH_LONG
            ).show()
            finish()

        }
    }
}