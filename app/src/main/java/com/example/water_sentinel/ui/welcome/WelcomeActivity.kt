package com.example.water_sentinel.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.R
import com.example.water_sentinel.ui.dashboard.DashboardActivity
import com.example.water_sentinel.ui.splash.SplashActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        lifecycleScope.launch {
            delay(900L)
            val intent = Intent(this@WelcomeActivity, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

}