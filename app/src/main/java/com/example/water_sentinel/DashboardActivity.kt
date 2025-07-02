package com.example.water_sentinel

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentContainerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.water_sentinel.db.TodoDao
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
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.db.DataHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId


class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val CODIGO_PERMISSAO_NOTIFICACAO = 1001
        private const val CODIGO_PERMISSAO_LOCALIZACAO = 1002
        private const val TAG = "DashboardActivity" // Tag para logs
    }
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val database = Firebase.database
    private var lastNotifiedAlertLevel: Int = -1
    private var locationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 3000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val todoDao: TodoDao by lazy { (application as MyApp).database.todoDao() }

    private lateinit var txtTemp: TextView
    private lateinit var txtUmi: TextView
    private lateinit var txtPressao: TextView
    private lateinit var txtPreci: TextView
    private lateinit var txtvolume: TextView
    private lateinit var txtPercentual: TextView
    private lateinit var txtStatus: TextView


    private val handler = Handler(Looper.getMainLooper())
    private lateinit var statusCheckRunnable: Runnable
    private var ultimoTimestampRecebido: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        Log.d(TAG, "onCreate: Activity Criada")

        // Captura o mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)


        solicitarPermissaoNotificacao()

        setupFirebaseListener()

        txtTemp = findViewById<TextView>(R.id.tv_temperature)
        txtUmi = findViewById<TextView>(R.id.tv_humidity)
        txtPressao = findViewById<TextView>(R.id.tv_pressure)
        txtPreci = findViewById<TextView>(R.id.tv_flood_level)
        txtvolume = findViewById<TextView>(R.id.tv_volume_mm)
        txtPercentual = findViewById<TextView>(R.id.tv_flood_percent)
        txtStatus = findViewById(R.id.tv_weather_desc)

        // Configura clique para abrir o mapa
        findViewById<FragmentContainerView>(R.id.mapView).setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        // Recupera a localização do usuário nesta activity
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // --- CONFIGURAÇÃO DOS CLIQUES NOS CARDS ---
        findViewById<LinearLayout>(R.id.card_humidity).setOnClickListener {
            showHistoryDialog("humidity", "Histórico de Umidade")
        }
        findViewById<LinearLayout>(R.id.card_pressure).setOnClickListener {
            showHistoryDialog("pressure", "Histórico de Pressão")
        }
        findViewById<LinearLayout>(R.id.card_flood_level).setOnClickListener {
            showHistoryDialog("card_precipitation", "Histórico de Precipitação")
        }



        // isso abaixo verifica o status para ver se o embarcado continua mandando dados
        statusCheckRunnable = object : Runnable {
            override fun run() {
                checkStatus()
                handler.postDelayed(this, 3000) // 5000 ms = 5 segundos
            }
        }
        handler.post(statusCheckRunnable)
    }

    // ------------ DADOS -----------

    // Função que recupera os dados do Firebase
    private fun setupFirebaseListener() {
        setupDataListener()
        setupTimestampListener()
    }

    // Função que acessa os dados do Firebase
    private fun setupDataListener() {

        // Declara o caminho dos dados do sensor DHT
        val refDht = database.getReference("sensor/data/")

        refDht.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isSystemActive) {
                    return
                }

                val temperatura = snapshot.child("temperatura").getValue(Float::class.java)
                val umidade = snapshot.child("umidade").getValue(Int::class.java)
                val pressaoRaw = snapshot.child("pressao").getValue(Int::class.java)
                val volume = snapshot.child("volume").getValue(Float::class.java)
                val percentual = snapshot.child("percentual").getValue(Int::class.java)

                val pressao: Int? = if (pressaoRaw == 0 || pressaoRaw == null) null else pressaoRaw

                txtTemp.text = temperatura?.let { "%.1f°C".format(it).replace('.', ',') } ?: "---"
                txtUmi.text = umidade?.let { "$it%" } ?: "---"
                txtPressao.text = pressao?.let { "$it hPa" } ?: "---"
                txtPreci.text = volume?.let { String.format("%.1f mm", it).replace('.', ',') } ?: "---"
                txtvolume.text = volume?.let { String.format("%.1f mm/s", it).replace('.', ',') } ?: "---"
                txtPercentual.text = percentual?.let { "$it%" } ?: "---"

                val alertLevelAtual = snapshot.child("alertLevel").getValue(Int::class.java)
                processarMudancaAlertLevel(alertLevelAtual)

                val status = findViewById<TextView>(R.id.tv_weather_desc).text.toString()
                if (isSystemActive) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val currentReading = DataHistory(
                            temperature = temperatura,
                            humidity = umidade,
                            pressure = pressao,
                            precipitation = volume,
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
                txtPreci.text = getString(R.string.volume)
                txtvolume.text = getString(R.string.sem_dados)
                processarMudancaAlertLevel(null)
            }
        })
    }


    private fun showHistoryDialog(metricType: String, title: String) {
        val dialog = HistoryDialogFragment.newInstance(metricType, title)
        dialog.show(supportFragmentManager, "HistoryDialog")
    }


    private fun processarMudancaAlertLevel(alertLevelFirebase: Int?) {
        // Obtém as referências para os elementos do card de risco AQUI
        val tvLocalRiskLevelText = findViewById<TextView>(R.id.tv_flood_risk_level_text)
        val imgLocalRiskIcon = findViewById<ImageView>(R.id.img_flood_icon)
        val tvLocalFloodPercent = findViewById<TextView>(R.id.tv_flood_percent)
        val txtStatus = findViewById<TextView>(R.id.tv_weather_desc)

        val nivelAlertaAtual = alertLevelFirebase ?: 0
        //Log.d(TAG, "processarMudancaAlertLevel - nivelAlertaAtual SENDO PROCESSADO: $nivelAlertaAtual (valor original do Firebase: $alertLevelFirebase)")

        val textoRisco: String
        val corTextoRiscoRes: Int
        val idIconeGota: Int
        val corIconeRes: Int

        var tituloNotificacao = ""
        var mensagemNotificacao = ""
        var deveEnviarNotificacao = false

        if(!isSystemActive) {
            textoRisco = getString(R.string.risk_level_unknown)
            corTextoRiscoRes = android.R.color.darker_gray
            idIconeGota = R.drawable.sinal_off_de_rede // trocar por outra coisa
            corIconeRes = android.R.color.darker_gray
            (application as MyApp).postoAlerta.apply {
                this.riscoPorcentagem = 0
                this.umidade = 0
                this.temperatura = 0f
                this.pressao = 0
                this.status = -1
            }
        } else {
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
                else -> {
                    textoRisco = getString(R.string.risk_level_unknown)
                    corTextoRiscoRes = android.R.color.darker_gray
                    idIconeGota = R.drawable.sinal_off_de_rede // trocar por outra coisa
                    corIconeRes = android.R.color.darker_gray
                }
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
            if (checarPermissaoNotificacao()) {
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

    private var isSystemActive: Boolean = false

    // função que altera o status do sistema
    private fun checkStatus() {
        val atualTimestamp = System.currentTimeMillis()
        val diferencaSeg = (atualTimestamp - ultimoTimestampRecebido) / 1000

        if (ultimoTimestampRecebido == 0L || diferencaSeg > 20) {
            if (isSystemActive) {
                isSystemActive = false
                txtStatus.text = "Sistema inativo"
                clearDashboardData() // Limpa a UI
            }
        } else {
            if (!isSystemActive) {
                isSystemActive = true
                txtStatus.text = "Sistema ativo"
            }
        }
    }

    private fun clearDashboardData() { //limpa os dados caso não venha mais do firebase
        txtTemp.text = "---"
        txtUmi.text = "---"
        txtPressao.text = "---"
        txtPreci.text = "---"
        txtvolume.text = "---"
        txtPercentual.text = "---"

        processarMudancaAlertLevel(null)
    }

    // ------------ NOTIFICAÇÕES -----------

    // Função para tratar a resposta da solicitação de permissao
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
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
                }
                checarPermissaoLocalizacao()
            }
            CODIGO_PERMISSAO_LOCALIZACAO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (::map.isInitialized) {
                        mostrarLocalizacaoAtual()
                    }
                } else {
                    // Permissão negada
                    Toast.makeText(
                        this,
                        "Ative a localização nas configurações para ver sua posição",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Função auxiliar para verificar a permissão antes de tentar enviar uma notificação
    private fun checarPermissaoNotificacao(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true // Para versões anteriores ao Android 13, a permissão é concedida por padrão
    }

    // Função que realiza a solicitação da permissão de notificações
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

    // ------------ MAPA -----------

    // Função de setup do mapa
    //@RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checarPermissaoLocalizacao()
        desativarInteracoes()
        //Log.e("LOCALIZACAO", "passou, map = $map")
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mostrarLocalizacaoAtual()
        }

        addRiskMarker((application as MyApp).postoAlerta)
    }

    // Função que desativa as interações do mapa
    private fun desativarInteracoes() {
        with(map.uiSettings) {
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isMapToolbarEnabled = false
        }
        map.setOnMapClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }
    }

    private fun addRiskMarker(posto: PostoAlerta) {
        //val statusRisco = findViewById<TextView>(R.id.tv_flood_risk_level_text).text.toString()

        val icone: BitmapDescriptor = when (posto.status) {
            0 -> bitmapDescriptorFromVector(getDrawable(R.drawable.ic_marker_no_risk)!!)
            1 -> bitmapDescriptorFromVector(getDrawable(R.drawable.ic_marker_low_risk)!!)
            2 -> bitmapDescriptorFromVector(getDrawable(R.drawable.ic_marker_medium_risk)!!)
            3 -> bitmapDescriptorFromVector(getDrawable(R.drawable.ic_marker_high_risk)!!)
            else -> bitmapDescriptorFromVector(getDrawable(R.drawable.sinal_off_de_rede)!!)
        }

        /*val titulo: String = when (posto.status) {
            0 -> binding.root.context.getString(R.string.risk_0_no_risk)
            1 -> binding.root.context.getString(R.string.risk_1_low)
            2 -> binding.root.context.getString(R.string.risk_2_medium)
            3 -> binding.root.context.getString(R.string.risk_3_high)
            else -> binding.root.context.getString(R.string.risk_level_unknown)
        }*/

        map.addMarker(
            MarkerOptions()
                .position(posto.latLng)
                .icon(icone)
        )?.also { marker ->
            marker.tag = posto
        }
    }

    private fun bitmapDescriptorFromVector(drawable: android.graphics.drawable.Drawable): BitmapDescriptor {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // Função que verifica a permissão de localizacao
    private fun checarPermissaoLocalizacao() {
        val permissao = Manifest.permission.ACCESS_FINE_LOCATION

        when {
            // Verifica se já há permissão
            ContextCompat.checkSelfPermission(this, permissao) == PackageManager.PERMISSION_GRANTED -> {
                mostrarLocalizacaoAtual()
            }

            // Verifica se o usuário já negou uma vez
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, permissao) -> {
                solicitarPermissaoLocalizacao()
            }

            // Se não há permissão
            else -> {
                solicitarPermissaoLocalizacao()
            }

        }
    }

    // Função para verificar a permissão do acesso a localização
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun mostrarLocalizacaoAtual() {
        // Ativa as configurações de localização atual do GoogleMaps
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        // A cada atualização da localização, o mapa também é atualizado
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                lr.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }

        // Realiza a atualização da localização do dispositivo em um tempo determinado
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // Função para solicitar a permissão de localização do usuário
    private fun solicitarPermissaoLocalizacao() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            CODIGO_PERMISSAO_LOCALIZACAO
        )
    }
}