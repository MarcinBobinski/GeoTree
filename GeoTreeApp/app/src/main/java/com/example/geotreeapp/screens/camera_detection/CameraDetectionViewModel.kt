package com.example.geotreeapp.screens.camera_detection

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree

class CameraDetectionViewModel(): ViewModel() {

    private val _expectedNumberOfTrees: MutableLiveData<Int> = MutableLiveData(0)
    val expectedNumberOfTrees: LiveData<Int>
        get() = _expectedNumberOfTrees

    fun updateExpectedAmountOfTrees(
        trees: List<Tree>,
        location: Location,
        orientation: Double,
        distance: Double
    ) {

    }

}
