package com.example.water_sentinel

import android.app.Application
import com.example.water_sentinel.data.db.AppDatabase

class MyApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        NotificationHelper.createNotificationChannel(applicationContext)
    }
}