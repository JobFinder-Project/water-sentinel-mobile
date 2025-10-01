package com.example.water_sentinel.service

import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.MyApp
import com.example.water_sentinel.NotificationHelper
import com.example.water_sentinel.R
import com.example.water_sentinel.data.db.DataHistory
import com.example.water_sentinel.data.remote.FirebaseDataSource
import com.example.water_sentinel.data.repository.DataRepository
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class DataCollectionService: LifecycleService() {
    private lateinit var repository: DataRepository
    private var ultimaNotifAlert: Int = -1

    companion object {
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()

        val firebaseDataSource = FirebaseDataSource(Firebase.database)
        repository = DataRepository(
            firebaseDataSource = firebaseDataSource,
            todoDao = (application as MyApp).database.todoDao()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Transforma o serviço em um Foreground Service.
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("Monitoramento Ativo")
            .setContentText("O sistema está monitorando os dados em tempo real.")
            .setSmallIcon(R.drawable.perigo_chuva) // Troque pelo ícone do seu app
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Inicia a coleta de dados e as outras lógicas em uma corrotina
        lifecycleScope.launch {
            repository.observePostos().collect { postos ->
                postos.forEach { posto ->
                    // Lógica para salvar o histórico no banco de dados local
                    val dataHistory = DataHistory(
                        temperature = posto.temperatura,
                        humidity = posto.umidade,
                        pressure = posto.pressao,
                        volume = posto.volume,
                        percentage = posto.riscoPorcentagem,
                        status = posto.status.toString()
                    )
                    repository.saveHistoricalData(dataHistory)

                    // Lógica para enviar a notificação
                    val nivelAtual = posto.status
                    handleNotification(nivelAtual)

                    // Reseta a variável de controle quando o risco volta para 0
                    if (nivelAtual == 0 && ultimaNotifAlert != 0) {
                        ultimaNotifAlert = 0
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun handleNotification(nivelAtual: Int?) {
        if (nivelAtual == -1 && ultimaNotifAlert != -1) {
            ultimaNotifAlert = -1 // Marca como inativo/desconhecido para não notificar
            return
        }

        if (nivelAtual != ultimaNotifAlert && nivelAtual != null && nivelAtual > 0) {
            val titulo = when (nivelAtual) {
                1 -> getString(R.string.risk_1_low)
                2 -> getString(R.string.risk_2_medium)
                3 -> getString(R.string.risk_3_high)
                else -> null
            }

            val mensagem = when (nivelAtual) {
                1 -> getString(R.string.message_low_risk)
                2 -> getString(R.string.message_medium_risk)
                3 -> getString(R.string.message_high_risk)
                else -> null
            }

            if (titulo != null && mensagem != null) {
                NotificationHelper.sendFloodRiskNotification(
                    this@DataCollectionService,
                    titulo,
                    mensagem
                )
                ultimaNotifAlert = nivelAtual
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}