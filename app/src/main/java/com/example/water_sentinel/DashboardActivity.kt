package com.example.water_sentinel

import android.Manifest
import android.app.AlertDialog
import android.widget.Button
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.database.getValue
import com.google.firebase.Firebase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class DashboardActivity : AppCompatActivity() {
    companion object {
        private const val CODIGO_PERMISSAO_NOTIFICACAO = 1001
        private const val TAG = "DashboardActivity" // Tag para logs
    }
    private val database = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        Log.d(TAG, "onCreate: Activity Criada")

        //criarCanalNotificacao()

        solicitarPermissaoNotificacao()

        setupFirebaseListener()
        // Encontrar os botões pelos IDs corretos do XML
        val btnSimularBaixo: Button? = findViewById(R.id.btnSimularRiscoBaixo)
        val btnSimularMedio: Button? = findViewById(R.id.btnSimularRiscoMedio)
        val btnSimularAlto: Button? = findViewById(R.id.btnSimularRiscoAlto)

        // Adicionar logs para verificar se os botões são encontrados
        if (btnSimularBaixo == null) Log.e(TAG, "Botão btnSimularRiscoBaixo NÃO encontrado!") else Log.d(TAG, "Botão btnSimularRiscoBaixo encontrado.")
        if (btnSimularMedio == null) Log.e(TAG, "Botão btnSimularRiscoMedio NÃO encontrado!") else Log.d(TAG, "Botão btnSimularRiscoMedio encontrado.")
        if (btnSimularAlto == null) Log.e(TAG, "Botão btnSimularRiscoAlto NÃO encontrado!") else Log.d(TAG, "Botão btnSimularRiscoAlto encontrado.")

        btnSimularBaixo?.setOnClickListener {
            Log.d(TAG, "Botão Simular Risco Baixo CLICADO")
            if (checkNotificationPermission()) {
                NotificationHelper.simulateLowRisk(this)
            } else {
                Log.w(TAG, "Permissão de notificação não concedida. Não é possível enviar Risco Baixo.")
                Toast.makeText(this, "Permissão de notificação necessária.", Toast.LENGTH_SHORT).show()
            }
        }
        btnSimularMedio?.setOnClickListener {
            Log.d(TAG, "Botão Simular Risco Médio CLICADO")
            if (checkNotificationPermission()) {
                NotificationHelper.simulateMediumRisk(this)
            } else {
                Log.w(TAG, "Permissão de notificação não concedida. Não é possível enviar Risco Médio.")
                Toast.makeText(this, "Permissão de notificação necessária.", Toast.LENGTH_SHORT).show()
            }
        }
        btnSimularAlto?.setOnClickListener {
            Log.d(TAG, "Botão Simular Risco Alto CLICADO")
            if (checkNotificationPermission()) {
                NotificationHelper.simulateHighRisk(this)
            } else {
                Log.w(TAG, "Permissão de notificação não concedida. Não é possível enviar Risco Alto.")
                Toast.makeText(this, "Permissão de notificação necessária.", Toast.LENGTH_SHORT).show()
            }
        }

    }
    // Função auxiliar para verificar a permissão antes de tentar enviar uma notificação
    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true // Para versões anteriores ao Android 13, a permissão é concedida por padrão
    }

    // Função que recupera os dados do Firebase
    private fun setupFirebaseListener() {
        setupDataListener()
        setupTimestampListener()
    }

    // Função que acessa os dados do Firebase
    private fun setupDataListener() {
        // define os elementos UI
        val txtTemp = findViewById<TextView>(R.id.tv_temperature)
        val txtUmi = findViewById<TextView>(R.id.tv_humidity)
        val txtPressao = findViewById<TextView>(R.id.tv_pressure)
        val txtPreci = findViewById<TextView>(R.id.tv_flood_level)
        val texLevel = findViewById<TextView>(R.id.tv_flood_risk_label)

        // Declara o caminho dos dados do sensor DHT
        val refDht = database.getReference("sensor/data/")

        refDht.addValueEventListener(object : ValueEventListener {
            // Busca temperatura e umidade toda vez que for alterado
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatura = snapshot.child("temperatura").getValue<Float>()
                txtTemp.text = "%.1f°C".format(temperatura).replace('.', ',')

                val umidade = snapshot.child("umidade").getValue<Int>()
                txtUmi.text = "$umidade%"

                val pressao = snapshot.child("pressao").getValue<Float>()
                txtPressao.text = "%.1f hPa".format(pressao).replace('.',',')

                val volume = snapshot.child("volume").getValue<Float>()
                txtPreci.text = "%.1f mm".format(volume).replace('.',',')

            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
            }
        })
    }

    // Função para alterar satus do sistema
    private fun setupTimestampListener() {

        // define o elemento UI
        val txtStatus = findViewById<TextView>(R.id.tv_weather_desc)

        // declara o caminho do timestamp
        val refTimestamp = database.getReference("timestamp/")

        refTimestamp.addValueEventListener(object : ValueEventListener {
            // acessa o ultimo timestamp
            override fun onDataChange(snapshot: DataSnapshot) {
                val horaStr = snapshot.child("hora").getValue<String>()
                val dataStr = snapshot.child("data").getValue<String>()

                // verifica se os dados não são nulos para formatá-los
                if (dataStr != null && horaStr != null) {
                    val formatterData = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val formatterHora = DateTimeFormatter.ofPattern("HH:mm:ss")

                    // formata a data e hora registrada no firebase
                    try {
                        val data = LocalDate.parse(dataStr, formatterData)
                        val hora = LocalTime.parse(horaStr, formatterHora)
                        val dataHora = LocalDateTime.of(data, hora)
                        val ultTimestamp = dataHora.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        checkStatus(txtStatus, ultTimestamp)
                    } catch (e: Exception) {
                        Log.e("DateTime", "Erro ao parsear data/hora", e)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler hora", error.toException())
            }
        })
    }

    // função que altera o status do sistema
    private fun checkStatus(txtStatus: TextView, ultTimestamp: Long) {
        // captura o timestamp atual
        val atualTimestamp = System.currentTimeMillis()

        // verifica a diferença de tempo
        val diferenca = atualTimestamp - ultTimestamp
        val diferencaSeg = diferenca/1000

        // se caso tiver inatividade a mais de 20 segundos, altera o status
        if (diferencaSeg > 20) {
            txtStatus.text = "Sistema inativo"
        } else {
            txtStatus.text = "Sistema ativo"
        }
    }

    private fun criarCanalNotificacao() {
        val name = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel("water-sentinel-channel", name, importance).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 100, 200)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    private fun solicitarPermissaoNotificacao() {
        val permissao = Manifest.permission.POST_NOTIFICATIONS
        when {

            // caso a permissão já tenha sido concedida
            ContextCompat.checkSelfPermission(
                this,
                permissao
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permissão já concedida
                //showNotification()
            }

            // caso o usuário já tenha negado antes
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permissao) -> {
                // explicar ao usuário porque a permissão é necessária
                mostrarExplicacaoPermissao()
            }

            // primeira vez solicitando
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permissao),
                    CODIGO_PERMISSAO_NOTIFICACAO)
            }
        }
    }

    // Função da caixa de diálogo da permissão
    private fun mostrarExplicacaoPermissao() {
        AlertDialog.Builder(this)
            .setTitle("Permissão de Notificações")
            .setMessage("Este app precisa enviar notificações para alertar sobre mudanças no sistema de monitoramento de água.")
            .setPositiveButton("Permitir") { _, _ ->
                solicitarPermissaoNotificacao()
            }
            .setNegativeButton("Agora não", null)
            .show()
    }

    // Função para tratar a resposta da solicitação de permissao
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CODIGO_PERMISSAO_NOTIFICACAO -> {
                // se a requisição for cancelada, o array estará vazio
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissão concedida
                    Toast.makeText(
                        this,
                        "Notificações ativadas",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Permissão negada
                    Toast.makeText(
                        this,
                        "Notificações desativadas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}