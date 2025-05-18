package com.example.water_sentinel

import android.os.Bundle
import android.widget.Button
//import android.content.Intent
//import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
// androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.btnEnter)
        button.setOnClickListener {
            //val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }
}