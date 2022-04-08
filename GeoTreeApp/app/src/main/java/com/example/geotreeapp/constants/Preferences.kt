package com.example.geotreeapp.constants

object Preferences {
    const val NAME = "APP_PREFERENCES"

    val DISTANCE_MIN = Pair("DETECTOR_DISTANCE_MIN", 0)
    val DISTANCE_MAX = Pair("DETECTOR_DISTANCE_MAX", 100)
    val DISTANCE = Pair("DETECTOR_DISTANCE", 20)

    val MAX_OBJECTS = Pair("MAP_MAX", 150)

    val LAST_UPDATE = Pair("DATA_LAST_UPDATE", "-")
    val SOURCE = Pair("DATA_SOURCE", "-")

    val GPU_MODE = Pair("MODEL_GPU", true)
    val SHOW_INFERENCE_TIME = Pair("MODEL_INFERENCE_TIME", false)
}