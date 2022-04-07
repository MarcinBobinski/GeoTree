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

        bindGUI()
        bindServices()

        return binding.root
    }


    private fun bindGUI(){
        binding.back.setOnClickListener{ Navigation.findNavController(binding.root).navigateUp() }

        binding.update.setOnClickListener {
            treeService?.updateData()
        }
    }

    private fun bindServices(){
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
    }
}