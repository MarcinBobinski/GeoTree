package com.example.geotreeapp.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import com.example.geotreeapp.R
import com.example.geotreeapp.databinding.SettingsFragmentBinding
import com.example.geotreeapp.tree.TreeService

class SettingsFragment : Fragment() {

    private lateinit var binding: SettingsFragmentBinding

    private var treeService: TreeService? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingsFragmentBinding.inflate(inflater, container, false)

        binding.back.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_settingsFragment_to_cameraDetectionFragment))

        binding.update.setOnClickListener {
            treeService?.updateData()
        }

        val treeServiceConnection = object: ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                treeService = (service as TreeService.TreeServiceBinder).getService()

                treeService!!.treesNumber.observe(this@SettingsFragment.viewLifecycleOwner) {
                    this@SettingsFragment.binding.treesAmount.text = it.toString()
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



//        var gpsService: GpsService? = null
//        var gpsBound  = false
//        val connection = object :ServiceConnection{
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                gpsBound = true
//                gpsService = (service as GpsService.GpsServiceBinder).getService()
//                gpsService?.run {
//                    if(!checkPermissions()){
//                        requirePermissions(requireActivity())
//                    }
//                    this.startGPS()
//                }
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//                gpsBound = false
//            }
//
//        }
//
//        requireActivity().run {
//            Intent(this, GpsService::class.java).also { intent ->
//                this.bindService(intent, connection, Context.BIND_AUTO_CREATE)
//            }
//        }






//        var orientationService: OrientationService?
//        var orientationBound = false
//        val orientationConnction = object : ServiceConnection{
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                orientationService = (service as OrientationService.OrientationServiceBinder).getService()
//                orientationService!!.startOrientationUpdates()
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
//            }
//        }
//        requireActivity().run {
//            Intent(this, OrientationService::class.java).also { intent ->
//                this.bindService(intent, orientationConnction, Context.BIND_AUTO_CREATE)
//            }
//        }



        return binding.root
    }
}