package com.example.water_sentinel.ui.dashboard

import android.Manifest
import com.example.water_sentinel.R
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.water_sentinel.MyApp
import com.example.water_sentinel.service.DataCollectionService
import com.example.water_sentinel.data.remote.FirebaseDataSource
import com.example.water_sentinel.data.repository.DataRepository
import com.example.water_sentinel.databinding.ActivityDashboardBinding
import com.example.water_sentinel.domain.model.PostoAlerta
import com.example.water_sentinel.ui.history.HistoryDialogFragment
import com.example.water_sentinel.ui.maps.MapsActivity
import com.example.water_sentinel.util.AppUtils
import com.example.water_sentinel.util.PermissionHelper
import com.example.water_sentinel.util.PermissionHelper.CODIGO_PERMISSAO_LOCALIZACAO
import com.example.water_sentinel.util.PermissionHelper.CODIGO_PERMISSAO_NOTIFICACAO
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback, HistoryDialogFragment.OnDialogDismissListener {
    companion object {
        private const val TAG = "DashboardActivity" // Tag para logs
        private const val DIALOG_TAG = "HistoryDialog"
    }
    private lateinit var binding: ActivityDashboardBinding

    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(
            DataRepository(
                FirebaseDataSource(Firebase.database),
                (application as MyApp).database.todoDao()
            )
        )
    }

    private var isDialogCurrentlyShowing = false
    private var isPrimAtualizacaoLoc = true
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val locationRequest: LocationRequest by lazy {
        LocationRequest.Builder(5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla a interface e configura o conteúdo
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicita as permissões necessárias
        PermissionHelper.solicitarPermissaoNotif(this)
        PermissionHelper.solicitarPermissaoLoc(this)

        // Configura os listeners e o observador do ViewModel
        setupClickListeners()
        observeViewModelState()

        // Inicia o serviço de coleta de dados em segundo plano
        val intent = Intent(this, DataCollectionService::class.java)
        ContextCompat.startForegroundService(this, intent)

        // Inicializa o cliente de localização e o mapa
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    private fun setupClickListeners() {
        // Configura clique para abrir o mapa
        binding.mapView.setOnClickListener {
            startActivity(Intent(this, MapsActivity::class.java))
        }

        // Configura cliques dos históricos
        binding.cardFloodRisk.setOnClickListener {
            showHistoryDialog("percentage", "Histórico de Risco")
        }

        binding.cardHumidity.setOnClickListener {
            showHistoryDialog("humidity", "Histórico de Umidade")
        }

        binding.cardPressure.setOnClickListener {
            showHistoryDialog("pressure", "Histórico de Pressão")
        }

        binding.cardFloodLevel.setOnClickListener {
            showHistoryDialog("card_precipitation", "Histórico de Volume")
        }
    }

    private fun observeViewModelState() {
        lifecycleScope.launch {
            // Observa o estado da UI e atualiza a tela
            viewModel.uiState.collect { uiState ->
                val posto = uiState.principalPosto
                val riscoData = uiState.cardRiscoData

                // Atualiza a interface com os dados do posto principal
                if (posto != null) {
                    // Lógica para mostrar o status do sistema (ativo/inativo)
                    if (posto.ativo) {
                        binding.tvWeatherDesc.text = AppUtils.getStatusSistemaText(this@DashboardActivity, posto.ativo)
                        binding.tvTemperature.text = getString(R.string.valor_temp, posto.temperatura)
                        binding.tvHumidity.text = getString(R.string.valor_umidade, posto.umidade)
                        binding.tvPressure.text = getString(R.string.valor_pressao, posto.pressao)
                        binding.tvVolume.text = getString(R.string.valor_volume, posto.volume)
                        binding.tvFloodPercent.text = getString(R.string.valor_risco, posto.riscoPorcentagem)

                        // Estilização do card de risco
                        val corTexto = ContextCompat.getColor(
                            this@DashboardActivity,
                            riscoData.corTextoRiscoRes
                        )
                        binding.tvFloodRiskLevelText.text = getString(riscoData.textoRisco)
                        binding.tvFloodRiskLevelText.setTextColor(corTexto)
                        binding.tvFloodPercent.setTextColor(corTexto)
                        binding.imgFloodIcon.setImageResource(riscoData.idIconeGota)

                        // Lógica para o tint da imagem
                        if (riscoData.idIconeGota == R.drawable.sunny) {
                            ImageViewCompat.setImageTintList(binding.imgFloodIcon, null)
                        } else {
                            val corIcone = ContextCompat.getColor(
                                this@DashboardActivity,
                                riscoData.corIconeRes
                            )
                            ImageViewCompat.setImageTintList(
                                binding.imgFloodIcon,
                                ColorStateList.valueOf(corIcone)
                            )
                        }

                        // Atualiza o mapa com o marcador
                        if (::map.isInitialized) {
                            map.clear()
                            addRiskMarker(posto)
                        }
                    } else {
                        binding.tvWeatherDesc.text = AppUtils.getStatusSistemaText(this@DashboardActivity, posto.ativo)
                        clearDashboardData()
                    }
                } else { // Limpa os dados caso não venha mais do firebase
                    clearDashboardData()
                }
            }
        }
    }

    private fun clearDashboardData() {
        binding.tvTemperature.text = "---"
        binding.tvHumidity.text = "---"
        binding.tvPressure.text = "---"
        binding.tvVolume.text = "---"
        binding.tvFloodPercent.text = "---"
        binding.tvFloodRiskLevelText.text = "---"
    }

    // ------------ DIALOG DE HISTÓRICO -----------
    override fun onDialogDismissed() {
        isDialogCurrentlyShowing = false // Destrava
    }

    private fun showHistoryDialog(metricType: String, title: String) {
        if (isDialogCurrentlyShowing) {
            Log.d(TAG, "Um diálogo já está sendo exibido. Clique em '$title' ignorado.")
            return
        }

        isDialogCurrentlyShowing = true
        Log.d(TAG, "Abrindo diálogo para '$title'. Trava ativada.")

        val dialog = HistoryDialogFragment.Companion.newInstance(metricType, title)
        dialog.show(supportFragmentManager, "HistoryDialog")
    }

    // ------------ PERMISSÕES -----------

    // Função para tratar a resposta da solicitação de permissao
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CODIGO_PERMISSAO_NOTIFICACAO -> {
                // Se a requisição for cancelada, o array estará vazio
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissão concedida
                    Toast.makeText(this, "Notificações ativadas", Toast.LENGTH_SHORT).show()
                }
                // Chama a checagem de localização aqui para garantir a sequência correta de permissões
                checarPermissaoLocalizacao()
            }
            CODIGO_PERMISSAO_LOCALIZACAO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (::map.isInitialized) {
                        ativarLocUser()
                    }
                } else {
                    // Permissão negada
                    Toast.makeText(this, "Ative a localização nas configurações para ver sua posição", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ------------ MAPA -----------

    // Função de setup do mapa
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        desativarInteracoes()
        mostrarLocalizacaoAtual()
        checarPermissaoLocalizacao()
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                uiState.principalPosto.let { posto ->
                    map.clear()
                    addRiskMarker(posto)
                }
            }
        }
    }

    // Função que verifica a permissão de localizacao
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun checarPermissaoLocalizacao() {
        if (::map.isInitialized) {
            if (PermissionHelper.checarPermissaoLoc(this)) {
                ativarLocUser()
            } else {
                PermissionHelper.solicitarPermissaoLoc(this)
            }
        }
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        // Inicia as atualizações de localização apenas quando a Activity está ativa
        if (PermissionHelper.checarPermissaoLoc(this)) {
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // Remove as atualizações de localização para economizar bateria
        locationCallback?.let { callback ->
            if (::fusedLocationClient.isInitialized) {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cria o Intent para o DataCollectionService
        val intent = Intent(this, DataCollectionService::class.java)

        // Chama stopService para parar o serviço Foreground
        stopService(intent)
    }

    // Função para verificar a permissão do acesso a localização
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun mostrarLocalizacaoAtual() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(lr: LocationResult) {
                // Centraliza apenas na primeira atualização
                lr.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun ativarLocUser() {
        // Esta função só é chamada se tivermos permissão
        if (!::map.isInitialized) return

        // Ativa as configurações de localização atual do GoogleMaps
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        }
    }

    private fun addRiskMarker(posto: PostoAlerta?) {
        val icone: BitmapDescriptor = when (posto?.status) {
            0 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_no_risk)!!)
            1 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_low_risk)!!)
            2 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_medium_risk)!!)
            3 -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.ic_marker_high_risk)!!)
            else -> bitmapDescriptorFromVector(AppCompatResources.getDrawable(this, R.drawable.sinal_off_de_rede)!!)
        }

        posto?.latLng?.let { latLng ->
            // Cria o objeto MarkerOptions
            val markerOptions = MarkerOptions()
                .position(latLng)
                .icon(icone)

            // Passa o MarkerOptions para a função addMarker()
            val marker = map.addMarker(markerOptions)

            // O objeto Marker é usado para definir a tag
            marker?.tag = posto
        }
    }

    private fun bitmapDescriptorFromVector(drawable: Drawable): BitmapDescriptor {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap =
            createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}