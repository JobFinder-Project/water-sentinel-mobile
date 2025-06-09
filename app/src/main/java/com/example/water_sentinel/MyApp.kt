package com.example.water_sentinel

import android.app.Application

class MyApp : Application() {
    var globalStatusRisco: Int = 4

    override fun onCreate() {
        super.onCreate()
    }
}