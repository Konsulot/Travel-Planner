package com.travelplanner

import android.app.Application
import com.travelplanner.data.AppContainer
import com.travelplanner.data.DefaultAppContainer

class TravelPlannerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
