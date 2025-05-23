package com.example.water_sentinel

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    }
    private val database = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        criarCanalNotificacao()

        solicitarPermissaoNotificacao()

        setupFirebaseListener()
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

        // Declara o caminho dos dados do sensor DHT
        val refDht = database.getReference("sensor/dht/")

        refDht.addValueEventListener(object : ValueEventListener {
            // Busca temperatura e umidade toda vez que for alterado
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatura = snapshot.child("temperatura").getValue<Int>()
                txtTemp.text = "$temperatura°C"

                val umidade = snapshot.child("umidade").getValue<Int>()
                txtUmi.text = "$umidade%"

            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados do sensor DHT", error.toException())
            }
        })

        val refPressao = database.getReference("sensor/bpm180/pressao")

        refPressao.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pressao = snapshot.getValue<Int>()
                txtPressao.text = "$pressao Pas"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler a pressão", error.toException())
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



        //val button = findViewById<Button>(R.id.btnMap)
        //button.setOnClickListener {
            //val intent = Intent(this, MapsActivity::class.java)
            //startActivity(intent)
        //}
}