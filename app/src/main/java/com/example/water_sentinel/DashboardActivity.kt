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
        // Identifica os elementos da interface
        val txtTemp = findViewById<TextView>(R.id.tv_temperature)
        getTemp(txtTemp)

        val txtUmi = findViewById<TextView>(R.id.tv_humidity)
        getUmi(txtUmi)
    }

    // Função que acessa a temperatura no Firebase
    fun getTemp(txtTemp: TextView) {

        // Declara o caminho do dado
        val database = Firebase.database
        val myRefTemp = database.getReference("sensor/dht/temperatura")

        myRefTemp.addValueEventListener(object : ValueEventListener {
            // Busca o dado toda vez que for alterado
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val temperatura = dataSnapshot.getValue<Int>()
                txtTemp.text = "$temperatura°C"

            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
            }
        })}

    // Função que acessa a umidade no Firebase
    fun getUmi(txtUmi: TextView) {

        // Declara o caminho do dado
        val database = Firebase.database
        val myRefUmi = database.getReference("sensor/dht/umidade")

        myRefUmi.addValueEventListener(object : ValueEventListener {
            // Busca o dado toda vez que for alterado
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val umidade = dataSnapshot.getValue<Float>()
                txtUmi.text = "$umidade%"
            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
            }
        })
    }

        //val button = findViewById<Button>(R.id.btnMap)
        //button.setOnClickListener {
            //val intent = Intent(this, MapsActivity::class.java)
            //startActivity(intent)
        //}
}