package com.example.autocare

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import com.example.autocare.util.PredictiveCheckWorker

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        PredictiveCheckWorker.schedule(this)
    }
}
