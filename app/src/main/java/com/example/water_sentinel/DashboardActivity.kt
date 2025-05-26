package com.example.water_sentinel

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
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
    private var lastNotifiedAlertLevel: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        Log.d(TAG, "onCreate: Activity Criada")

        solicitarPermissaoNotificacao()

        setupFirebaseListener()

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
        val txtvolume = findViewById<TextView>(R.id.tv_volume_mm)
        val txtPercentual = findViewById<TextView>(R.id.tv_flood_percent)

        // Declara o caminho dos dados do sensor DHT
        val refDht = database.getReference("sensor/data/")

        refDht.addValueEventListener(object : ValueEventListener {
            // Busca temperatura e umidade toda vez que for alterado
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatura = snapshot.child("temperatura").getValue<Float>()
                if(temperatura != null) {
                    txtTemp.text = "%.1f°C".format(temperatura).replace('.', ',')
                }else{
                    txtTemp.text = getString(R.string.sem_temperatura)
                }

                val umidade = snapshot.child("umidade").getValue<Int>()
                if (umidade != null) {
                    txtUmi.text = "$umidade%"
                }else{
                    txtUmi.text = getString(R.string.sem_dados)
                }

                val pressao = snapshot.child("pressao").getValue<Float>()
                if(pressao != null) {
                    //txtPressao.text = "%.1f hPa".format(pressao).replace('.', ',')
                    txtPressao.text = "%d hPa".format(pressao.toInt())
                }else{
                    txtPressao.text = getString(R.string.sem_dados)
                }

                //como são o mesmo dado, defino junto
                val volume = snapshot.child("volume").getValue<Float>()
                if (volume != null) {
                    txtPreci.text = String.format("%.1f mm", volume).replace('.', ',')
                    txtvolume.text = String.format("%.1f mm/s", volume).replace('.', ',')
                }else{
                    txtPreci.text = getString(R.string.volume)
                    txtvolume.text = getString(R.string.sem_dados)
                }

                val percentual = snapshot.child("percentual").getValue<Int>()
                if(percentual != null){
                    txtPercentual.text = "$percentual%"

                } else{
                    txtPercentual.text = getString(R.string.porcentagem)
                }

                // Busca e processa o alertLevel
                val alertLevelAtual = snapshot.child("alertLevel").getValue<Int>()
                //Log.d(TAG, "setupDataListener - alertLevelAtual DO FIREBASE: $alertLevelAtual")

                processarMudancaAlertLevel(alertLevelAtual)
            }
            // Função que trata algum erro
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Erro ao ler dados", error.toException())
                // Opcional: Resetar UI para placeholders
                txtTemp.text = getString(R.string.sem_temperatura)
                txtUmi.text = getString(R.string.sem_dados)
                txtPressao.text = getString(R.string.sem_dados)
                txtPreci.text = getString(R.string.volume)
                txtvolume.text = getString(R.string.sem_dados)
                processarMudancaAlertLevel(null)
            }
        })
    }

    private fun processarMudancaAlertLevel(alertLevelFirebase: Int?) {
        // Obtém as referências para os elementos do card de risco AQUI
        val tvLocalRiskLevelText = findViewById<TextView>(R.id.tv_flood_risk_level_text)
        val imgLocalRiskIcon = findViewById<ImageView>(R.id.img_flood_icon)
        val tvLocalFloodPercent = findViewById<TextView>(R.id.tv_flood_percent)

        val nivelAlertaAtual = alertLevelFirebase ?: 0
        //Log.d(TAG, "processarMudancaAlertLevel - nivelAlertaAtual SENDO PROCESSADO: $nivelAlertaAtual (valor original do Firebase: $alertLevelFirebase)")


        val textoRisco: String
        val corTextoRiscoRes: Int
        val idIconSunny: Int
        val idIconeGota: Int
        val corIconeRes: Int

        var tituloNotificacao = ""
        var mensagemNotificacao = ""
        var deveEnviarNotificacao = false

        when (nivelAlertaAtual) {
            0 -> { // Sem Risco
                textoRisco = getString(R.string.risk_0_no_risk)
                corTextoRiscoRes = R.color.risk_color_blue
                idIconeGota = R.drawable.sunny
                corIconeRes = R.color.risk_color_blue
            }
            1 -> { // Baixo Risco
                textoRisco = getString(R.string.risk_1_low)
                corTextoRiscoRes = R.color.risk_color_green
                idIconeGota = R.drawable.gota
                corIconeRes = R.color.risk_color_green

                tituloNotificacao = getString(R.string.risk_1_low)
                mensagemNotificacao = getString(R.string.message_low_risk)
                deveEnviarNotificacao = true
            }
            2 -> { // Médio Risco
                textoRisco = getString(R.string.risk_2_medium)
                corTextoRiscoRes = R.color.risk_color_yellow
                idIconeGota = R.drawable.gota
                corIconeRes = R.color.risk_color_yellow

                tituloNotificacao = getString(R.string.risk_2_medium)
                mensagemNotificacao = getString(R.string.message_medium_risk)
                deveEnviarNotificacao = true
            }
            3 -> { // Alto Risco
                textoRisco = getString(R.string.risk_3_high)
                corTextoRiscoRes = R.color.risk_color_red
                idIconeGota = R.drawable.gota
                corIconeRes = R.color.risk_color_red

                tituloNotificacao = getString(R.string.risk_3_high)
                mensagemNotificacao = getString(R.string.message_high_risk)
                deveEnviarNotificacao = true
            }
            else -> { // nenhum nível
                textoRisco = getString(R.string.risk_level_unknown)
                corTextoRiscoRes = android.R.color.darker_gray
                idIconeGota = R.drawable.sunny // trocar por outra coisa
                corIconeRes = android.R.color.darker_gray
            }
        }

        // Converte o recurso de cor para a cor real uma vez
        val corResolvedaParaTexto = ContextCompat.getColor(this, corTextoRiscoRes)
        val corResolvedaParaIcone = ContextCompat.getColor(this, corIconeRes)


        // atualiza a UI do card de risco
        tvLocalRiskLevelText.text = textoRisco
        tvLocalRiskLevelText.setTextColor(corResolvedaParaTexto)

        imgLocalRiskIcon.setImageResource(idIconeGota)
        if (idIconeGota == R.drawable.sunny) {
            ImageViewCompat.setImageTintList(imgLocalRiskIcon, null)
        } else {

            val corResolvedaParaIconeTint = ContextCompat.getColor(this, corIconeRes)
            ImageViewCompat.setImageTintList(imgLocalRiskIcon, ColorStateList.valueOf(corResolvedaParaIconeTint))
        }

        tvLocalFloodPercent.setTextColor(corResolvedaParaTexto) // Usa a mesma cor do texto de risco

        // envia notificação se necessário e permitido
        if (deveEnviarNotificacao) { // verifica se uma notificação é justificada pelo nível de alerta
            if (checkNotificationPermission()) {
                if (nivelAlertaAtual != lastNotifiedAlertLevel) {
                    NotificationHelper.sendFloodRiskNotification(this, tituloNotificacao, mensagemNotificacao)
                    lastNotifiedAlertLevel = nivelAlertaAtual
                    Log.d(TAG, "Notificação enviada para alertLevel: $nivelAlertaAtual")
                }
            } else { // deveEnviarNotificacao era true, mas não há permissão
                Log.w(TAG, "Permissão de notificação não concedida para $tituloNotificacao.")
            }
        }

        if (nivelAlertaAtual == 0 && lastNotifiedAlertLevel != 0) {
            lastNotifiedAlertLevel = 0
            //Log.d(TAG, "Nível de risco zerado. lastNotifiedAlertLevel resetado.")
        }
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

    private fun solicitarPermissaoNotificacao() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            val permissao = Manifest.permission.POST_NOTIFICATIONS
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    permissao
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permissão já concedida
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, permissao) -> {
                    mostrarExplicacaoPermissao()
                }
                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(permissao),
                        CODIGO_PERMISSAO_NOTIFICACAO
                    )
                }
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