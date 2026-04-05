package com.planespotter

import android.app.Application
import org.osmdroid.config.Configuration

class PlaneSpotterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
    }
}
