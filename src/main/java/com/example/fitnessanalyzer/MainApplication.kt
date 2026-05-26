package com.example.fitnessanalyzer

import android.app.Application
import com.example.fitnessanalyzer.data.local.FitnessDatabase

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FitnessDatabase.getDatabase(this)
    }
}
