package com.example.geotreeapp.screens.tree_map

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.geotreeapp.R

class TreeMapFragment : Fragment() {

    companion object {
        fun newInstance() = TreeMapFragment()
    }

    private lateinit var viewModel: TreeMapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tree_map_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TreeMapViewModel::class.java)
        // TODO: Use the ViewModel
    }

}