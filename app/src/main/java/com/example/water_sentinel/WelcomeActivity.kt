package com.example.water_sentinel

import android.os.Bundle
import android.widget.Button
import android.content.Intent
import android.os.Handler
import android.os.Looper
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
// androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }

}