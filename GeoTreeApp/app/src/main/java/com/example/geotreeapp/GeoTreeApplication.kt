package com.example.geotreeapp

import android.app.Application
import timber.log.Timber

class GeoTreeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}