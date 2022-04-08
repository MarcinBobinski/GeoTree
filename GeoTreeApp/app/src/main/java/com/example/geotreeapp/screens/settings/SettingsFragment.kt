package com.example.geotreeapp.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.geotreeapp.constants.Preferences
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
        val sharedPreferences =  requireActivity().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE)

        binding.lastUpdate.text = Preferences.LAST_UPDATE.run {  sharedPreferences.getString(first, second) }
        binding.dataSource.text = Preferences.SOURCE.run {  sharedPreferences.getString(first, second) }


        binding.update.setOnClickListener {
            treeService?.updateData()
        }

        val savedDistanceFrom = Preferences.DISTANCE_MIN.run{sharedPreferences.getInt(first, second)}
        val savedDistanceTo = Preferences.DISTANCE_MAX.run{sharedPreferences.getInt(first, second)}
        binding.distanceFrom.let{
            it.minValue = 0
            it.maxValue = savedDistanceTo
            it.value = savedDistanceFrom
            it.setOnValueChangedListener { _, oldValue, newValue ->
                sharedPreferences.edit().apply {
                    putInt(Preferences.DISTANCE_MIN.first, newValue)
                }.apply()
                binding.distanceTo.minValue = newValue
            }
        }

        binding.distanceTo.let{
            it.minValue = savedDistanceFrom
            it.maxValue = 999
            it.value = savedDistanceTo
            it.setOnValueChangedListener { _, oldValue, newValue ->
                sharedPreferences.edit().apply {
                    putInt(Preferences.DISTANCE_MAX.first, newValue)
                }.apply()
                binding.distanceFrom.maxValue = newValue
            }
        }

        // model
        binding.GPUSwitch.apply {
            isChecked = Preferences.GPU_MODE.run { sharedPreferences.getBoolean(first, second) }
            setOnCheckedChangeListener { compoundButton, checked ->
                sharedPreferences.edit().apply {
                    putBoolean(Preferences.GPU_MODE.first, checked)
                }.apply()
            }
        }

        binding.inferenceTimeSwitch.apply {
            isChecked = Preferences.SHOW_INFERENCE_TIME.run { sharedPreferences.getBoolean(first, second) }
            setOnCheckedChangeListener { compoundButton, checked ->
                sharedPreferences.edit().apply {
                    putBoolean(Preferences.SHOW_INFERENCE_TIME.first, checked)
                }.apply()
            }
        }

        // map
        val mapMax = Preferences.MAX_OBJECTS.run { sharedPreferences.getInt(first, second) }
        binding.mapMax.apply {
            setText(mapMax.toString())
            addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(editable: Editable) {
                    val number = try{
                        editable.toString().toInt()
                    } catch (e: Exception){
                        Preferences.MAX_OBJECTS.second.let {
                            binding.mapMax.setText(it.toString())
                            it
                        }
                    }
                    sharedPreferences.edit().apply {
                        putInt(Preferences.MAX_OBJECTS.first, number)
                    }.apply()
                }

            })
        }


    }

    private fun bindServices(){
        val treeServiceConnection = object: ServiceConnection{
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                treeService = (service as TreeService.TreeServiceBinder).getService()

                treeService!!.treesNumber.observe(this@SettingsFragment.viewLifecycleOwner) {
                    this@SettingsFragment.binding.treesAmount.text = it.toString()
                }

                treeService!!.updateStatus.observe(this@SettingsFragment.viewLifecycleOwner){
                    if(it == null){
                        binding.progressBar.run {
                            progress = 0
                            visibility = View.GONE
                        }
                    } else if(it == 100) {
                        binding.progressBar.run {
                            //Update Gui
                            visibility = View.GONE
                            progress = 0
                            val sharedPreferences =  requireActivity().getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE)
                            binding.lastUpdate.text = Preferences.LAST_UPDATE.run {  sharedPreferences.getString(first, second) }
                            binding.dataSource.text = Preferences.SOURCE.run {  sharedPreferences.getString(first, second) }
                        }
                    } else {
                        binding.progressBar.run {
                            visibility = View.VISIBLE
                            progress = it
                        }
                    }
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