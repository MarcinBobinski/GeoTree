package com.example.geotreeapp.screens.camera_detection

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geotreeapp.tree.tree_db.infrastructure.Tree

class CameraDetectionViewModel(application: Application): AndroidViewModel(application) {

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

    init {

    }

}
