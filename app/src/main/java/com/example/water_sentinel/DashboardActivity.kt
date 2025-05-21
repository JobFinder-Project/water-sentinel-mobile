package com.example.water_sentinel

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.Firebase

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        val txtTemp = findViewById<TextView>(R.id.tv_temperature)
        getTemp(txtTemp)
    }

    // Função que acessa a temperatura no Firebase
    fun getTemp(txtTemp: TextView) {

        // Configuração do Banco de Dados
        val database = Firebase.database
        val myRef = database.getReference("sensor/dht/temperatura1")

        myRef.addValueEventListener(object : ValueEventListener {
            // Busca o dado toda vez que for alterado
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val temperatura = dataSnapshot.getValue<Float>()
                txtTemp.text = "$temperatura°C"

            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
            }
        })
    }
}