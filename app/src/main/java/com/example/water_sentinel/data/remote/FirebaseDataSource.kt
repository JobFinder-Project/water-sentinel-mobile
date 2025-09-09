package com.example.water_sentinel.data.remote

import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.MyApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FirebaseDataSource(private val database: FirebaseDatabase) {

    // Função que recupera os dados do Firebase
    private fun setupFirebaseListener() {
        setupTimestampListener()
        setupDataListener()
    }

    // Função que acessa os dados do Firebase
    private fun setupDataListener() {

        // Declara o caminho dos dados do sensor DHT
        val refDht = database.getReference("sensor/data/")

        refDht.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val temperatura = snapshot.child("temperatura").getValue(Float::class.java)
                val umidade = snapshot.child("umidade").getValue(Int::class.java)
                val pressaoRaw = snapshot.child("pressao").getValue(Int::class.java)
                val volume = snapshot.child("volume").getValue(Float::class.java)
                val percentual = snapshot.child("percentual").getValue(Int::class.java)

                val pressao: Int? = if (pressaoRaw == 0 || pressaoRaw == null) null else pressaoRaw

                txtTemp.text = temperatura?.let { "%.1f°C".format(it).replace('.', ',') } ?: "---"
                txtUmi.text = umidade?.let { "$it%" } ?: "---"
                txtPressao.text = pressao?.let { "$it hPa" } ?: "---"
                txtvolume.text = volume?.let { String.format("%.1f ml", it).replace('.', ',') } ?: "---"
                txtPercentual.text = percentual?.let { "$it%" } ?: "---"

                val alertLevelAtual = snapshot.child("alertLevel").getValue(Int::class.java)

                val status = findViewById<TextView>(R.id.tv_weather_desc).text.toString()
                if (isSystemActive) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val currentReading = DataHistory(
                            temperature = temperatura,
                            humidity = umidade,
                            pressure = pressao,
                            volume = volume,
                            percentage = percentual,
                            status = status
                        )
                        todoDao.insert(currentReading)

                    }
                }

                val app = (application as MyApp)
                app.postoAlerta.apply {
                    this.temperatura = temperatura ?: 0f
                    this.umidade = umidade ?: 0
                    this.pressao = pressao ?: 0
                    this.riscoPorcentagem = percentual ?: 0
                    this.status = alertLevelAtual ?: -1
                }

                // atualiza o status do sistema
                checkStatus()
                processarMudancaAlertLevel(alertLevelAtual)

                if (::map.isInitialized) {
                    map.clear()
                    addRiskMarker(app.postoAlerta)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
                txtTemp.text = getString(R.string.sem_temperatura)
                txtUmi.text = getString(R.string.sem_dados)
                txtPressao.text = getString(R.string.sem_dados)
                txtvolume.text = getString(R.string.sem_dados)
                processarMudancaAlertLevel(null)
            }
        })
    }

    // Função para alterar satus do sistema
    private fun setupTimestampListener() {
        val refTimestamp = database.getReference("timestamp/")

        refTimestamp.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val horaStr = snapshot.child("hora").getValue<String>()
                val dataStr = snapshot.child("data").getValue<String>()

                if (dataStr != null && horaStr != null) {
                    try {
                        val data = LocalDate.parse(dataStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val hora = LocalTime.parse(horaStr, DateTimeFormatter.ofPattern("HH:mm:ss"))
                        val dataHora = LocalDateTime.of(data, hora)

                        ultimoTimestampRecebido = dataHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        checkStatus()
                    } catch (e: Exception) {
                        Log.e("DateTime", "Erro ao parsear data/hora do Firebase", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler timestamp", error.toException())
            }
        })
    }
}